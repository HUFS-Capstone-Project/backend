package com.hufs.capstone.backend.course.infrastructure.persistence;

import com.hufs.capstone.backend.course.application.dto.CategorySlotCommand;
import com.hufs.capstone.backend.course.application.dto.DateCourseCandidate;
import com.hufs.capstone.backend.course.application.port.DateCourseCandidateQueryPort;
import com.hufs.capstone.backend.link.domain.entity.QLink;
import com.hufs.capstone.backend.link.domain.entity.QRoomLink;
import com.hufs.capstone.backend.place.domain.entity.QPlace;
import com.hufs.capstone.backend.place.domain.entity.QPlaceBusinessHours;
import com.hufs.capstone.backend.place.domain.entity.QPlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.QPlaceTag;
import com.hufs.capstone.backend.place.domain.entity.QRoomPlace;
import com.hufs.capstone.backend.place.domain.enums.BusinessHoursStatus;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class QuerydslDateCourseCandidateAdapter implements DateCourseCandidateQueryPort {

	private static final QRoomPlace ROOM_PLACE = QRoomPlace.roomPlace;
	private static final QPlace PLACE = QPlace.place;
	private static final QPlaceCategory CATEGORY = QPlaceCategory.placeCategory;
	private static final QPlaceTag TAG = QPlaceTag.placeTag;
	private static final QRoomLink ORIGIN_ROOM_LINK = QRoomLink.roomLink;
	private static final QLink ORIGIN_LINK = QLink.link;
	private static final QPlaceBusinessHours PBH = QPlaceBusinessHours.placeBusinessHours;

	private final JPAQueryFactory queryFactory;

	public QuerydslDateCourseCandidateAdapter(EntityManager entityManager) {
		this.queryFactory = new JPAQueryFactory(entityManager);
	}

	@Override
	public List<DateCourseCandidate> findCandidates(
			Long roomId,
			List<CategorySlotCommand> slots,
			Instant now,
			String sigunguCode
	) {
		BooleanExpression categoryFilter = buildCategoryFilter(slots);
		if (categoryFilter == null) {
			return List.of();
		}
		BooleanExpression hasOriginLink = ROOM_PLACE.originRoomLink.isNotNull();
		List<Tuple> rows = queryFactory
				.select(
						ROOM_PLACE,
						CATEGORY.code,
						TAG.code,
						PLACE.latitude,
						PLACE.longitude,
						ROOM_PLACE.createdAt,
						ORIGIN_LINK.linkSourceType,
						ORIGIN_LINK.likeCount,
						hasOriginLink,
						PBH.businessHoursJson
				)
				.from(ROOM_PLACE)
				.join(ROOM_PLACE.place, PLACE)
				.join(PLACE.serviceCategory, CATEGORY)
				.join(PLACE.serviceTag, TAG)
				.leftJoin(ROOM_PLACE.originRoomLink, ORIGIN_ROOM_LINK)
				.leftJoin(ORIGIN_ROOM_LINK.link, ORIGIN_LINK)
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

		return rows.stream()
				.map(row -> new DateCourseCandidate(
						row.get(ROOM_PLACE),
						row.get(CATEGORY.code),
						row.get(TAG.code),
						row.get(PLACE.latitude),
						row.get(PLACE.longitude),
						row.get(ROOM_PLACE.createdAt),
						row.get(ORIGIN_LINK.linkSourceType),
						row.get(ORIGIN_LINK.likeCount),
						Boolean.TRUE.equals(row.get(hasOriginLink)),
						row.get(PBH.businessHoursJson)
				))
				.toList();
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
