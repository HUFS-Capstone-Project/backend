package com.hufs.capstone.backend.course.application;

import com.hufs.capstone.backend.course.application.dto.AvailableCandidate;
import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.place.domain.entity.Place;
import java.util.List;

class AvailablePool {

	private final List<AvailableCandidate> all;

	AvailablePool(List<AvailableCandidate> candidates) {
		this.all = List.copyOf(candidates);
	}

	List<AvailableCandidate> all() {
		return all;
	}

	List<AvailableCandidate> forSlot(CategorySlotCommand slot) {
		return all.stream()
				.filter(c -> matches(c, slot))
				.toList();
	}

	boolean isEmpty() {
		return all.isEmpty();
	}

	private static boolean matches(AvailableCandidate candidate, CategorySlotCommand slot) {
		Place place = candidate.roomPlace().getPlace();
		String categoryCode = place.getServiceCategory().getCode();
		String tagCode = place.getServiceTag().getCode();
		if (!categoryCode.equals(slot.categoryCode())) {
			return false;
		}
		return slot.isWildcard() || tagCode.equals(slot.tagCode());
	}
}
