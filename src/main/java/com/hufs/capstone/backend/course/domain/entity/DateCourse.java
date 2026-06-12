package com.hufs.capstone.backend.course.domain.entity;

import com.hufs.capstone.backend.course.domain.enums.CourseMode;
import com.hufs.capstone.backend.global.common.entity.SoftDeletableEntity;
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
			@Index(name = "idx_date_courses_saved_by_user_id_saved_at", columnList = "saved_by_user_id, saved_at"),
			@Index(name = "idx_date_courses_saved_by_user_saved_id", columnList = "saved_by_user_id, saved_at, id"),
			@Index(name = "idx_date_courses_room_saved_id", columnList = "room_id, saved_at, id")
		},
		uniqueConstraints = {
			@UniqueConstraint(name = "uq_date_courses_date_course_id", columnNames = "date_course_id")
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DateCourse extends SoftDeletableEntity {

	@Column(name = "date_course_id", nullable = false, length = 36)
	private String dateCourseId;

	@Column(name = "course_name", length = 20)
	private String courseName;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(name = "created_by_user_id", nullable = false)
	private Long createdByUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "course_mode", nullable = false, length = 20)
	private CourseMode courseMode;

	@Column(name = "start_date_time", nullable = false)
	private Instant startDateTime;

	@Column(name = "end_date_time", nullable = false)
	private Instant endDateTime;

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
			String dateCourseId,
			Room room,
			Long createdByUserId,
			CourseMode courseMode,
			Instant startDateTime,
			Instant endDateTime,
			String generationBatchId,
			String sigunguCode,
			String categorySequenceJson,
			String skippedSlotIndicesJson
	) {
		this.dateCourseId = dateCourseId;
		this.room = room;
		this.createdByUserId = createdByUserId;
		this.courseMode = courseMode;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.generationBatchId = generationBatchId;
		this.sigunguCode = sigunguCode;
		this.categorySequenceJson = categorySequenceJson;
		this.skippedSlotIndicesJson = skippedSlotIndicesJson;
	}

	public static DateCourse create(
			String dateCourseId,
			Room room,
			Long createdByUserId,
			CourseMode courseMode,
			Instant startDateTime,
			Instant endDateTime,
			String generationBatchId,
			String sigunguCode,
			String categorySequenceJson,
			String skippedSlotIndicesJson
	) {
		if (dateCourseId == null || room == null || createdByUserId == null || courseMode == null
				|| startDateTime == null || endDateTime == null || generationBatchId == null
				|| sigunguCode == null || sigunguCode.isBlank()) {
			throw new IllegalArgumentException("DateCourse required values are missing.");
		}
		if (!startDateTime.isBefore(endDateTime)) {
			throw new IllegalArgumentException("DateCourse startDateTime must be before endDateTime.");
		}
		return new DateCourse(dateCourseId, room, createdByUserId, courseMode, startDateTime, endDateTime,
				generationBatchId, sigunguCode, categorySequenceJson, skippedSlotIndicesJson);
	}

	/**
	 * 코스 이름을 변경한다. (정규화/검증은 호출 전에 완료되어야 한다)
	 */
	public void rename(String courseName) {
		this.courseName = courseName;
	}

	/**
	 * 코스를 수동으로 편집한 이후에는 생성 시점의 "건너뛴 슬롯" 정보가
	 * 현재 장소 구성과 일치하지 않으므로 비운다.
	 */
	public void clearSkippedSlots() {
		this.skippedSlotIndicesJson = "[]";
	}
}
