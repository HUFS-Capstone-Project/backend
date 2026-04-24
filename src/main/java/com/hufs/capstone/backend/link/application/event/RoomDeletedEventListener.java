package com.hufs.capstone.backend.link.application.event;

import com.hufs.capstone.backend.link.application.RoomLinkCleanupWriteService;
import com.hufs.capstone.backend.room.application.event.RoomDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomDeletedEventListener {

	private final RoomLinkCleanupWriteService roomLinkCleanupWriteService;

	@EventListener
	public void onRoomDeleted(RoomDeletedEvent event) {
		roomLinkCleanupWriteService.deleteAllByRoomId(event.roomId());
		log.info("방 링크 관련 데이터 정리를 완료했습니다. roomId={}, roomPublicId={}", event.roomId(), event.roomPublicId());
	}
}

