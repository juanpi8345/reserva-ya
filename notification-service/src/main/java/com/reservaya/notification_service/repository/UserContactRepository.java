package com.reservaya.notification_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reservaya.notification_service.entity.UserContact;

@Repository
public interface UserContactRepository extends JpaRepository<UserContact, Long>{
}
