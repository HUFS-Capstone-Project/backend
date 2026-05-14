package com.hufs.capstone.backend.user.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
		@NotBlank(message = "닉네임은 필수입니다.")
		@Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다.")
		String nickname
) {

	public UpdateNicknameRequest {
		if (nickname != null) {
			nickname = nickname.trim();
		}
	}
}
