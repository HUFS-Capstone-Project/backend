package com.hufs.capstone.backend.course.application;

import static org.mockito.Mockito.inOrder;

import com.hufs.capstone.backend.course.domain.repository.DateCoursePlaceRepository;
import com.hufs.capstone.backend.course.domain.repository.DateCourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateCourseRoomCleanupAdapterTest {

	@Mock
	private DateCoursePlaceRepository dateCoursePlaceRepository;

	@Mock
	private DateCourseRepository dateCourseRepository;

	@InjectMocks
	private DateCourseRoomCleanupAdapter cleanupAdapter;

	@Test
	void deleteAllByRoomIdShouldDeleteCoursePlacesBeforeCourses() {
		cleanupAdapter.deleteAllByRoomId(1L);

		InOrder inOrder = inOrder(dateCoursePlaceRepository, dateCourseRepository);
		inOrder.verify(dateCoursePlaceRepository).deleteByRoomId(1L);
		inOrder.verify(dateCourseRepository).deleteByRoomId(1L);
	}
}
