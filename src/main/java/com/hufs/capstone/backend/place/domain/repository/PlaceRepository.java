package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.enums.PlaceSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	Optional<Place> findBySourceAndExternalPlaceId(PlaceSource source, String externalPlaceId);

	Optional<Place> findByKakaoPlaceId(String kakaoPlaceId);

	@Query("""
			select p
			from Place p
			join fetch p.serviceCategory c
			join fetch p.serviceTag t
			where p.kakaoPlaceId = :kakaoPlaceId
			""")
	Optional<Place> findWithTaxonomyByKakaoPlaceId(@Param("kakaoPlaceId") String kakaoPlaceId);
}
