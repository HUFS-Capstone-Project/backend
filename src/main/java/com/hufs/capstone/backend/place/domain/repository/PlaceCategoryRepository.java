package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlaceCategoryRepository extends JpaRepository<PlaceCategory, Long> {

	Optional<PlaceCategory> findByCode(String code);

	@Query("""
			select c
			from PlaceCategory c
			where c.isActive = true
			order by c.sortOrder asc, c.id asc
			""")
	List<PlaceCategory> findActiveCategories();
}
