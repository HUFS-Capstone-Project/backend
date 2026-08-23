package com.hufs.capstone.backend.region.application;

import com.hufs.capstone.backend.region.domain.vo.ResolvedRegion;

public interface RegionAddressResolver {

	ResolvedRegion resolve(String address, String roadAddress);
}
