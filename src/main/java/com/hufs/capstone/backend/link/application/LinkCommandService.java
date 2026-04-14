package com.hufs.capstone.backend.link.application;

import com.hufs.capstone.backend.link.application.dto.RegisterLinkCommand;
import com.hufs.capstone.backend.link.application.dto.RegisterLinkResult;
import org.springframework.stereotype.Service;

/**
 * 링크 등록·분석 요청을 처리하는 application 계층 진입점 (골격).
 * <p>
 * 설계상 열어둘 결정 사항:
 * <ul>
 *   <li><b>트랜잭션 경계</b>: 링크 행 저장과 {@code ProcessingClient#createJob} 호출을 한 트랜잭션에 둘지,
 *       아니면 로컬 커밋 후 비동기 호출로 나눌지. 외부 API는 2PC에 참여하지 않으므로
 *       일반적으로 “등록 성공 + 분석 실패” 상태를 허용할지 정책이 필요하다.</li>
 *   <li><b>실패 의미</b>: processing 호출 실패 시 사용자에게는 “등록 실패”로 보일지,
 *       “등록됐으나 분석 대기 중/재시도”로 보일지.</li>
 *   <li><b>진행 확인</b>: job 상태를 {@code ProcessingClient#getJob} 등으로 polling 할지,
 *       webhook/이벤트로 받을지.</li>
 *   <li><b>DTO 승격</b>: {@code external.processing.dto} 응답을 어느 시점에서 application DTO/도메인 결과로
 *       매핑할지 (보통 이 서비스 내에서 한 번만 수행한다).</li>
 * </ul>
 */
@Service
public class LinkCommandService {

	/**
	 * 링크를 등록하고 필요 시 processing job을 생성한다. 아직 미구현.
	 *
	 * @throws UnsupportedOperationException 구현 전까지 호출 시
	 */
	public RegisterLinkResult register(RegisterLinkCommand command) {
		throw new UnsupportedOperationException(
				"TODO: LinkCommandService.register — Link 엔티티/저장소, ProcessingClient 연동, "
						+ "실패 시 정책이 확정되면 구현한다.");
	}
}
