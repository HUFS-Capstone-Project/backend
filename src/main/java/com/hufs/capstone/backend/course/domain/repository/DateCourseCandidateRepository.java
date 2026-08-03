package com.hufs.capstone.backend.course.domain.repository;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.DateCourseCandidate;
import java.time.Instant;
import java.util.List;

public interface DateCourseCandidateRepository {

	List<DateCourseCandidate> findCandidates(Long roomId, List<CategorySlotCommand> slots, Instant now,
			String sigunguCode);
}
