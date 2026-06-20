package com.hufs.capstone.backend.course.domain.repository;

import com.hufs.capstone.backend.course.domain.entity.DateCoursePlace;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DateCoursePlaceRepository extends JpaRepository<DateCoursePlace, Long> {

	@Query("""
			SELECT COUNT(dcp) > 0 FROM DateCoursePlace dcp
			JOIN dcp.dateCourse dc
			WHERE dcp.roomPlace.id = :roomPlaceId
			AND dc.savedByUserId IS NOT NULL
			AND dc.deletedAt IS NULL
			""")
	boolean existsByRoomPlaceIdInSavedDateCourse(@Param("roomPlaceId") Long roomPlaceId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM DateCoursePlace dcp WHERE dcp.roomPlace.id = :roomPlaceId")
	int deleteByRoomPlaceId(@Param("roomPlaceId") Long roomPlaceId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM DateCoursePlace dcp WHERE dcp.dateCourse.room.id = :roomId")
	int deleteByRoomId(@Param("roomId") Long roomId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT dcp FROM DateCoursePlace dcp
			JOIN FETCH dcp.roomPlace rp
			WHERE dcp.dateCourse.id = :courseId
			ORDER BY dcp.sequenceOrder ASC
			""")
	List<DateCoursePlace> findWithRoomPlacesByCourseIdForUpdate(@Param("courseId") Long courseId);

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
			AND dc.deletedAt IS NULL
			ORDER BY dc.id ASC, dcp.sequenceOrder ASC
			""")
	List<DateCoursePlace> findSavedPlacesByRoomId(@Param("roomId") Long roomId);

	@Query("""
			SELECT dcp FROM DateCoursePlace dcp
			JOIN FETCH dcp.dateCourse dc
			JOIN FETCH dcp.roomPlace rp
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			AND dc.deletedAt IS NULL
			AND dc.id <> :excludedCourseId
			ORDER BY dc.id ASC, dcp.sequenceOrder ASC
			""")
	List<DateCoursePlace> findSavedPlacesByRoomIdExcludingCourseId(
			@Param("roomId") Long roomId,
			@Param("excludedCourseId") Long excludedCourseId
	);

	/**
	 * 코스의 장소를 전체 교체할 때 기존 장소를 모두 삭제한다.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM DateCoursePlace dcp WHERE dcp.dateCourse.id = :courseId")
	int deleteByDateCourseId(@Param("courseId") Long courseId);
}
