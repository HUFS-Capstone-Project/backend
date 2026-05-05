package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;

public record SaveManualRoomPlaceCommand(
		PlaceSnapshot snapshot
) {
}
