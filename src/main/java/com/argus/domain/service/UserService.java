package com.argus.domain.service;

import com.argus.domain.model.User;
import com.argus.domain.port.persistence.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserPersistencePort userPersistencePort;

    public User getOrCreateUser(String supabaseUid, String email) {
        return userPersistencePort.findBySupabaseUid(supabaseUid)
                .orElseGet(() -> {
                    try {
                        log.info("Provisioning new user for supabaseUid: {}", supabaseUid);
                        User newUser = User.builder()
                                .supabaseUid(supabaseUid)
                                .email(email)
                                .build();
                        return userPersistencePort.save(newUser);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        log.debug("User already created by concurrent request for: {}", supabaseUid);
                        return userPersistencePort.findBySupabaseUid(supabaseUid)
                                .orElseThrow(
                                        () -> new RuntimeException("Failed to find or create user: " + supabaseUid));
                    }
                });
    }
}
