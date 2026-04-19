package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkAuthorizationServiceTest {

	@Mock
	private RoomLinkRepository roomLinkRepository;

	@InjectMocks
	private LinkAuthorizationService linkAuthorizationService;

	@Test
	void assertReadableShouldPassWhenAccessible() {
		when(roomLinkRepository.existsAccessibleLinkForUser(10L, 100L)).thenReturn(true);

		assertThatCode(() -> linkAuthorizationService.assertReadable(100L, 10L))
				.doesNotThrowAnyException();
	}

	@Test
	void assertReadableShouldThrowForbiddenWhenInaccessible() {
		when(roomLinkRepository.existsAccessibleLinkForUser(10L, 100L)).thenReturn(false);

		assertThatThrownBy(() -> linkAuthorizationService.assertReadable(100L, 10L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.E403_FORBIDDEN));
	}
}
