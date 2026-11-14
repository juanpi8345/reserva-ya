package com.reservaya.notification_service.service.impl;

import java.util.Optional;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reservaya.notification_service.entity.UserContact;
import com.reservaya.notification_service.event.model.UserCreatedEvent;
import com.reservaya.notification_service.repository.UserContactRepository;
import com.reservaya.notification_service.service.UserContactService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserContactServiceImpl implements UserContactService {

    private final UserContactRepository userContactRepository;

    @Override
    @Cacheable(value = "userContacts", key = "#userId")
    public UserContact getUserContact(Long userId) {
        return userContactRepository.findById(userId).orElse(null);
    }

    @Override
    @Transactional
    @CachePut(value = "userContacts", key = "#event.getUserId()")
    public UserContact saveUserContact(UserCreatedEvent event) {
        Long userId = event.getUserId();
        log.info("Guardando datos de contacto para userId={}", userId);

        Optional<UserContact> existing = userContactRepository.findById(userId);

        if (existing.isPresent()) {
            log.warn("Datos de contacto para userId={} ya existen. Ignorando evento duplicado.", userId);
            return existing.get();
        }

        UserContact userContact = new UserContact(userId, event.getPhone(), event.getEmail());
        return userContactRepository.save(userContact);
    }
}
