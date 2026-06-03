package com.hufs.capstone.backend.course.domain.repository;

import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DateCoursePlaceRepository extends JpaRepository<DateCoursePlace, Long> {

	@Query("""
			SELECT dcp FROM DateCoursePlace dcp
			JOIN FETCH dcp.roomPlace rp
			JOIN FETCH rp.place p
			JOIN FETCH p.serviceCategory
			JOIN FETCH p.serviceTag
			LEFT JOIN FETCH rp.originRoomLink orl
			LEFT JOIN FETCH orl.link
			WHERE dcp.dateCourse.id IN :courseIds
			ORDER BY dcp.sequenceOrder ASC
			""")
	List<DateCoursePlace> findWithRoomPlacesByCourseIdIn(@Param("courseIds") List<Long> courseIds);

	@Query("""
			SELECT dcp FROM DateCoursePlace dcp
			JOIN FETCH dcp.dateCourse dc
			JOIN FETCH dcp.roomPlace rp
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			ORDER BY dc.id ASC, dcp.sequenceOrder ASC
			""")
	List<DateCoursePlace> findSavedPlacesByRoomId(@Param("roomId") Long roomId);

	@Query("""
			SELECT dcp FROM DateCoursePlace dcp
			JOIN FETCH dcp.dateCourse dc
			JOIN FETCH dcp.roomPlace rp
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			AND dc.id <> :excludedCourseId
			ORDER BY dc.id ASC, dcp.sequenceOrder ASC
			""")
	List<DateCoursePlace> findSavedPlacesByRoomIdExcludingCourseId(
			@Param("roomId") Long roomId,
			@Param("excludedCourseId") Long excludedCourseId
	);
}
