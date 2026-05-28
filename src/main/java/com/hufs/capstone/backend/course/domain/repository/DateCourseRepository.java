package com.hufs.capstone.backend.course.domain.repository;

import com.hufs.capstone.backend.course.domain.entity.DateCourse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DateCourseRepository extends JpaRepository<DateCourse, Long> {

	Optional<DateCourse> findByPublicId(String publicId);

	Optional<DateCourse> findByPublicIdAndRoomId(String publicId, Long roomId);

	@Query("""
			SELECT dc FROM DateCourse dc
			JOIN FETCH dc.room
			WHERE dc.room.id = :roomId
			ORDER BY dc.createdAt DESC
			""")
	List<DateCourse> findByRoomIdOrderByCreatedAtDesc(@Param("roomId") Long roomId);

	List<DateCourse> findByGenerationBatchId(String generationBatchId);
}
