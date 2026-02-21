package com.argus.domain.port.persistence;

import com.argus.domain.model.User;
import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);

    Optional<User> findBySupabaseUid(String supabaseUid);
}
