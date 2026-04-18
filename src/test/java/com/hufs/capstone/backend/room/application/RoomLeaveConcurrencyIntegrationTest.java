package com.hufs.capstone.backend.room.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingHistoryRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.room.domain.entity.Room;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
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

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
class RoomLeaveConcurrencyIntegrationTest {

	private static final Long USER_A = 101L;
	private static final Long USER_B = 102L;
	private static final String ROOM_PUBLIC_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

	@Autowired
	private RoomCommandService roomCommandService;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private RoomMemberRepository roomMemberRepository;

	@Autowired
	private RoomLinkRepository roomLinkRepository;

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private LinkProcessingHistoryRepository linkProcessingHistoryRepository;

	private Long roomDbId;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		Room room = roomRepository.saveAndFlush(Room.create(ROOM_PUBLIC_ID, "Leave 테스트 방", "LEAVE12345678", USER_A));
		roomDbId = room.getId();
		roomMemberRepository.saveAndFlush(RoomMember.join(room, USER_A));
		roomMemberRepository.saveAndFlush(RoomMember.join(room, USER_B));

		Link link1 = linkRepository.saveAndFlush(Link.register("https://example.com/1", "https://example.com/1", "job-1"));
		Link link2 = linkRepository.saveAndFlush(Link.register("https://example.com/2", "https://example.com/2", "job-2"));
		roomLinkRepository.saveAndFlush(RoomLink.bind(room, link1));
		roomLinkRepository.saveAndFlush(RoomLink.bind(room, link2));
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	void leaveShouldKeepRoomLinksUntilLastMemberLeaves() {
		roomCommandService.leaveRoom(USER_A, ROOM_PUBLIC_ID);

		assertThat(roomRepository.findByPublicId(ROOM_PUBLIC_ID)).isPresent();
		assertThat(roomMemberRepository.countByRoomId(roomDbId)).isEqualTo(1L);
		assertThat(roomLinkRepository.countByRoomId(roomDbId)).isEqualTo(2L);
		assertThat(linkRepository.count()).isEqualTo(2L);
	}

	@Test
	void concurrentLeaveShouldCleanupRoomAggregateConsistency() throws Exception {
		List<Object> outcomes = runConcurrentLeave(List.of(USER_A, USER_B), ROOM_PUBLIC_ID);

		long successCount = outcomes.stream()
				.filter(Boolean.class::isInstance)
				.map(Boolean.class::cast)
				.filter(Boolean::booleanValue)
				.count();

		assertThat(successCount).isEqualTo(2L);
		assertThat(roomRepository.findByPublicId(ROOM_PUBLIC_ID)).isEmpty();
		assertThat(roomMemberRepository.countByRoomId(roomDbId)).isEqualTo(0L);
		assertThat(roomLinkRepository.countByRoomId(roomDbId)).isEqualTo(0L);
		assertThat(linkRepository.count()).isEqualTo(2L);
	}

	private List<Object> runConcurrentLeave(List<Long> userIds, String roomPublicId) throws Exception {
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
					roomCommandService.leaveRoom(userId, roomPublicId);
					return Boolean.TRUE;
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

	private void cleanDatabase() {
		roomLinkRepository.deleteAll();
		linkProcessingHistoryRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		linkRepository.deleteAll();
	}
}
