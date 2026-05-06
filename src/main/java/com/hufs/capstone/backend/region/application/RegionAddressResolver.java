package com.hufs.capstone.backend.region.application;

import com.hufs.capstone.backend.region.application.dto.ResolvedRegion;

public interface RegionAddressResolver {

	ResolvedRegion resolve(String address, String roadAddress);
}
