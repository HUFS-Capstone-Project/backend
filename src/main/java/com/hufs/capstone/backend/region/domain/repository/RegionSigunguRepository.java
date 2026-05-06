package com.hufs.capstone.backend.region.domain.repository;

import com.hufs.capstone.backend.region.domain.entity.RegionSigungu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegionSigunguRepository extends JpaRepository<RegionSigungu, Long> {

	Optional<RegionSigungu> findByCode(String code);

	@Query("""
			select sg
			from RegionSigungu sg
			join fetch sg.sido s
			where sg.code = :code
			  and sg.active = true
			""")
	Optional<RegionSigungu> findActiveByCode(@Param("code") String code);

	@Query("""
			select sg
			from RegionSigungu sg
			join fetch sg.sido s
			where s.code = :sidoCode
			  and sg.active = true
			order by sg.displayOrder asc, sg.code asc
			""")
	List<RegionSigungu> findActiveBySidoCode(@Param("sidoCode") String sidoCode);

	@Query("""
			select sg
			from RegionSigungu sg
			join fetch sg.sido s
			where sg.active = true
			order by s.displayOrder asc, sg.displayOrder asc, sg.code asc
			""")
	List<RegionSigungu> findAllActiveWithSido();
}
