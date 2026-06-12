package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.link.domain.entity.QLink;
import com.hufs.capstone.backend.link.domain.entity.QRoomLink;
import com.hufs.capstone.backend.place.domain.entity.QPlace;
import com.hufs.capstone.backend.place.domain.entity.QPlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.QPlaceTag;
import com.hufs.capstone.backend.place.domain.entity.QRoomPlace;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.PlaceSource;
import com.hufs.capstone.backend.room.domain.entity.QRoom;
import com.hufs.capstone.backend.room.domain.entity.QRoomMember;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

public class RoomPlaceSearchRepositoryImpl implements RoomPlaceSearchRepository {

	private static final QRoomPlace ROOM_PLACE = QRoomPlace.roomPlace;
	private static final QPlace PLACE = QPlace.place;
	private static final QPlaceCategory CATEGORY = QPlaceCategory.placeCategory;
	private static final QPlaceTag TAG = QPlaceTag.placeTag;
	private static final QRoom ROOM = QRoom.room;
	private static final QRoomMember ROOM_MEMBER = QRoomMember.roomMember;
	private static final QRoomLink ORIGIN_ROOM_LINK = QRoomLink.roomLink;
	private static final QLink ORIGIN_LINK = QLink.link;

	private final JPAQueryFactory queryFactory;

	public RoomPlaceSearchRepositoryImpl(EntityManager entityManager) {
		this.queryFactory = new JPAQueryFactory(entityManager);
	}

