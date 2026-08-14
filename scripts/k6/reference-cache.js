import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
const REQUEST_TIMEOUT = '2s';
const READINESS_PATH = __ENV.READINESS_PATH || '/actuator/health/readiness';
const ACCESS_TOKEN = (__ENV.ACCESS_TOKEN || '').trim();
const DEV_USER_ID = (__ENV.DEV_USER_ID || '').trim();
const LINK_URL = __ENV.LINK_URL || 'https://example.com/k6/reference-cache';
const LINK_SOURCE = (__ENV.LINK_SOURCE || 'WEB').trim().toUpperCase();
const PLACE_ID_PREFIX = (__ENV.PLACE_ID_PREFIX || 'k6-reference-cache').trim();
const SUMMARY_PATH = (__ENV.SUMMARY_PATH || '').trim();

const READINESS_MAX_ATTEMPTS = envPositiveInteger('READINESS_MAX_ATTEMPTS', 30);
const READINESS_POLL_INTERVAL_MS = envPositiveInteger('READINESS_POLL_INTERVAL_MS', 500);
const STATUS_MAX_ATTEMPTS = envPositiveInteger('STATUS_MAX_ATTEMPTS', 3);
const STATUS_POLL_INTERVAL_MS = envPositiveInteger('STATUS_POLL_INTERVAL_MS', 500);
const PLACE_IDS_PER_VU = envPositiveInteger('PLACE_IDS_PER_VU', 1);

const WARMUP_RATE = envPositiveInteger('WARMUP_RATE', 5);
const STEADY_RATE = envPositiveInteger('STEADY_RATE', 25);
const SPIKE_RATE = envPositiveInteger('SPIKE_RATE', 50);
const WARMUP_DURATION = envDuration('WARMUP_DURATION', '15s');
const STEADY_DURATION = envDuration('STEADY_DURATION', '45s');
const SPIKE_DURATION = envDuration('SPIKE_DURATION', '30s');
const GRACEFUL_STOP = envDuration('GRACEFUL_STOP', '5s');
const SETUP_TIMEOUT = envDuration('SETUP_TIMEOUT', '60s');
const TEARDOWN_TIMEOUT = envDuration('TEARDOWN_TIMEOUT', '30s');

const WARMUP_PRE_ALLOCATED_VUS = envPositiveInteger('WARMUP_PRE_ALLOCATED_VUS', 5);
const WARMUP_MAX_VUS = envPositiveInteger('WARMUP_MAX_VUS', 10);
const STEADY_PRE_ALLOCATED_VUS = envPositiveInteger('STEADY_PRE_ALLOCATED_VUS', 25);
const STEADY_MAX_VUS = envPositiveInteger('STEADY_MAX_VUS', 50);
const SPIKE_PRE_ALLOCATED_VUS = envPositiveInteger('SPIKE_PRE_ALLOCATED_VUS', 50);
const SPIKE_MAX_VUS = envPositiveInteger('SPIKE_MAX_VUS', 100);

const MIN_CHECK_RATE = envRatio('MIN_CHECK_RATE', 0.99);
const MAX_HTTP_FAILURE_RATE = envRatio('MAX_HTTP_FAILURE_RATE', 0.01);
const P95_DURATION_MS = envPositiveInteger('P95_DURATION_MS', 500);
const P99_DURATION_MS = envPositiveInteger('P99_DURATION_MS', 1000);

validateConfiguration();

const steadyStart = millisecondsToDuration(durationToMilliseconds(WARMUP_DURATION));
const spikeStart = millisecondsToDuration(
	durationToMilliseconds(WARMUP_DURATION) + durationToMilliseconds(STEADY_DURATION)
);

export const options = {
	discardResponseBodies: false,
	maxRedirects: 0,
	setupTimeout: SETUP_TIMEOUT,
	teardownTimeout: TEARDOWN_TIMEOUT,
	scenarios: {
		warmup: arrivalRateScenario(
			WARMUP_RATE,
			WARMUP_DURATION,
			'0s',
			WARMUP_PRE_ALLOCATED_VUS,
			WARMUP_MAX_VUS
		),
		steady: arrivalRateScenario(
			STEADY_RATE,
			STEADY_DURATION,
			steadyStart,
			STEADY_PRE_ALLOCATED_VUS,
			STEADY_MAX_VUS
		),
		spike: arrivalRateScenario(
			SPIKE_RATE,
			SPIKE_DURATION,
			spikeStart,
			SPIKE_PRE_ALLOCATED_VUS,
			SPIKE_MAX_VUS
		),
	},
	thresholds: {
		'checks{operation:manual_place_save}': [`rate>${MIN_CHECK_RATE}`],
		'http_req_failed{name:manual_place_save}': [`rate<${MAX_HTTP_FAILURE_RATE}`],
		'http_req_duration{name:manual_place_save}': [
			`p(95)<${P95_DURATION_MS}`,
			`p(99)<${P99_DURATION_MS}`,
		],
		dropped_iterations: ['count==0'],
	},
};

