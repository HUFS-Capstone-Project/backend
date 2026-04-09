package com.hufs.capstone.backend.user.domain.repository;

import com.hufs.capstone.backend.user.domain.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByIdAndDeletedAtIsNull(Long userId);
}



