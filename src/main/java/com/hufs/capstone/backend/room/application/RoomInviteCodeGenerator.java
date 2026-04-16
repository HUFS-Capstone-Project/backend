package com.hufs.capstone.backend.room.application;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class RoomInviteCodeGenerator {

	private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final int INVITE_CODE_LENGTH = 12;

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		char[] chars = new char[INVITE_CODE_LENGTH];
		for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
			chars[i] = BASE62[random.nextInt(BASE62.length)];
		}
		return new String(chars);
	}
}

