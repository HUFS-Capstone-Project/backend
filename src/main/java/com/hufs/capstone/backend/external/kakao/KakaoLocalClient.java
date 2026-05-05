package com.hufs.capstone.backend.external.kakao;

import com.hufs.capstone.backend.place.application.dto.ExternalPlaceCandidateSearchQuery;
import com.hufs.capstone.backend.place.domain.vo.PlaceSnapshot;
import java.util.List;

public interface KakaoLocalClient {

	List<PlaceSnapshot> searchByKeyword(ExternalPlaceCandidateSearchQuery query);
}
