package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.DateCourseCandidate;
import com.hufs.capstone.backend.course.domain.repository.DateCourseCandidateRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AvailablePoolBuilder {

	private final DateCourseCandidateRepository candidateRepository;
	private final BusinessHoursAtTimeChecker businessHoursAtTimeChecker;

	AvailablePool build(Long roomId, List<CategorySlotCommand> slots, Instant startDateTime, String sigunguCode) {
		Instant now = Instant.now();
		List<DateCourseCandidate> rows = candidateRepository.findCandidates(roomId, slots, now, sigunguCode);
		if (rows.isEmpty()) {
			return new AvailablePool(List.of());
		}

		List<AvailableCandidate> candidates = rows.stream()
				.filter(row -> row.businessHoursJson() != null)
				.filter(row -> businessHoursAtTimeChecker.isOpenAt(row.businessHoursJson(), startDateTime))
				.map(AvailableCandidate::from)
				.toList();

		return new AvailablePool(candidates);
	}
}
