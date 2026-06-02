package com.hufs.capstone.backend.course.domain.entity;

import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.room.domain.entity.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "date_courses",
		indexes = {
			@Index(name = "idx_date_courses_room_id_created_at", columnList = "room_id, created_at"),
			@Index(name = "idx_date_courses_created_by_user_id_created_at", columnList = "created_by_user_id, created_at"),
			@Index(name = "idx_date_courses_generation_batch_id", columnList = "generation_batch_id"),
			@Index(name = "idx_date_courses_saved_by_user_id_saved_at", columnList = "saved_by_user_id, saved_at")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_date_courses_public_id", columnNames = "public_id")
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DateCourse extends AuditableEntity {

	@Column(name = "public_id", nullable = false, length = 36)
	private String publicId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(name = "created_by_user_id", nullable = false)
	private Long createdByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "course_mode", nullable = false, length = 20)
	private CourseMode courseMode;

	@Column(name = "planned_date_time", nullable = false)
	private Instant plannedDateTime;

	@Column(name = "generation_batch_id", nullable = false, length = 36)
	private String generationBatchId;

	@Column(name = "sigungu_code", nullable = false, length = 5)
	private String sigunguCode;

	@Column(name = "category_sequence_json", columnDefinition = "text")
	private String categorySequenceJson;

	@Column(name = "skipped_slot_indices_json", columnDefinition = "text")
	private String skippedSlotIndicesJson;

	@Column(name = "saved_by_user_id")
	private Long savedByUserId;

	@Column(name = "saved_at")
	private Instant savedAt;

	private DateCourse(
			String publicId,
			Room room,
			Long createdByUserId,
			CourseMode courseMode,
			Instant plannedDateTime,
			String generationBatchId,
			String sigunguCode,
			String categorySequenceJson,
			String skippedSlotIndicesJson
	) {
		this.publicId = publicId;
		this.room = room;
		this.createdByUserId = createdByUserId;
		this.courseMode = courseMode;
		this.plannedDateTime = plannedDateTime;
		this.generationBatchId = generationBatchId;
		this.sigunguCode = sigunguCode;
		this.categorySequenceJson = categorySequenceJson;
		this.skippedSlotIndicesJson = skippedSlotIndicesJson;
	}

	public static DateCourse create(
			String publicId,
			Room room,
			Long createdByUserId,
			CourseMode courseMode,
			Instant plannedDateTime,
			String generationBatchId,
			String sigunguCode,
			String categorySequenceJson,
			String skippedSlotIndicesJson
	) {
		if (publicId == null || room == null || createdByUserId == null || courseMode == null
				|| plannedDateTime == null || generationBatchId == null
				|| sigunguCode == null || sigunguCode.isBlank()) {
			throw new IllegalArgumentException("DateCourse required values are missing.");
		}
		return new DateCourse(publicId, room, createdByUserId, courseMode, plannedDateTime,
				generationBatchId, sigunguCode, categorySequenceJson, skippedSlotIndicesJson);
	}

	public void markAsSaved(Long userId) {
		if (this.savedByUserId != null) {
			throw new IllegalStateException("이미 저장된 코스입니다.");
		}
		this.savedByUserId = userId;
		this.savedAt = Instant.now();
	}
}
