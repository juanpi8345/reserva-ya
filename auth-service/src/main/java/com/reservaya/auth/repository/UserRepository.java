package com.reservaya.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reservaya.auth.entity.UserSec;

@Repository
public interface UserRepository extends JpaRepository<UserSec, Long> {
	Optional<UserSec> findByUsername(String username);
	boolean existsByUsername(String username);
}