	@Override
	public Page<RoomPlace> searchRoomPlaces(
			Long roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy,
			Pageable pageable
	) {
		List<RoomPlace> content = baseRoomPlaceQuery()
				.where(
						roomIdEq(roomId),
						keywordContains(keyword),
						categoryCodeEq(categoryCode),
						tagCodeEq(tagCode),
						sidoCodeEq(sidoCode),
						sigunguCodeEq(sigunguCode),
						createdByEq(createdBy)
				)
				.orderBy(ROOM_PLACE.createdAt.desc(), ROOM_PLACE.id.desc())
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		Long total = queryFactory
				.select(ROOM_PLACE.id.countDistinct())
				.from(ROOM_PLACE)
				.join(ROOM_PLACE.place, PLACE)
				.join(PLACE.serviceCategory, CATEGORY)
				.join(PLACE.serviceTag, TAG)
				.where(
						roomIdEq(roomId),
						keywordContains(keyword),
						categoryCodeEq(categoryCode),
						tagCodeEq(tagCode),
						sidoCodeEq(sidoCode),
						sigunguCodeEq(sigunguCode),
						createdByEq(createdBy)
				)
				.fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	@Override
	public List<RoomPlace> searchRoomPlacesAfterCursor(
			Long roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy,
			Instant cursorCreatedAt,
			Long cursorRoomPlaceId,
			int limit
	) {
		return baseRoomPlaceQuery()
				.where(
						roomIdEq(roomId),
						keywordContains(keyword),
						categoryCodeEq(categoryCode),
						tagCodeEq(tagCode),
						sidoCodeEq(sidoCode),
						sigunguCodeEq(sigunguCode),
						createdByEq(createdBy),
						afterRoomPlaceCursor(cursorCreatedAt, cursorRoomPlaceId)
				)
				.orderBy(ROOM_PLACE.createdAt.desc(), ROOM_PLACE.id.desc())
				.limit(limit)
				.fetch();
	}

	@Override
	public long countRoomPlaces(
			Long roomId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Long createdBy
	) {
		Long total = queryFactory
				.select(ROOM_PLACE.id.countDistinct())
				.from(ROOM_PLACE)
				.join(ROOM_PLACE.place, PLACE)
				.join(PLACE.serviceCategory, CATEGORY)
				.join(PLACE.serviceTag, TAG)
				.where(
						roomIdEq(roomId),
						keywordContains(keyword),
						categoryCodeEq(categoryCode),
						tagCodeEq(tagCode),
						sidoCodeEq(sidoCode),
						sigunguCodeEq(sigunguCode),
						createdByEq(createdBy)
				)
				.fetchOne();
		return total == null ? 0 : total;
	}

	@Override
	public List<RoomPlace> findMapPlacesInBounds(
			Long roomId,
			BigDecimal minLatitude,
			BigDecimal maxLatitude,
			BigDecimal minLongitude,
			BigDecimal maxLongitude,
			int limit
	) {
		return baseRoomPlaceQuery()
				.where(
						roomIdEq(roomId),
						PLACE.latitude.isNotNull(),
						PLACE.longitude.isNotNull(),
						PLACE.latitude.between(minLatitude, maxLatitude),
						PLACE.longitude.between(minLongitude, maxLongitude)
				)
				.orderBy(ROOM_PLACE.createdAt.desc(), ROOM_PLACE.id.desc())
				.limit(limit)
				.fetch();
	}

	@Override
	public Page<RoomPlace> searchMyRoomPlaces(
			Long userId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Pageable pageable
	) {
		List<RoomPlace> content = baseMyRoomPlaceQuery()
				.where(
						createdByEq(userId),
						memberExists(userId),
						keywordContains(keyword),
						categoryCodeEq(categoryCode),
						tagCodeEq(tagCode),
						sidoCodeEq(sidoCode),
						sigunguCodeEq(sigunguCode)
				)
				.orderBy(ROOM_PLACE.createdAt.desc(), ROOM_PLACE.id.desc())
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		Long total = queryFactory
				.select(ROOM_PLACE.id.countDistinct())
				.from(ROOM_PLACE)
				.join(ROOM_PLACE.place, PLACE)
				.join(PLACE.serviceCategory, CATEGORY)
				.join(PLACE.serviceTag, TAG)
				.where(
						createdByEq(userId),
						memberExists(userId),
						keywordContains(keyword),
						categoryCodeEq(categoryCode),
						tagCodeEq(tagCode),
						sidoCodeEq(sidoCode),
						sigunguCodeEq(sigunguCode)
				)
				.fetchOne();
		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	@Override
	public long countMyRoomPlaces(
			Long userId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode
	) {
		Long total = queryFactory
				.select(ROOM_PLACE.id.countDistinct())
				.from(ROOM_PLACE)
				.join(ROOM_PLACE.place, PLACE)
				.join(PLACE.serviceCategory, CATEGORY)
				.join(PLACE.serviceTag, TAG)
				.where(
						createdByEq(userId),
						memberExists(userId),
						keywordContains(keyword),
						categoryCodeEq(categoryCode),
						tagCodeEq(tagCode),
						sidoCodeEq(sidoCode),
						sigunguCodeEq(sigunguCode)
				)
				.fetchOne();
		return total == null ? 0 : total;
	}

	@Override
	public List<RoomPlace> searchMyRoomPlacesAfterCursor(
			Long userId,
			String keyword,
			String categoryCode,
			String tagCode,
			String sidoCode,
			String sigunguCode,
			Instant cursorCreatedAt,
			Long cursorRoomPlaceId,
			int limit
	) {
		return baseMyRoomPlaceQuery()
				.where(
						createdByEq(userId),
						memberExists(userId),
						keywordContains(keyword),
						categoryCodeEq(categoryCode),
						tagCodeEq(tagCode),
						sidoCodeEq(sidoCode),
						sigunguCodeEq(sigunguCode),
						afterRoomPlaceCursor(cursorCreatedAt, cursorRoomPlaceId)
				)
				.orderBy(ROOM_PLACE.createdAt.desc(), ROOM_PLACE.id.desc())
				.limit(limit)
				.fetch();
	}

	@Override
	public List<RoomPlace> findExistingByRoomIdAndKakaoPlaceIds(Long roomId, Collection<String> kakaoPlaceIds) {
		if (kakaoPlaceIds == null || kakaoPlaceIds.isEmpty()) {
			return List.of();
		}
		return baseRoomPlaceQuery()
				.where(
						roomIdEq(roomId),
						PLACE.kakaoPlaceId.in(kakaoPlaceIds)
				)
				.fetch();
	}

	@Override
	public List<RoomPlace> findExistingByRoomIdAndSourceExternalPlaceIds(
			Long roomId,
			PlaceSource source,
			Collection<String> externalPlaceIds
	) {
		if (externalPlaceIds == null || externalPlaceIds.isEmpty()) {
			return List.of();
		}
		return baseRoomPlaceQuery()
				.where(
						roomIdEq(roomId),
						PLACE.source.eq(source),
						PLACE.externalPlaceId.in(externalPlaceIds)
				)
				.fetch();
	}

	private JPAQuery<RoomPlace> baseRoomPlaceQuery() {
		return queryFactory
				.selectFrom(ROOM_PLACE)
				.distinct()
				.join(ROOM_PLACE.place, PLACE).fetchJoin()
				.join(PLACE.serviceCategory, CATEGORY).fetchJoin()
				.join(PLACE.serviceTag, TAG).fetchJoin()
				.leftJoin(ROOM_PLACE.originRoomLink, ORIGIN_ROOM_LINK).fetchJoin()
				.leftJoin(ORIGIN_ROOM_LINK.link, ORIGIN_LINK).fetchJoin();
	}

	private JPAQuery<RoomPlace> baseMyRoomPlaceQuery() {
		return queryFactory
				.selectFrom(ROOM_PLACE)
				.distinct()
				.join(ROOM_PLACE.room, ROOM).fetchJoin()
				.join(ROOM_PLACE.place, PLACE).fetchJoin()
				.join(PLACE.serviceCategory, CATEGORY).fetchJoin()
				.join(PLACE.serviceTag, TAG).fetchJoin()
				.leftJoin(ROOM_PLACE.originRoomLink, ORIGIN_ROOM_LINK).fetchJoin()
				.leftJoin(ORIGIN_ROOM_LINK.link, ORIGIN_LINK).fetchJoin();
	}

	private static BooleanExpression roomIdEq(Long roomId) {
		return ROOM_PLACE.room.id.eq(roomId);
	}

	private static BooleanExpression categoryCodeEq(String categoryCode) {
		return StringUtils.hasText(categoryCode) ? CATEGORY.code.eq(categoryCode) : null;
	}

	private static BooleanExpression tagCodeEq(String tagCode) {
		return StringUtils.hasText(tagCode) ? TAG.code.eq(tagCode) : null;
	}

	private static BooleanExpression sidoCodeEq(String sidoCode) {
		return StringUtils.hasText(sidoCode) ? ROOM_PLACE.sidoCode.eq(sidoCode) : null;
	}

	private static BooleanExpression sigunguCodeEq(String sigunguCode) {
		return StringUtils.hasText(sigunguCode) ? ROOM_PLACE.sigunguCode.eq(sigunguCode) : null;
	}

	private static BooleanExpression createdByEq(Long createdBy) {
		return createdBy == null ? null : ROOM_PLACE.createdByUserId.eq(createdBy);
	}

	private static BooleanExpression afterRoomPlaceCursor(Instant createdAt, Long roomPlaceId) {
		if (createdAt == null || roomPlaceId == null) {
			return null;
		}
		return ROOM_PLACE.createdAt.lt(createdAt)
				.or(ROOM_PLACE.createdAt.eq(createdAt).and(ROOM_PLACE.id.lt(roomPlaceId)));
	}

	private static BooleanExpression memberExists(Long userId) {
		if (userId == null) {
			return null;
		}
		return JPAExpressions
				.selectOne()
				.from(ROOM_MEMBER)
				.where(
						ROOM_MEMBER.room.eq(ROOM_PLACE.room),
						ROOM_MEMBER.userId.eq(userId)
				)
				.exists();
	}

	private static BooleanExpression keywordContains(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return null;
		}
		return containsIgnoreCase(PLACE.name, keyword)
				.or(containsIgnoreCase(PLACE.address, keyword))
				.or(containsIgnoreCase(PLACE.roadAddress, keyword))
				.or(containsIgnoreCase(PLACE.categoryName, keyword))
				.or(containsIgnoreCase(CATEGORY.name, keyword))
				.or(containsIgnoreCase(CATEGORY.code, keyword))
				.or(containsIgnoreCase(TAG.name, keyword))
				.or(containsIgnoreCase(TAG.code, keyword));
	}

	private static BooleanExpression containsIgnoreCase(StringPath path, String keyword) {
		return path.isNotNull().and(path.containsIgnoreCase(keyword));
	}
}
