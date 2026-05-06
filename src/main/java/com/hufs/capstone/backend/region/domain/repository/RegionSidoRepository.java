package com.hufs.capstone.backend.region.domain.repository;

import com.hufs.capstone.backend.region.domain.entity.RegionSido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionSidoRepository extends JpaRepository<RegionSido, Long> {

	Optional<RegionSido> findByCode(String code);

	Optional<RegionSido> findByCodeAndActiveTrue(String code);

	List<RegionSido> findAllByActiveTrueOrderByDisplayOrderAscCodeAsc();
}
