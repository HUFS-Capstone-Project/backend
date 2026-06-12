package com.hufs.capstone.backend.course.domain.repository;

import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DateCourseRepository extends JpaRepository<DateCourse, Long> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE DateCourse dc
			SET dc.savedByUserId = :userId,
			    dc.savedAt = :savedAt,
			    dc.courseName = :courseName
			WHERE dc.id = :id AND dc.savedByUserId IS NULL
			""")
	int markAsSavedIfAbsent(
			@Param("id") Long id,
			@Param("userId") Long userId,
			@Param("savedAt") Instant savedAt,
			@Param("courseName") String courseName
	);

	Optional<DateCourse> findByDateCourseIdAndRoomIdAndDeletedAtIsNull(String dateCourseId, Long roomId);

	@Query(value = """
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			AND dc.deletedAt IS NULL
			ORDER BY dc.savedAt DESC
			""",
			countQuery = """
			SELECT COUNT(dc) FROM DateCourse dc
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			AND dc.deletedAt IS NULL
			""")
	Page<DateCourse> findSavedByRoomIdOrderBySavedAtDesc(@Param("roomId") Long roomId, Pageable pageable);

	@Query("""
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			AND dc.deletedAt IS NULL
			ORDER BY dc.savedAt DESC, dc.id DESC
			""")
	List<DateCourse> findSavedByRoomIdFirstPage(@Param("roomId") Long roomId, Pageable pageable);

	@Query("""
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			AND dc.deletedAt IS NULL
			AND (
				:cursorSavedAt IS NULL
				OR dc.savedAt < :cursorSavedAt
				OR (dc.savedAt = :cursorSavedAt AND dc.id < :cursorDateCoursePk)
			)
			ORDER BY dc.savedAt DESC, dc.id DESC
			""")
	List<DateCourse> findSavedByRoomIdAfterCursor(
			@Param("roomId") Long roomId,
			@Param("cursorSavedAt") Instant cursorSavedAt,
			@Param("cursorDateCoursePk") Long cursorDateCoursePk,
			Pageable pageable
	);

	@Query("""
			SELECT COUNT(dc) FROM DateCourse dc
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			AND dc.deletedAt IS NULL
			""")
	long countSavedByRoomId(@Param("roomId") Long roomId);

	@Query(value = """
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.savedByUserId = :userId
			AND dc.deletedAt IS NULL
			ORDER BY dc.savedAt DESC
			""",
			countQuery = """
			SELECT COUNT(dc) FROM DateCourse dc
			WHERE dc.savedByUserId = :userId
			AND dc.deletedAt IS NULL
			""")
	Page<DateCourse> findSavedByUserIdOrderBySavedAtDesc(@Param("userId") Long userId, Pageable pageable);

	@Query("""
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.savedByUserId = :userId
			AND dc.deletedAt IS NULL
			ORDER BY dc.savedAt DESC, dc.id DESC
			""")
	List<DateCourse> findSavedByUserIdFirstPage(@Param("userId") Long userId, Pageable pageable);

	@Query("""
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.savedByUserId = :userId
			AND dc.deletedAt IS NULL
			AND (
				:cursorSavedAt IS NULL
				OR dc.savedAt < :cursorSavedAt
				OR (dc.savedAt = :cursorSavedAt AND dc.id < :cursorDateCoursePk)
			)
			ORDER BY dc.savedAt DESC, dc.id DESC
			""")
	List<DateCourse> findSavedByUserIdAfterCursor(
			@Param("userId") Long userId,
			@Param("cursorSavedAt") Instant cursorSavedAt,
			@Param("cursorDateCoursePk") Long cursorDateCoursePk,
			Pageable pageable
	);

	@Query("""
			SELECT COUNT(dc) FROM DateCourse dc
			WHERE dc.savedByUserId = :userId
			AND dc.deletedAt IS NULL
			""")
	long countSavedByUserId(@Param("userId") Long userId);
}
