package com.hufs.capstone.backend.link.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.link.domain.vo.PlaceCandidateSnapshot;
import com.hufs.capstone.backend.room.domain.entity.Room;
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
		name = "room_places",
		indexes = {
			@Index(name = "idx_room_places_room_id", columnList = "room_id"),
			@Index(name = "idx_room_places_room_link_id", columnList = "room_link_id"),
			@Index(name = "idx_room_places_room_id_kakao_place_id", columnList = "room_id, kakao_place_id")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_room_places_room_id_kakao_place_id", columnNames = {"room_id", "kakao_place_id"})
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomPlace extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_link_id", nullable = false)
	private RoomLink roomLink;

	@Column(nullable = false)
	private Long savedBy;

	@Column(name = "kakao_place_id", nullable = false, length = 100)
	private String kakaoPlaceId;

	@Column(length = 255)
	private String placeName;

	@Column(length = 500)
	private String categoryName;

	@Column(length = 50)
	private String categoryGroupCode;

	@Column(length = 100)
	private String categoryGroupName;

	@Column(length = 500)
	private String addressName;

	@Column(length = 500)
	private String roadAddressName;

	@Column(precision = 18, scale = 15)
	private BigDecimal longitude;

	@Column(precision = 18, scale = 15)
	private BigDecimal latitude;

	@Column(length = 100)
	private String phone;

	@Column(length = 2048)
	private String placeUrl;

	@Column(precision = 5, scale = 4)
	private BigDecimal confidence;

	@Column(length = 255)
	private String sourceKeyword;

	@Column(length = 500)
	private String sourceSentence;

	@Column(length = 255)
	private String rawCandidate;

	private RoomPlace(RoomLink roomLink, Long savedBy, PlaceCandidateSnapshot candidate) {
		this.room = roomLink.getRoom();
		this.roomLink = roomLink;
		this.savedBy = savedBy;
		this.kakaoPlaceId = candidate.kakaoPlaceId();
		this.placeName = candidate.placeName();
		this.categoryName = candidate.categoryName();
		this.categoryGroupCode = candidate.categoryGroupCode();
		this.categoryGroupName = candidate.categoryGroupName();
		this.addressName = candidate.addressName();
		this.roadAddressName = candidate.roadAddressName();
		this.longitude = candidate.longitude();
		this.latitude = candidate.latitude();
		this.phone = candidate.phone();
		this.placeUrl = candidate.placeUrl();
		this.confidence = candidate.confidence();
		this.sourceKeyword = candidate.sourceKeyword();
		this.sourceSentence = candidate.sourceSentence();
		this.rawCandidate = candidate.rawCandidate();
	}

	public static RoomPlace create(RoomLink roomLink, Long savedBy, PlaceCandidateSnapshot candidate) {
		if (candidate.kakaoPlaceId() == null) {
			throw new IllegalArgumentException("kakaoPlaceId is required to save a room place.");
		}
		return new RoomPlace(roomLink, savedBy, candidate);
	}
}
