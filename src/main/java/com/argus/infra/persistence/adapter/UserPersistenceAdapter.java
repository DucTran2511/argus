package com.argus.infra.persistence.adapter;

import com.argus.domain.model.User;
import com.argus.domain.port.persistence.UserPersistencePort;
import com.argus.infra.persistence.entity.UserEntity;
import com.argus.infra.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository repository;

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findBySupabaseUid(String supabaseUid) {
        return repository.findBySupabaseUid(supabaseUid)
                .map(this::toDomain);
    }

    private User toDomain(UserEntity entity) {
        if (entity == null)
            return null;
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .supabaseUid(entity.getSupabaseUid())
                .telegramChatId(entity.getTelegramChatId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserEntity toEntity(User domain) {
        if (domain == null)
            return null;
        UserEntity entity = new UserEntity();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail());
        entity.setSupabaseUid(domain.getSupabaseUid());
        entity.setTelegramChatId(domain.getTelegramChatId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
