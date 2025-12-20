package com.argus.domain.port.persistence;

import com.argus.domain.model.Signal;

public interface SignalPersistencePort {
    Signal save(Signal signal);
}
