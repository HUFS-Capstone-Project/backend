package com.hufs.capstone.backend.course.application.dto;

import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.util.List;

public record CourseSelectionResult(
		List<RoomPlace> pickedPlaces,
		List<Integer> skippedSlotIndices
) {
}
