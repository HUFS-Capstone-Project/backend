package com.hufs.capstone.backend.link.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hufs.capstone.backend.global.exception.BusinessException;
import com.hufs.capstone.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class LinkPlaceCandidateSnapshotMapperTest {

	private final LinkPlaceCandidateSnapshotMapper mapper = new LinkPlaceCandidateSnapshotMapper(new ObjectMapper());

	@Test
	void readShouldConvertCorruptedStoredSnapshotToServerError() {
		assertThatThrownBy(() -> mapper.read("[{"))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
						.isEqualTo(ErrorCode.LINK_CANDIDATE_SNAPSHOT_CORRUPTED));
	}
}
