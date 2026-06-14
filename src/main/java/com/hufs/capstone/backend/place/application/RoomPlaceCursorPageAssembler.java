package com.hufs.capstone.backend.place.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.global.pagination.CursorCodec;
import com.hufs.capstone.backend.global.pagination.CursorPageResult;
import com.hufs.capstone.backend.place.application.dto.RoomPlaceCursor;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RoomPlaceCursorPageAssembler {

	private final ObjectMapper objectMapper;

	RoomPlaceCursor decode(String cursor) {
		return cursorCodec().decode(cursor);
	}

	<T> CursorPageResult<T> assemble(List<T> items, List<RoomPlace> pageItems, int limit, long totalCount, boolean hasNext) {
		String nextCursor = hasNext ? cursorCodec().encode(toCursor(pageItems.get(pageItems.size() - 1))) : null;
		return new CursorPageResult<>(items, limit, totalCount, nextCursor, hasNext);
	}

	private CursorCodec<RoomPlaceCursor> cursorCodec() {
		return new CursorCodec<>(objectMapper, RoomPlaceCursor.class);
	}

	private static RoomPlaceCursor toCursor(RoomPlace roomPlace) {
		return new RoomPlaceCursor(roomPlace.getCreatedAt(), roomPlace.getId());
	}
}
