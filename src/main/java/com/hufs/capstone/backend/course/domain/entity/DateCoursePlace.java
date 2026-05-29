package com.hufs.capstone.backend.course.domain.entity;

import com.hufs.capstone.backend.global.common.entity.AuditableEntity;
import com.hufs.capstone.backend.place.domain.entity.RoomPlace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "date_course_places",
		indexes = {
			@Index(name = "idx_date_course_places_date_course_id", columnList = "date_course_id")
		},
		uniqueConstraints = {
			@UniqueConstraint(
					name = "uq_date_course_places_course_order",
					columnNames = {"date_course_id", "sequence_order"}
				)
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DateCoursePlace extends AuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "date_course_id", nullable = false)
	private DateCourse dateCourse;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_place_id", nullable = false)
	private RoomPlace roomPlace;

	@Column(name = "sequence_order", nullable = false)
	private Integer sequenceOrder;

	private DateCoursePlace(DateCourse dateCourse, RoomPlace roomPlace, Integer sequenceOrder) {
		this.dateCourse = dateCourse;
		this.roomPlace = roomPlace;
		this.sequenceOrder = sequenceOrder;
	}

	public static DateCoursePlace create(DateCourse dateCourse, RoomPlace roomPlace, Integer sequenceOrder) {
		if (dateCourse == null || roomPlace == null || sequenceOrder == null) {
			throw new IllegalArgumentException("DateCoursePlace required values are missing.");
		}
		return new DateCoursePlace(dateCourse, roomPlace, sequenceOrder);
	}
}
