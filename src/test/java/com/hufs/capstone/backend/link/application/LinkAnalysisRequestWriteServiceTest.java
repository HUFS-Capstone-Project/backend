package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.hufs.capstone.backend.link.domain.LinkSourceType;
import com.hufs.capstone.backend.link.domain.entity.Link;
import com.hufs.capstone.backend.link.domain.entity.LinkAnalysisRequest;
import com.hufs.capstone.backend.link.domain.entity.LinkProcessingDispatchAttempt;
import com.hufs.capstone.backend.link.domain.repository.LinkAnalysisRequestRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkProcessingDispatchAttemptRepository;
import com.hufs.capstone.backend.link.domain.repository.LinkRepository;
import com.hufs.capstone.backend.room.application.RoomAccessService;
import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class LinkAnalysisRequestWriteServiceTest {

	@Mock
	private LinkRepository linkRepository;

	@Mock
	private LinkAnalysisRequestRepository linkAnalysisRequestRepository;

	@Mock
	private LinkProcessingDispatchAttemptRepository linkProcessingDispatchAttemptRepository;

	@Mock
	private RoomAccessService roomAccessService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private LinkProcessingDispatchPolicy dispatchPolicy;

	@InjectMocks
	private LinkAnalysisRequestWriteService service;

	@Test
	void shouldStoreOriginalAndNormalizedUrlSeparatelyWhenRegisteringNewLink() {
		Room room = Room.create("room-public-id", "Room", "INVITE123", 100L);
		ReflectionTestUtils.setField(room, "id", 1L);
		LinkUrlNormalizer.NormalizedUrl normalizedUrl = LinkUrlNormalizer.normalize(
				"https://m.instagram.com/reels/abc/?igsh=tracking"
		);
		ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);
		ArgumentCaptor<LinkAnalysisRequest> requestCaptor = ArgumentCaptor.forClass(LinkAnalysisRequest.class);
		ArgumentCaptor<LinkProcessingDispatchAttempt> dispatchAttemptCaptor =
				ArgumentCaptor.forClass(LinkProcessingDispatchAttempt.class);

		when(roomAccessService.requireMemberRoom("room-public-id", 100L)).thenReturn(room);
		when(linkRepository.findByNormalizedUrl(normalizedUrl.normalizedUrl())).thenReturn(Optional.empty());
		when(linkRepository.saveAndFlush(linkCaptor.capture())).thenAnswer(invocation -> {
			Link saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 10L);
			return saved;
		});
		when(linkAnalysisRequestRepository.findByRoomAndLinkId(room, 10L)).thenReturn(Optional.empty());
		when(linkAnalysisRequestRepository.saveAndFlush(requestCaptor.capture())).thenAnswer(invocation -> {
			LinkAnalysisRequest saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", 20L);
			return saved;
		});
		when(linkProcessingDispatchAttemptRepository.saveAndFlush(dispatchAttemptCaptor.capture()))
				.thenAnswer(invocation -> {
					LinkProcessingDispatchAttempt saved = invocation.getArgument(0);
					ReflectionTestUtils.setField(saved, "id", 30L);
					return saved;
				});

		service.requestWithinWriteTransaction(normalizedUrl, "room-public-id", 100L, "WEB");

		Link savedLink = linkCaptor.getValue();
		assertThat(savedLink.getOriginalUrl()).isEqualTo("https://m.instagram.com/reels/abc/?igsh=tracking");
		assertThat(savedLink.getNormalizedUrl()).isEqualTo("https://www.instagram.com/reel/abc/");
		assertThat(savedLink.getLinkSourceType()).isEqualTo(LinkSourceType.INSTAGRAM);
		assertThat(requestCaptor.getValue().getOriginalUrl()).isEqualTo(savedLink.getOriginalUrl());
		assertThat(dispatchAttemptCaptor.getValue().getLink()).isSameAs(savedLink);
		assertThat(dispatchAttemptCaptor.getValue().getOriginalUrl()).isEqualTo(savedLink.getOriginalUrl());
		assertThat(dispatchAttemptCaptor.getValue().getRoomId()).isEqualTo("room-public-id");
	}
}