export function setup() {
	waitUntilReady();

	const token = ACCESS_TOKEN || issueDevMasterToken();
	const headers = authorizationHeaders(token);
	const room = createRoom(headers);
	const analysis = createLinkAnalysisRequest(room.roomId, headers);
	pollLinkAnalysisStatus(room.roomId, analysis.analysisRequestId, analysis.status, headers);

	// Prime both reference-data caches outside the measured scenarios.
	const primePlaceId = `${PLACE_ID_PREFIX}-setup`;
	const primeResponse = saveManualPlace(
		room.roomId,
		analysis.analysisRequestId,
		manualPlacePayload(primePlaceId, 'setup', 0, 0),
		headers,
		'setup_manual_place_save'
	);
	requireManualSave(primeResponse, primePlaceId, 'cache-prime manual place save');

	return {
		token,
		roomId: room.roomId,
		analysisRequestId: analysis.analysisRequestId,
	};
}

export function manualPlaceSave(context) {
	const phase = exec.scenario.name;
	const phaseCode = phase === 'warmup' ? 'w' : phase === 'steady' ? 's' : 'p';
	const placeSlot = __ITER % PLACE_IDS_PER_VU;
	// Each VU rotates through a fixed pool, keeping DB growth bounded across long and repeated runs.
	const placeId = `${PLACE_ID_PREFIX}-${phaseCode}-v${__VU}-i${placeSlot}`;
	const response = saveManualPlace(
		context.roomId,
		context.analysisRequestId,
		manualPlacePayload(placeId, phase, __VU, placeSlot),
		authorizationHeaders(context.token),
		'manual_place_save'
	);

	let body = null;
	try {
		body = response.json();
	} catch (_) {
		// The checks below report a malformed or empty response without aborting later iterations.
	}
	const savedPlaces = body && body.data && Array.isArray(body.data.places) ? body.data.places : [];
	check(
		response,
		{
			'manual save returns HTTP 200': (result) => result.status === 200,
			'manual save returns success envelope': () => body !== null && body.success === true,
			'manual save returns requested place': () =>
				savedPlaces.length === 1 && savedPlaces[0].kakaoPlaceId === placeId,
		},
		{ operation: 'manual_place_save', phase }
	);
}

export function teardown(context) {
	if (!context || !context.roomId || !context.token) {
		return;
	}
	const response = http.del(
		`${BASE_URL}/api/v1/rooms/${encodeURIComponent(context.roomId)}/leave`,
		null,
		requestParams('teardown_leave_room', context.token)
	);
	if (response.status !== 200) {
		console.warn(`teardown room cleanup returned HTTP ${response.status}`);
	}
}

export function handleSummary(data) {
	// K6 otherwise includes setup_data, which contains the short-lived bearer token.
	delete data.setup_data;
	const duration = data.metrics['http_req_duration{name:manual_place_save}'];
	const failures = data.metrics['http_req_failed{name:manual_place_save}'];
	const dropped = data.metrics.dropped_iterations;
	const concise = [
		'manual_place_save summary',
		`requests=${duration ? duration.values.count : 0}`,
		`p95_ms=${duration ? duration.values['p(95)'] : 'n/a'}`,
		`p99_ms=${duration ? duration.values['p(99)'] : 'n/a'}`,
		`failure_rate=${failures ? failures.values.rate : 'n/a'}`,
		`dropped_iterations=${dropped ? dropped.values.count : 'n/a'}`,
	].join(' ');
	const outputs = { stdout: `${concise}\n` };
	if (SUMMARY_PATH) {
		outputs[SUMMARY_PATH] = JSON.stringify(data, null, 2);
	}
	return outputs;
}

function arrivalRateScenario(rate, duration, startTime, preAllocatedVUs, maxVUs) {
	return {
		executor: 'constant-arrival-rate',
		exec: 'manualPlaceSave',
		rate,
		timeUnit: '1s',
		duration,
		startTime,
		preAllocatedVUs,
		maxVUs,
		gracefulStop: GRACEFUL_STOP,
	};
}

