package com.hufs.capstone.backend.region.infrastructure.seed;

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
import java.util.List;
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

	private final RegionSidoRepository regionSidoRepository;
	private final RegionSigunguRepository regionSigunguRepository;
	private final TransactionTemplate transactionTemplate;
	private final ThreadPoolTaskExecutor seedTaskExecutor;
	private final boolean async;
	private final AtomicBoolean shutdownRequested = new AtomicBoolean();

	private volatile Future<?> seedTask;

	public RegionSeedDataInitializer(
			RegionSidoRepository regionSidoRepository,
			RegionSigunguRepository regionSigunguRepository,
			TransactionTemplate transactionTemplate,
			@Qualifier(RegionSeedAsyncConfig.REGION_SEED_TASK_EXECUTOR)
			ThreadPoolTaskExecutor seedTaskExecutor,
			@Value("${app.region.seed.async:false}") boolean async
	) {
		this.regionSidoRepository = regionSidoRepository;
		this.regionSigunguRepository = regionSigunguRepository;
		this.transactionTemplate = transactionTemplate;
		this.seedTaskExecutor = seedTaskExecutor;
		this.async = async;
	}

	@Override
	public void run(ApplicationArguments args) throws IOException {
		if (async) {
			seedTask = seedTaskExecutor.submit(this::seedAsync);
			return;
		}
		seed();
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
			if (seed()) {
				log.info("Region seed initialization completed.");
			}
		} catch (IOException | RuntimeException exception) {
			if (shouldStop()) {
				log.info("Region seed initialization stopped because application shutdown started.");
				return;
			}
			log.error("Region seed initialization failed.", exception);
		}
	}

	private boolean seed() throws IOException {
		List<SeedRow> rows = readRows();
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
