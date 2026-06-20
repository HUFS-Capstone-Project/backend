package com.hufs.capstone.backend.link.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hufs.capstone.backend.global.config.JpaAuditingConfig;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkCandidate;
import com.hufs.capstone.backend.link.domain.entity.RoomLink;
import com.hufs.capstone.backend.link.domain.entity.RoomLinkCandidateOverride;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase
@Import(JpaAuditingConfig.class)
class RoomLinkCandidateOverrideRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private RoomLinkCandidateOverrideRepository repository;

	@Test
	void deleteByRoomIdShouldDeleteOverridesForRoomLinks() {
		Room room = entityManager.persist(Room.create(UUID.randomUUID().toString(), "room", "invite-code-1", 100L));
		Link link = entityManager.persist(Link.register("https://example.com/p/1", "https://example.com/p/1", "job-1"));
		RoomLink roomLink = entityManager.persist(RoomLink.bind(room, link));
		LinkCandidate candidate = entityManager.persist(LinkCandidate.create(
				link,
				0,
				new PlaceCandidateSnapshot("kakao-1", "place", null, null, null, null, null, null, null, null)
		));
		entityManager.persist(RoomLinkCandidateOverride.create(
				roomLink,
				candidate,
				100L,
				PlaceSnapshot.kakao("kakao-2", "updated place", null, null, null, null, null, null, null, null)
		));
		entityManager.flush();

		int deleted = repository.deleteByRoomId(room.getId());
		entityManager.flush();
		entityManager.clear();

		assertThat(deleted).isEqualTo(1);
		assertThat(repository.count()).isZero();
	}
}
