package com.hufs.capstone.backend.course.domain.repository;

import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import java.time.Instant;
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
			SET dc.savedByUserId = :userId, dc.savedAt = :savedAt
			WHERE dc.id = :id AND dc.savedByUserId IS NULL
			""")
	int markAsSavedIfAbsent(@Param("id") Long id,
	                        @Param("userId") Long userId,
	                        @Param("savedAt") Instant savedAt);

	Optional<DateCourse> findByDateCourseIdAndRoomId(String dateCourseId, Long roomId);

	@Query(value = """
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			ORDER BY dc.savedAt DESC
			""",
			countQuery = """
			SELECT COUNT(dc) FROM DateCourse dc
			WHERE dc.room.id = :roomId
			AND dc.savedByUserId IS NOT NULL
			""")
	Page<DateCourse> findSavedByRoomIdOrderBySavedAtDesc(@Param("roomId") Long roomId, Pageable pageable);

	@Query(value = """
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.savedByUserId = :userId
			ORDER BY dc.savedAt DESC
			""",
			countQuery = """
			SELECT COUNT(dc) FROM DateCourse dc
			WHERE dc.savedByUserId = :userId
			""")
	Page<DateCourse> findSavedByUserIdOrderBySavedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
