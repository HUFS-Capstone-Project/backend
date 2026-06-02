package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.place.application.BusinessHoursDisplayResolver;
import com.hufs.capstone.backend.place.domain.enums.BusinessStatus;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessHoursAtTimeChecker {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	private static final Set<BusinessStatus> OPEN_STATUSES = Set.of(
			BusinessStatus.OPEN,
			BusinessStatus.OPEN_24_HOURS,
			BusinessStatus.CLOSING_SOON
	);

	private final BusinessHoursDisplayResolver resolver;

	public boolean isOpenAt(String businessHoursJson, Instant plannedDateTime) {
		ZonedDateTime at = plannedDateTime.atZone(SEOUL_ZONE);
		BusinessStatus status = resolver.statusAt(businessHoursJson, at);
		return OPEN_STATUSES.contains(status);
	}
}
