package com.argus.core.security;

import com.argus.domain.model.User;
import com.argus.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class UserContext {

    private final UserService userService;
    private User currentUser;

    public User getUser() {
        if (currentUser == null) {
            AuthenticatedUser auth = AuthContext.currentUser();
            currentUser = userService.getOrCreateUser(auth.supabaseUid(), auth.email());
        }
        return currentUser;
    }

    public UUID getUserId() {
        return getUser().getId();
    }
}