function waitUntilReady() {
	let lastStatus = 0;
	let lastHealthStatus = 'UNKNOWN';
	for (let attempt = 1; attempt <= READINESS_MAX_ATTEMPTS; attempt += 1) {
		const response = http.get(
			`${BASE_URL}${READINESS_PATH}`,
			requestParams('setup_readiness')
		);
		lastStatus = response.status;
		const health = safeJson(response);
		lastHealthStatus = health && health.status ? health.status : 'UNKNOWN';
		if (response.status === 200 && lastHealthStatus === 'UP') {
			return;
		}
		if (attempt < READINESS_MAX_ATTEMPTS) {
			sleep(READINESS_POLL_INTERVAL_MS / 1000);
		}
	}
	fail(
		`readiness did not become UP after ${READINESS_MAX_ATTEMPTS} attempts `
		+ `(HTTP ${lastStatus}, status ${lastHealthStatus}, path ${READINESS_PATH})`
	);
}

function issueDevMasterToken() {
	const query = DEV_USER_ID ? `?userId=${encodeURIComponent(DEV_USER_ID)}` : '';
	const response = http.get(
		`${BASE_URL}/api/v1/auth/dev/master-token${query}`,
		requestParams('setup_dev_master_token')
	);
	const data = requireCommonData(response, [200], 'dev master token');
	const token = data.token && data.token.accessToken;
	if (!token) {
		fail('dev master token response did not contain data.token.accessToken');
	}
	return token;
}

function createRoom(headers) {
	const roomSuffix = String(Date.now()).slice(-10);
	const response = http.post(
		`${BASE_URL}/api/v1/rooms`,
		JSON.stringify({ name: `k6-cache-${roomSuffix}` }),
		requestParams('setup_create_room', null, headers)
	);
	const data = requireCommonData(response, [201], 'room creation');
	if (!data.roomId) {
		fail('room creation response did not contain data.roomId');
	}
	return data;
}

function createLinkAnalysisRequest(roomId, headers) {
	const response = http.post(
		`${BASE_URL}/api/v1/rooms/${encodeURIComponent(roomId)}/link-analysis-requests`,
		JSON.stringify({ originalUrl: LINK_URL, source: LINK_SOURCE }),
		requestParams('setup_create_link_analysis', null, headers)
	);
	const data = requireCommonData(response, [200, 201], 'link analysis request');
	if (data.analysisRequestId === null || data.analysisRequestId === undefined) {
		fail('link analysis response did not contain data.analysisRequestId');
	}
	return data;
}

function pollLinkAnalysisStatus(roomId, analysisRequestId, initialStatus, headers) {
	const terminalStatuses = new Set(['SUCCEEDED', 'FAILED', 'DISPATCH_FAILED']);
	let lastStatus = initialStatus || 'UNKNOWN';
	if (terminalStatuses.has(lastStatus)) {
		return lastStatus;
	}

	for (let attempt = 1; attempt <= STATUS_MAX_ATTEMPTS; attempt += 1) {
		const response = http.get(
			`${BASE_URL}/api/v1/rooms/${encodeURIComponent(roomId)}`
			+ `/link-analysis-requests/${analysisRequestId}`,
			requestParams('setup_link_analysis_status', null, headers)
		);
		if (response.status === 401 || response.status === 403 || response.status === 404) {
			fail(`link analysis status polling returned HTTP ${response.status}`);
		}
		const body = safeJson(response);
		if (response.status === 200 && body && body.success === true && body.data) {
			lastStatus = body.data.status || lastStatus;
			if (terminalStatuses.has(lastStatus)) {
				return lastStatus;
			}
		}
		if (attempt < STATUS_MAX_ATTEMPTS) {
			sleep(STATUS_POLL_INTERVAL_MS / 1000);
		}
	}

	// Manual save is intentionally valid for REQUESTED/PROCESSING links, so polling remains bounded.
	console.warn(
		`link analysis remained ${lastStatus} after ${STATUS_MAX_ATTEMPTS} status attempts; `
		+ 'continuing with the manual-save path'
	);
	return lastStatus;
}

function saveManualPlace(roomId, analysisRequestId, payload, headers, requestName) {
	return http.post(
		`${BASE_URL}/api/v1/rooms/${encodeURIComponent(roomId)}`
		+ `/link-analysis-requests/${analysisRequestId}/places/manual`,
		JSON.stringify(payload),
		requestParams(requestName, null, headers)
	);
}

function manualPlacePayload(placeId, phase, vuId, placeSlot) {
	return {
		kakaoPlaceId: placeId,
		name: `K6 ${phase} place ${vuId}-${placeSlot}`,
		address: '서울특별시 강남구 역삼동 1',
		roadAddress: '서울특별시 강남구 테헤란로 1',
		latitude: 37.498095,
		longitude: 127.02761,
		categoryName: '음식점 > 일식 > 돈까스',
		categoryGroupCode: 'FD6',
		phone: '02-0000-0000',
		placeUrl: null,
	};
}

