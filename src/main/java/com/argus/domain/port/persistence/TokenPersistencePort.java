package com.argus.domain.port.persistence;

import com.argus.domain.model.Token;

public interface TokenPersistencePort {
    Token save(Token token);

}
