package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTagGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceTagGroupRepository extends JpaRepository<PlaceTagGroup, Long> {

	Optional<PlaceTagGroup> findByCategoryAndCode(PlaceCategory category, String code);
}
