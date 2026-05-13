package com.hufs.capstone.backend.room.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.QPlace;
import com.hufs.capstone.backend.place.domain.entity.QRoomPlace;
import com.hufs.capstone.backend.room.domain.entity.QRoom;
import com.hufs.capstone.backend.room.domain.entity.QRoomMember;
import com.hufs.capstone.backend.room.domain.entity.RoomMember;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.util.StringUtils;

public class RoomMemberSearchRepositoryImpl implements RoomMemberSearchRepository {

	private static final QRoomMember ROOM_MEMBER = QRoomMember.roomMember;
	private static final QRoom ROOM = QRoom.room;
	private static final QRoomPlace ROOM_PLACE = QRoomPlace.roomPlace;
	private static final QPlace PLACE = QPlace.place;

	private final JPAQueryFactory queryFactory;

	public RoomMemberSearchRepositoryImpl(EntityManager entityManager) {
		this.queryFactory = new JPAQueryFactory(entityManager);
	}

	@Override
	public List<RoomMember> findMyRooms(Long userId, String keyword) {
		return queryFactory
				.selectFrom(ROOM_MEMBER)
				.join(ROOM_MEMBER.room, ROOM).fetchJoin()
				.where(
						ROOM_MEMBER.userId.eq(userId),
						keywordContains(keyword)
				)
				.orderBy(ROOM_MEMBER.pinned.desc(), ROOM.createdAt.desc())
				.fetch();
	}

	private static BooleanExpression keywordContains(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return null;
		}
		return ROOM.name.containsIgnoreCase(keyword)
				.or(placeNameExists(keyword));
	}

	private static BooleanExpression placeNameExists(String keyword) {
		return JPAExpressions
				.selectOne()
				.from(ROOM_PLACE)
				.join(ROOM_PLACE.place, PLACE)
				.where(
						ROOM_PLACE.room.eq(ROOM),
						PLACE.name.isNotNull(),
						PLACE.name.containsIgnoreCase(keyword)
				)
				.exists();
	}
}
