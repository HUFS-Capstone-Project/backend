package com.hufs.capstone.backend.auth.oauth.userinfo;

import com.hufs.capstone.backend.auth.oauth.SocialIdentity;

public interface OAuth2UserInfo {

	SocialIdentity toIdentity();
}



