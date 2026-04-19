package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlaceTagRepository extends JpaRepository<PlaceTag, Long> {

	Optional<PlaceTag> findByCategoryAndCode(PlaceCategory category, String code);

	@Query("""
			select t
			from PlaceTag t
			join fetch t.category c
			left join fetch t.tagGroup g
			where c.isActive = true
			  and t.isActive = true
			  and (g is null or g.isActive = true)
			order by c.sortOrder asc, c.id asc, g.sortOrder asc, g.id asc, t.sortOrder asc, t.id asc
			""")
	List<PlaceTag> findActiveTaxonomyTags();
}