function requireManualSave(response, expectedPlaceId, label) {
	const data = requireCommonData(response, [200], label);
	if (!Array.isArray(data.places)
			|| data.places.length !== 1
			|| data.places[0].kakaoPlaceId !== expectedPlaceId) {
		fail(`${label} response did not contain the requested place`);
	}
}

function requireCommonData(response, acceptedStatuses, label) {
	if (!acceptedStatuses.includes(response.status)) {
		fail(`${label} returned HTTP ${response.status}`);
	}
	const body = safeJson(response);
	if (!body || body.success !== true || body.data === null || body.data === undefined) {
		fail(`${label} did not return a successful CommonResponse with data`);
	}
	return body.data;
}

function safeJson(response) {
	try {
		return response.json();
	} catch (_) {
		return null;
	}
}

function authorizationHeaders(token) {
	return {
		Authorization: `Bearer ${token}`,
		'Content-Type': 'application/json',
	};
}

function requestParams(name, token, providedHeaders) {
	const headers = providedHeaders || (token ? authorizationHeaders(token) : undefined);
	return {
		timeout: REQUEST_TIMEOUT,
		tags: { name },
		...(headers ? { headers } : {}),
	};
}

function envPositiveInteger(name, fallback) {
	const raw = __ENV[name];
	if (raw === undefined || raw === '') {
		return fallback;
	}
	const value = Number(raw);
	if (!Number.isInteger(value) || value <= 0) {
		throw new Error(`${name} must be a positive integer`);
	}
	return value;
}

function envRatio(name, fallback) {
	const raw = __ENV[name];
	if (raw === undefined || raw === '') {
		return fallback;
	}
	const value = Number(raw);
	if (!Number.isFinite(value) || value <= 0 || value >= 1) {
		throw new Error(`${name} must be greater than 0 and less than 1`);
	}
	return value;
}

function envDuration(name, fallback) {
	const value = (__ENV[name] || fallback).trim();
	durationToMilliseconds(value, name);
	return value;
}

function durationToMilliseconds(value, name = 'duration') {
	const unitMilliseconds = { ms: 1, s: 1000, m: 60000, h: 3600000 };
	const parts = value.match(/\d+(?:\.\d+)?(?:ms|s|m|h)/g) || [];
	let total = 0;
	let cursor = 0;
	for (const part of parts) {
		if (value.slice(cursor, cursor + part.length) !== part) {
			throw new Error(`${name} must be a k6 duration such as 500ms, 30s, or 1m30s`);
		}
		const match = /^(\d+(?:\.\d+)?)(ms|s|m|h)$/.exec(part);
		total += Number(match[1]) * unitMilliseconds[match[2]];
		cursor += part.length;
	}
	if (cursor !== value.length || total <= 0) {
		throw new Error(`${name} must be a positive k6 duration such as 500ms, 30s, or 1m30s`);
	}
	return Math.ceil(total);
}

function millisecondsToDuration(milliseconds) {
	return `${milliseconds}ms`;
}

function validateConfiguration() {
	if (!/^https?:\/\//i.test(BASE_URL)) {
		throw new Error('BASE_URL must begin with http:// or https://');
	}
	if (!READINESS_PATH.startsWith('/')) {
		throw new Error('READINESS_PATH must begin with /');
	}
	if (!['WEB', 'APP'].includes(LINK_SOURCE)) {
		throw new Error('LINK_SOURCE must be WEB or APP');
	}
	if (!PLACE_ID_PREFIX || PLACE_ID_PREFIX.length > 60) {
		throw new Error('PLACE_ID_PREFIX must contain 1 to 60 characters');
	}
	if (DEV_USER_ID && (!/^\d+$/.test(DEV_USER_ID) || Number(DEV_USER_ID) <= 0)) {
		throw new Error('DEV_USER_ID must be a positive integer when provided');
	}
	validateVuRange('warmup', WARMUP_PRE_ALLOCATED_VUS, WARMUP_MAX_VUS);
	validateVuRange('steady', STEADY_PRE_ALLOCATED_VUS, STEADY_MAX_VUS);
	validateVuRange('spike', SPIKE_PRE_ALLOCATED_VUS, SPIKE_MAX_VUS);
	const largestVuId = WARMUP_MAX_VUS + STEADY_MAX_VUS + SPIKE_MAX_VUS;
	const longestPlaceId = `${PLACE_ID_PREFIX}-x-v${largestVuId}-i${PLACE_IDS_PER_VU - 1}`;
	if (longestPlaceId.length > 100) {
		throw new Error('PLACE_ID_PREFIX/VU/place-pool settings can exceed the 100-character kakaoPlaceId limit');
	}
}

function validateVuRange(phase, preAllocatedVUs, maxVUs) {
	if (preAllocatedVUs > maxVUs) {
		throw new Error(`${phase} pre-allocated VUs must not exceed max VUs`);
	}
}
