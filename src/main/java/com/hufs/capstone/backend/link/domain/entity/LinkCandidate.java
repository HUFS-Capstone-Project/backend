package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "link_candidates",
		indexes = @Index(name = "idx_link_candidates_link_id", columnList = "link_id"),
		uniqueConstraints = @UniqueConstraint(
				name = "uq_link_candidates_link_id_order",
				columnNames = {"link_id", "candidate_order"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkCandidate extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "link_id", nullable = false)
	private Link link;

	@Column(name = "candidate_order", nullable = false)
	private Integer candidateOrder;

	@Column(name = "kakao_place_id", length = 100)
	private String kakaoPlaceId;

	@Column(name = "place_name", length = 255)
	private String placeName;

	@Column(name = "category_name", length = 500)
	private String categoryName;

	@Column(name = "category_group_code", length = 50)
	private String categoryGroupCode;

	@Column(name = "category_group_name", length = 100)
	private String categoryGroupName;

	@Column(length = 100)
	private String phone;

	@Column(name = "address_name", length = 500)
	private String addressName;

	@Column(name = "road_address_name", length = 500)
	private String roadAddressName;

	@Column(precision = 18, scale = 15)
	private BigDecimal longitude;

	@Column(precision = 18, scale = 15)
	private BigDecimal latitude;

	@Column(name = "place_url", length = 2048)
	private String placeUrl;

	@Column(precision = 5, scale = 4)
	private BigDecimal confidence;

	@Column(length = 255)
	private String sourceKeyword;

	@Column(length = 500)
	private String sourceSentence;

	@Column(columnDefinition = "text")
	private String rawCandidate;

	private LinkCandidate(Link link, Integer candidateOrder, PlaceCandidateSnapshot snapshot) {
		this.link = link;
		this.candidateOrder = candidateOrder;
		apply(snapshot);
	}

	public static LinkCandidate create(Link link, Integer candidateOrder, PlaceCandidateSnapshot snapshot) {
		if (link == null || candidateOrder == null || snapshot == null) {
			throw new IllegalArgumentException("Link candidate required values are missing.");
		}
		return new LinkCandidate(link, candidateOrder, snapshot);
	}

	public PlaceCandidateSnapshot toSnapshot() {
		return new PlaceCandidateSnapshot(
				kakaoPlaceId,
				placeName,
				categoryName,
				categoryGroupCode,
				categoryGroupName,
				phone,
				addressName,
				roadAddressName,
				longitude,
				latitude,
				placeUrl,
				confidence,
				sourceKeyword,
				sourceSentence,
				rawCandidate
		);
	}

	private void apply(PlaceCandidateSnapshot snapshot) {
		this.kakaoPlaceId = trimToNull(snapshot.kakaoPlaceId());
		this.placeName = trimToNull(snapshot.placeName());
		this.categoryName = trimToNull(snapshot.categoryName());
		this.categoryGroupCode = trimToNull(snapshot.categoryGroupCode());
		this.categoryGroupName = trimToNull(snapshot.categoryGroupName());
		this.phone = trimToNull(snapshot.phone());
		this.addressName = trimToNull(snapshot.addressName());
		this.roadAddressName = trimToNull(snapshot.roadAddressName());
		this.longitude = snapshot.longitude();
		this.latitude = snapshot.latitude();
		this.placeUrl = trimToNull(snapshot.placeUrl());
		this.confidence = snapshot.confidence();
		this.sourceKeyword = trimToNull(snapshot.sourceKeyword());
		this.sourceSentence = trimToNull(snapshot.sourceSentence());
		this.rawCandidate = trimToNull(snapshot.rawCandidate());
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
