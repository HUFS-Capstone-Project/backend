package com.hufs.capstone.backend.room.domain.repository;

import com.hufs.capstone.backend.room.domain.entity.Room;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Optional<Room> findByPublicId(String publicId);

	/**
	 * 동일 room aggregate에 대한 leave 요청을 row lock 하에서 순차 처리하기 위한 조회.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.publicId = :publicId")
	Optional<Room> findByPublicIdForUpdate(@Param("publicId") String publicId);

	Optional<Room> findByInviteCode(String inviteCode);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.inviteCode = :inviteCode")
	Optional<Room> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

	boolean existsByInviteCode(String inviteCode);
}
