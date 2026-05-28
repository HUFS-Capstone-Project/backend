package com.hufs.capstone.backend.course.domain.repository;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.time.Instant;
import java.util.List;

public interface DateCourseCandidateRepository {

	List<RoomPlace> findCandidates(Long roomId, List<CategorySlotCommand> slots, Instant now, String sigunguCode);
}
