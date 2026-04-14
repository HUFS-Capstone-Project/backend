package com.hufs.capstone.backend.link.application.dto;

import com.hufs.capstone.backend.link.domain.LinkAnalysisStatus;

/**
 * 링크 등록 유스케이스의 결과.
 * <p>
 * TODO: 저장소에 생성된 링크 id, processing job id, 클라이언트에 노출할 필드 등은
 * 정책 확정 후 확장한다. 현재는 골격만 둔다.
 */
public record RegisterLinkResult(
		LinkAnalysisStatus analysisStatus
) {
}
