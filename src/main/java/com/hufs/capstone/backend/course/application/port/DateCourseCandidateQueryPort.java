package com.hufs.capstone.backend.course.application.port;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.DateCourseCandidate;
import java.time.Instant;
import java.util.List;

public interface DateCourseCandidateQueryPort {

	List<DateCourseCandidate> findCandidates(Long roomId, List<CategorySlotCommand> slots, Instant now,
			String sigunguCode);
}
