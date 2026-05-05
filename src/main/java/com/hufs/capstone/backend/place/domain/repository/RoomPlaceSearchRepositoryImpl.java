package com.hufs.capstone.backend.place.domain.repository;

import com.hufs.capstone.backend.place.domain.entity.Place;
import com.hufs.capstone.backend.place.domain.entity.PlaceCategory;
import com.hufs.capstone.backend.place.domain.entity.PlaceTag;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class RoomPlaceSearchRepositoryImpl implements RoomPlaceSearchRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Page<RoomPlace> searchRoomPlaces(
			Long roomId,
			String keyword,
			String initialKeyword,
			String categoryCode,
			String tagCode,
			Pageable pageable
	) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<RoomPlace> query = cb.createQuery(RoomPlace.class);
		Root<RoomPlace> roomPlace = query.from(RoomPlace.class);
		Fetch<RoomPlace, Place> placeFetch = roomPlace.fetch("place", JoinType.INNER);
		placeFetch.fetch("serviceCategory", JoinType.INNER);
		placeFetch.fetch("serviceTag", JoinType.INNER);
		Join<RoomPlace, Place> place = roomPlace.join("place", JoinType.INNER);
		Join<Place, PlaceCategory> category = place.join("serviceCategory", JoinType.INNER);
		Join<Place, PlaceTag> tag = place.join("serviceTag", JoinType.INNER);

		query.select(roomPlace)
				.distinct(true)
				.where(predicates(cb, roomPlace, place, category, tag, roomId, keyword, initialKeyword, categoryCode, tagCode))
				.orderBy(cb.desc(roomPlace.get("createdAt")), cb.desc(roomPlace.get("id")));

		TypedQuery<RoomPlace> typedQuery = entityManager.createQuery(query);
		typedQuery.setFirstResult((int) pageable.getOffset());
		typedQuery.setMaxResults(pageable.getPageSize());
		List<RoomPlace> content = typedQuery.getResultList();

		long total = count(cb, roomId, keyword, initialKeyword, categoryCode, tagCode);
		return new PageImpl<>(content, pageable, total);
	}

	private long count(
			CriteriaBuilder cb,
			Long roomId,
			String keyword,
			String initialKeyword,
			String categoryCode,
			String tagCode
	) {
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<RoomPlace> roomPlace = query.from(RoomPlace.class);
		Join<RoomPlace, Place> place = roomPlace.join("place", JoinType.INNER);
		Join<Place, PlaceCategory> category = place.join("serviceCategory", JoinType.INNER);
		Join<Place, PlaceTag> tag = place.join("serviceTag", JoinType.INNER);

		query.select(cb.countDistinct(roomPlace))
				.where(predicates(cb, roomPlace, place, category, tag, roomId, keyword, initialKeyword, categoryCode, tagCode));
		return entityManager.createQuery(query).getSingleResult();
	}

	private Predicate[] predicates(
			CriteriaBuilder cb,
			Root<RoomPlace> roomPlace,
			Join<RoomPlace, Place> place,
			Join<Place, PlaceCategory> category,
			Join<Place, PlaceTag> tag,
			Long roomId,
			String keyword,
			String initialKeyword,
			String categoryCode,
			String tagCode
	) {
		List<Predicate> predicates = new ArrayList<>();
		predicates.add(cb.equal(roomPlace.get("room").get("id"), roomId));
		if (keyword != null) {
			predicates.add(keywordPredicate(cb, roomPlace, place, keyword, initialKeyword));
		}
		if (categoryCode != null) {
			predicates.add(cb.equal(category.get("code"), categoryCode));
		}
		if (tagCode != null) {
			predicates.add(cb.equal(tag.get("code"), tagCode));
		}
		return predicates.toArray(Predicate[]::new);
	}

	private Predicate keywordPredicate(
			CriteriaBuilder cb,
			Root<RoomPlace> roomPlace,
			Join<RoomPlace, Place> place,
			String keyword,
			String initialKeyword
	) {
		String likeKeyword = "%" + keyword + "%";
		List<Predicate> predicates = new ArrayList<>();
		predicates.add(cb.like(lowerCoalesce(cb, place.get("searchText")), likeKeyword));
		predicates.add(cb.like(lowerCoalesce(cb, place.get("name")), likeKeyword));
		predicates.add(cb.like(lowerCoalesce(cb, place.get("address")), likeKeyword));
		predicates.add(cb.like(lowerCoalesce(cb, place.get("roadAddress")), likeKeyword));
		predicates.add(cb.like(lowerCoalesce(cb, place.get("categoryName")), likeKeyword));
		predicates.add(cb.like(lowerCoalesce(cb, roomPlace.get("memo")), likeKeyword));
		if (initialKeyword != null) {
			predicates.add(cb.like(
					cb.coalesce(place.get("initialConsonants"), ""),
					"%" + initialKeyword + "%"
			));
		}
		return cb.or(predicates.toArray(Predicate[]::new));
	}

	private Expression<String> lowerCoalesce(
			CriteriaBuilder cb,
			Expression<String> expression
	) {
		return cb.lower(cb.coalesce(expression, ""));
	}
}
