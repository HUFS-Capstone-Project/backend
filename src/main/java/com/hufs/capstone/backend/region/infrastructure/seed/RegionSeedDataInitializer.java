package com.hufs.capstone.backend.region.infrastructure.seed;

import com.hufs.capstone.backend.global.cache.ReferenceDataCacheInvalidator;
import com.hufs.capstone.backend.region.domain.entity.RegionSido;
import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import com.hufs.capstone.backend.region.domain.repository.RegionSidoRepository;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import com.hufs.capstone.backend.region.infrastructure.config.RegionSeedAsyncConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class RegionSeedDataInitializer implements ApplicationRunner {

	private static final String SEED_PATH = "reference/regions.tsv";
	private static final int TRANSACTION_BATCH_SIZE = 25;
	// Source basis: MOIS standard code system 법정동코드목록조회, using active sido/sigungu legal-dong codes.
	private static final String TYPE_SIDO = "SIDO";
	private static final String TYPE_SIGUNGU = "SIGUNGU";
	private static final int EXPECTED_SIDO_COUNT = 17;
	private static final int EXPECTED_SIGUNGU_COUNT = 255;

	private final RegionSidoRepository regionSidoRepository;
	private final RegionSigunguRepository regionSigunguRepository;
	private final TransactionTemplate transactionTemplate;
	private final ThreadPoolTaskExecutor seedTaskExecutor;
	private final ReferenceDataCacheInvalidator cacheInvalidator;
	private final RegionSeedReadinessHealthIndicator readiness;
	private final boolean async;
	private final AtomicBoolean shutdownRequested = new AtomicBoolean();

	private volatile Future<?> seedTask;

	public RegionSeedDataInitializer(
			RegionSidoRepository regionSidoRepository,
			RegionSigunguRepository regionSigunguRepository,
			TransactionTemplate transactionTemplate,
			@Qualifier(RegionSeedAsyncConfig.REGION_SEED_TASK_EXECUTOR)
			ThreadPoolTaskExecutor seedTaskExecutor,
			ReferenceDataCacheInvalidator cacheInvalidator,
			RegionSeedReadinessHealthIndicator readiness,
			@Value("${app.region.seed.async:false}") boolean async
	) {
		this.regionSidoRepository = regionSidoRepository;
		this.regionSigunguRepository = regionSigunguRepository;
		this.transactionTemplate = transactionTemplate;
		this.seedTaskExecutor = seedTaskExecutor;
		this.cacheInvalidator = cacheInvalidator;
		this.readiness = readiness;
		this.async = async;
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		readiness.markSeeding();
		if (async) {
			try {
				seedTask = seedTaskExecutor.submit(this::seedAsync);
			} catch (RuntimeException exception) {
				readiness.markFailed();
				throw exception;
			}
			return;
		}
		try {
			completeSeed();
		} catch (IOException | RuntimeException exception) {
			readiness.markFailed();
			throw exception;
		}
	}

	@EventListener(ContextClosedEvent.class)
	public void stopOnShutdown() {
		shutdownRequested.set(true);
		Future<?> currentTask = seedTask;
		if (currentTask != null) {
			currentTask.cancel(true);
		}
	}

	private void seedAsync() {
		try {
			if (completeSeed()) {
				log.info("Region seed initialization completed.");
			}
		} catch (IOException | RuntimeException exception) {
			if (shouldStop()) {
				log.info("Region seed initialization stopped because application shutdown started.");
				return;
			}
			readiness.markFailed();
			log.error("Region seed initialization failed.", exception);
		}
	}

	private boolean completeSeed() throws IOException {
		if (!seed()) {
			return false;
		}
		cacheInvalidator.clearRegionCaches();
		readiness.markReady();
		return true;
	}

	private boolean seed() throws IOException {
		List<SeedRow> rows = readRows();
		validateRows(rows);
		List<SeedRow> sidoRows = rows.stream()
				.filter(row -> TYPE_SIDO.equals(row.type()))
				.toList();
		if (!seedRows(sidoRows, this::upsertSido)) {
			return false;
		}

		List<SeedRow> sigunguRows = rows.stream()
				.filter(row -> TYPE_SIGUNGU.equals(row.type()))
				.toList();
		return seedRows(sigunguRows, this::upsertSigungu);
	}

	private static void validateRows(List<SeedRow> rows) {
		List<SeedRow> sidoRows = rows.stream().filter(row -> TYPE_SIDO.equals(row.type())).toList();
		List<SeedRow> sigunguRows = rows.stream().filter(row -> TYPE_SIGUNGU.equals(row.type())).toList();
		if (sidoRows.size() != EXPECTED_SIDO_COUNT || sigunguRows.size() != EXPECTED_SIGUNGU_COUNT) {
			throw new IllegalStateException(
					"Region seed must contain exactly " + EXPECTED_SIDO_COUNT + " SIDO and "
							+ EXPECTED_SIGUNGU_COUNT + " SIGUNGU rows"
			);
		}
		if (rows.size() != sidoRows.size() + sigunguRows.size()) {
			throw new IllegalStateException("Region seed contains an unsupported row type");
		}

		Set<String> sidoCodes = new HashSet<>();
		for (SeedRow sido : sidoRows) {
			if (!sidoCodes.add(sido.code())) {
				throw new IllegalStateException("Duplicate SIDO code in region seed: " + sido.code());
			}
			if (sido.sidoCode() != null) {
				throw new IllegalStateException("SIDO row must not have a parent code: " + sido.code());
			}
		}

		Set<String> sigunguCodes = new HashSet<>();
		for (SeedRow sigungu : sigunguRows) {
			if (!sigunguCodes.add(sigungu.code())) {
				throw new IllegalStateException("Duplicate SIGUNGU code in region seed: " + sigungu.code());
			}
			if (!sidoCodes.contains(sigungu.sidoCode())) {
				throw new IllegalStateException(
						"SIGUNGU row has an unknown SIDO code: " + sigungu.code() + " -> " + sigungu.sidoCode()
				);
			}
		}
	}

	private boolean seedRows(List<SeedRow> rows, Consumer<SeedRow> upsert) {
		for (int start = 0; start < rows.size(); start += TRANSACTION_BATCH_SIZE) {
			if (shouldStop()) {
				return false;
			}
			int end = Math.min(start + TRANSACTION_BATCH_SIZE, rows.size());
			List<SeedRow> batch = rows.subList(start, end);
			boolean committed = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
				for (SeedRow row : batch) {
					if (shouldStop()) {
						status.setRollbackOnly();
						return false;
					}
					upsert.accept(row);
				}
				return true;
			}));
			if (!committed) {
				return false;
			}
		}
		return true;
	}

	private boolean shouldStop() {
		return shutdownRequested.get() || Thread.currentThread().isInterrupted();
	}

	private List<SeedRow> readRows() throws IOException {
		ClassPathResource resource = new ClassPathResource(SEED_PATH);
		List<SeedRow> rows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				resource.getInputStream(),
				StandardCharsets.UTF_8
		))) {
			String line;
			boolean first = true;
			while ((line = reader.readLine()) != null) {
				if (first) {
					first = false;
					continue;
				}
				if (line.isBlank()) {
					continue;
				}
				rows.add(SeedRow.parse(line));
			}
		}
		return List.copyOf(rows);
	}

	private RegionSido upsertSido(SeedRow row) {
		return regionSidoRepository.findByCode(row.code())
				.map(sido -> {
					sido.updateMetadata(row.name(), row.displayOrder(), true);
					return sido;
				})
				.orElseGet(() -> regionSidoRepository.save(RegionSido.create(
						row.code(),
						row.name(),
						row.displayOrder(),
						true
				)));
	}

	private void upsertSigungu(SeedRow row) {
		RegionSido sido = regionSidoRepository.findByCode(row.sidoCode())
				.orElseThrow(() -> new IllegalStateException("Missing sido for sigungu seed: " + row.sidoCode()));
		regionSigunguRepository.findByCode(row.code())
				.ifPresentOrElse(
						sigungu -> sigungu.updateMetadata(sido, row.name(), row.displayOrder(), true),
						() -> {
							RegionSigungu sigungu = RegionSigungu.create(
									sido,
									row.code(),
									row.name(),
									row.displayOrder(),
									true
							);
							regionSigunguRepository.save(sigungu);
						}
			);
	}

	private record SeedRow(
			String type,
			String code,
			String sidoCode,
			String name,
			Integer displayOrder
	) {

		private static SeedRow parse(String line) {
			String[] values = line.split("\t", -1);
			if (values.length != 5) {
				throw new IllegalStateException("Invalid region seed row: " + line);
			}
			return new SeedRow(
					values[0].trim(),
					values[1].trim(),
					trimToNull(values[2]),
					values[3].trim(),
					Integer.valueOf(values[4].trim())
			);
		}

		private static String trimToNull(String value) {
			String trimmed = value == null ? "" : value.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}
	}
}
