package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceBusinessHoursRepository extends JpaRepository<PlaceBusinessHours, Long> {

	Optional<PlaceBusinessHours> findByKakaoPlaceId(String kakaoPlaceId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select pbh from PlaceBusinessHours pbh where pbh.kakaoPlaceId = :kakaoPlaceId")
	Optional<PlaceBusinessHours> findByKakaoPlaceIdForUpdate(@Param("kakaoPlaceId") String kakaoPlaceId);

	List<PlaceBusinessHours> findByKakaoPlaceIdIn(Collection<String> kakaoPlaceIds);

	@Query("""
			select pbh
			from PlaceBusinessHours pbh
			where pbh.businessHoursStatus in (
			    com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus.PENDING,
			    com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus.FETCHING
			)
			  and pbh.businessHoursJobId is not null
			  and pbh.businessHoursJobId <> ''
			  and coalesce(pbh.lastPolledAt, pbh.updatedAt) <= :dueBefore
			order by coalesce(pbh.lastPolledAt, pbh.updatedAt) asc, pbh.id asc
			""")
	List<PlaceBusinessHours> findPollable(
			@Param("dueBefore") Instant dueBefore,
			Pageable pageable
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update PlaceBusinessHours pbh
			set pbh.lastPolledAt = :claimedAt
			where pbh.id = :id
			  and pbh.businessHoursStatus in (
			      com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus.PENDING,
			      com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus.FETCHING
			  )
			  and pbh.businessHoursJobId is not null
			  and pbh.businessHoursJobId <> ''
			  and coalesce(pbh.lastPolledAt, pbh.updatedAt) <= :dueBefore
			""")
	int claimPollable(
			@Param("id") Long id,
			@Param("dueBefore") Instant dueBefore,
			@Param("claimedAt") Instant claimedAt
	);

}
