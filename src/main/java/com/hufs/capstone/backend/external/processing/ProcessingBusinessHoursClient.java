package com.hufs.capstone.backend.external.processing;

import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobCreateRequest;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobCreateResponse;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursJobLookupResponse;
import com.hufs.capstone.backend.external.processing.dto.BusinessHoursPlaceResponse;
import java.util.Optional;

public interface ProcessingBusinessHoursClient {

	BusinessHoursJobCreateResponse createJob(BusinessHoursJobCreateRequest request);

	BusinessHoursJobLookupResponse getJob(String jobId);

	Optional<BusinessHoursPlaceResponse> getPlace(String kakaoPlaceId);
}
