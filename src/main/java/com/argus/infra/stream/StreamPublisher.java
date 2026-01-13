package com.argus.infra.stream;

import java.util.Map;
import com.argus.infra.stream.dto.TransactionEvent;

public interface StreamPublisher {
    String publish(String streamName, Map<String, String> fields);

    String publishEvent(TransactionEvent event);
}
