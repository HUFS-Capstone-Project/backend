package com.hufs.capstone.backend.course.domain.repository;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.link.domain.entity.QLink;
import com.hufs.capstone.backend.link.domain.entity.QRoomLink;
import com.hufs.capstone.backend.place.domain.entity.QPlace;
import com.hufs.capstone.backend.place.domain.entity.QPlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.QPlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.QPlaceTag;
import com.hufs.capstone.backend.place.domain.entity.QRoomPlace;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DateCourseCandidateRepositoryImpl implements DateCourseCandidateRepository {

	private static final QRoomPlace ROOM_PLACE = QRoomPlace.roomPlace;
	private static final QPlace PLACE = QPlace.place;
	private static final QPlaceCategory CATEGORY = QPlaceCategory.placeCategory;
	private static final QPlaceTag TAG = QPlaceTag.placeTag;
	private static final QRoomLink ORIGIN_ROOM_LINK = QRoomLink.roomLink;
	private static final QLink ORIGIN_LINK = QLink.link;
	private static final QPlaceBusinessHours PBH = QPlaceBusinessHours.placeBusinessHours;

	private final JPAQueryFactory queryFactory;

	public DateCourseCandidateRepositoryImpl(EntityManager entityManager) {
		this.queryFactory = new JPAQueryFactory(entityManager);
	}

	@Override
	public List<RoomPlace> findCandidates(Long roomId, List<CategorySlotCommand> slots, Instant now, String sigunguCode) {
		BooleanExpression categoryFilter = buildCategoryFilter(slots);
		if (categoryFilter == null) {
			return List.of();
		}
		return queryFactory
				.selectFrom(ROOM_PLACE)
				.distinct()
				.join(ROOM_PLACE.place, PLACE).fetchJoin()
				.join(PLACE.serviceCategory, CATEGORY).fetchJoin()
				.join(PLACE.serviceTag, TAG).fetchJoin()
				.leftJoin(ROOM_PLACE.originRoomLink, ORIGIN_ROOM_LINK).fetchJoin()
				.leftJoin(ORIGIN_ROOM_LINK.link, ORIGIN_LINK).fetchJoin()
				.join(PBH).on(
						PBH.kakaoPlaceId.eq(PLACE.kakaoPlaceId),
						PBH.businessHoursStatus.eq(BusinessHoursStatus.SUCCEEDED),
						PBH.businessHoursExpiresAt.after(now)
				)
				.where(
						ROOM_PLACE.room.id.eq(roomId),
						ROOM_PLACE.sigunguCode.eq(sigunguCode),
						categoryFilter
				)
				.orderBy(ROOM_PLACE.createdAt.desc(), ROOM_PLACE.id.desc())
				.fetch();
	}

	private static BooleanExpression buildCategoryFilter(List<CategorySlotCommand> slots) {
		BooleanExpression combined = null;
		for (CategorySlotCommand slot : slots) {
			BooleanExpression slotExpr = CATEGORY.code.eq(slot.categoryCode());
			if (!slot.isWildcard()) {
				slotExpr = slotExpr.and(TAG.code.eq(slot.tagCode()));
			}
			combined = (combined == null) ? slotExpr : combined.or(slotExpr);
		}
		return combined;
	}
}
