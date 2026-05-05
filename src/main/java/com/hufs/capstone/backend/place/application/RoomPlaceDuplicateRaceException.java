package com.hufs.capstone.backend.place.application;

public class RoomPlaceDuplicateRaceException extends RuntimeException {

	public RoomPlaceDuplicateRaceException(Throwable cause) {
		super("Room place duplicate race detected.", cause);
	}
}
