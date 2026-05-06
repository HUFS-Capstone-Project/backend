package com.hufs.capstone.backend.region.application;

import com.hufs.capstone.backend.region.application.dto.RegionFilter;
import com.hufs.capstone.backend.region.application.dto.RegionOptionResult;
import java.util.List;

public interface RegionQueryService {

	List<RegionOptionResult> getSidos();

	List<RegionOptionResult> getSigungus(String sidoCode);

	RegionFilter validateFilter(String sidoCode, String sigunguCode);
}
