package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.hufs.capstone.backend.global.exception.FieldValidationException;
import com.hufs.capstone.backend.link.application.dto.SaveManualRoomPlaceCommand;
import com.hufs.capstone.backend.link.domain.repository.LinkCandidateRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkCandidateOverrideRepository;
import com.hufs.capstone.backend.link.domain.repository.RoomLinkRepository;
import com.hufs.capstone.backend.place.application.RoomPlaceStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomPlaceCommandWriteServiceTest {

	@Mock
	private LinkAnalysisAuthorizationService linkAnalysisAuthorizationService;
	@Mock
	private RoomLinkRepository roomLinkRepository;
	@Mock
	private LinkCandidateRepository linkCandidateRepository;
	@Mock
	private RoomLinkCandidateOverrideRepository overrideRepository;
	@Mock
	private RoomPlaceStorageService roomPlaceStorageService;

	@InjectMocks
	private RoomPlaceCommandWriteService roomPlaceCommandWriteService;

	@Test
	void saveManualRoomPlaceShouldRejectNullCommandAsFieldValidation() {
		assertThatThrownBy(() -> roomPlaceCommandWriteService.saveManualRoomPlaceWithinTransaction(
				100L,
				"room-id",
				1L,
				null
		))
				.isInstanceOf(FieldValidationException.class)
				.satisfies(ex -> assertThat(((FieldValidationException) ex).getFieldErrors())
						.anySatisfy(error -> assertThat(error.field()).isEqualTo("snapshot")));

		verifyNoInteractions(linkAnalysisAuthorizationService, roomPlaceStorageService);
	}

	@Test
	void saveManualRoomPlaceShouldRejectNullSnapshotAsFieldValidation() {
		assertThatThrownBy(() -> roomPlaceCommandWriteService.saveManualRoomPlaceWithinTransaction(
				100L,
				"room-id",
				1L,
				new SaveManualRoomPlaceCommand(null)
		))
				.isInstanceOf(FieldValidationException.class)
				.satisfies(ex -> assertThat(((FieldValidationException) ex).getFieldErrors())
						.anySatisfy(error -> assertThat(error.field()).isEqualTo("snapshot")));

		verifyNoInteractions(linkAnalysisAuthorizationService, roomPlaceStorageService);
	}
}
