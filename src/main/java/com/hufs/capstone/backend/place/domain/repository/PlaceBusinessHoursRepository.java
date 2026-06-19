package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.PlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceBusinessHoursRepository extends JpaRepository<PlaceBusinessHours, Long> {

	Optional<PlaceBusinessHours> findByKakaoPlaceId(String kakaoPlaceId);

	List<PlaceBusinessHours> findByKakaoPlaceIdIn(Collection<String> kakaoPlaceIds);

	@Query("""
			select pbh
			from PlaceBusinessHours pbh
			where pbh.businessHoursStatus in :statuses
			  and pbh.businessHoursJobId is not null
			  and pbh.businessHoursJobId <> ''
			  and (
			      (pbh.lastPolledAt is null and pbh.updatedAt <= :dueBefore)
			      or pbh.lastPolledAt <= :dueBefore
			  )
			order by pbh.updatedAt asc, pbh.id asc
			""")
	List<PlaceBusinessHours> findPollable(
			@Param("statuses") Collection<BusinessHoursStatus> statuses,
			@Param("dueBefore") Instant dueBefore,
			Pageable pageable
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update PlaceBusinessHours pbh
			set pbh.lastPolledAt = :claimedAt
			where pbh.id = :id
			  and pbh.businessHoursStatus in :statuses
			  and pbh.businessHoursJobId is not null
			  and pbh.businessHoursJobId <> ''
			  and (
			      (pbh.lastPolledAt is null and pbh.updatedAt <= :dueBefore)
			      or pbh.lastPolledAt <= :dueBefore
			  )
			""")
	int claimPollable(
			@Param("id") Long id,
			@Param("statuses") Collection<BusinessHoursStatus> statuses,
			@Param("dueBefore") Instant dueBefore,
			@Param("claimedAt") Instant claimedAt
	);
}
