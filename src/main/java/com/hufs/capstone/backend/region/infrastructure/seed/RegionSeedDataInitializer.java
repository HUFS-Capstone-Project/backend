package com.hufs.capstone.backend.region.infrastructure.seed;

import com.hufs.capstone.backend.region.domain.entity.RegionSido;
import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import com.hufs.capstone.backend.region.domain.repository.RegionSidoRepository;
import com.hufs.capstone.backend.region.domain.repository.RegionSigunguRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RegionSeedDataInitializer implements ApplicationRunner {

	private static final String SEED_PATH = "reference/regions.tsv";
	// Source basis: MOIS standard code system 법정동코드목록조회, using active sido/sigungu legal-dong codes.
	private static final String TYPE_SIDO = "SIDO";
	private static final String TYPE_SIGUNGU = "SIGUNGU";

	private final RegionSidoRepository regionSidoRepository;
	private final RegionSigunguRepository regionSigunguRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws IOException {
		List<SeedRow> rows = readRows();
		for (SeedRow row : rows) {
			if (TYPE_SIDO.equals(row.type())) {
				upsertSido(row);
			}
		}
		for (SeedRow row : rows) {
			if (TYPE_SIGUNGU.equals(row.type())) {
				upsertSigungu(row);
			}
		}
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
