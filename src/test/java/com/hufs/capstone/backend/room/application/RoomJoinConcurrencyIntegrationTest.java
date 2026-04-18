package com.hufs.capstone.backend.room.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.room.application.dto.CreateRoomResult;
import com.hufs.capstone.backend.room.domain.repository.RoomMemberRepository;
import com.hufs.capstone.backend.room.domain.repository.RoomRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
class RoomJoinConcurrencyIntegrationTest {

	private static final Long INITIAL_MEMBER_USER_ID = 1L;

	@Autowired
	private RoomCommandService roomCommandService;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private RoomMemberRepository roomMemberRepository;

	@MockitoBean
	private RoomJoinRateLimiter roomJoinRateLimiter;

	private CreateRoomResult createdRoom;

	@BeforeEach
	void setUp() {
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		when(roomJoinRateLimiter.allow(anyLong(), anyString())).thenReturn(true);
		createdRoom = roomCommandService.createRoom(INITIAL_MEMBER_USER_ID, "동시성 테스트 방");
	}

	@AfterEach
	void tearDown() {
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
	}

	@Test
	void concurrentJoinShouldNotExceedRoomCapacity() throws Exception {
		List<Long> joinUserIds = List.of(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
		List<Object> outcomes = runConcurrentJoin(joinUserIds, createdRoom.inviteCode());

		long successCount = outcomes.stream()
				.filter(Boolean.class::isInstance)
				.map(Boolean.class::cast)
				.filter(Boolean::booleanValue)
				.count();
		long conflictCount = outcomes.stream()
				.filter(BusinessException.class::isInstance)
				.map(BusinessException.class::cast)
				.filter(ex -> ex.getErrorCode() == ErrorCode.E409_CONFLICT)
				.count();

		Long roomId = roomRepository.findByPublicId(createdRoom.roomId()).orElseThrow().getId();
		long finalMemberCount = roomMemberRepository.countByRoomId(roomId);

		assertThat(successCount).isEqualTo(5L);
		assertThat(conflictCount).isEqualTo(3L);
		assertThat(finalMemberCount).isEqualTo(6L);
	}

	private List<Object> runConcurrentJoin(List<Long> userIds, String inviteCode) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(userIds.size());
		try {
			CountDownLatch ready = new CountDownLatch(userIds.size());
			CountDownLatch start = new CountDownLatch(1);
			List<Future<Object>> futures = new ArrayList<>();

			for (Long userId : userIds) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					if (!start.await(3, TimeUnit.SECONDS)) {
						throw new IllegalStateException("start latch timeout");
					}
					try {
						roomCommandService.joinByInviteCode(userId, inviteCode, "127.0.0.1");
						return Boolean.TRUE;
					} catch (BusinessException ex) {
						return ex;
					}
				}));
			}

			if (!ready.await(3, TimeUnit.SECONDS)) {
				throw new IllegalStateException("ready latch timeout");
			}
			start.countDown();

			List<Object> outcomes = new ArrayList<>();
			for (Future<Object> future : futures) {
				outcomes.add(future.get(5, TimeUnit.SECONDS));
			}
			return outcomes;
		} finally {
			executor.shutdownNow();
		}
	}
}
