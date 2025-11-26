package com.callibrity.ai.chatjournal.jtokkit;

import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class JTokkitTokenUsageCalculator implements TokenUsageCalculator {

    private final Encoding encoding;

    public JTokkitTokenUsageCalculator(EncodingType encodingType) {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(encodingType);
    }

    public int calculateTokenUsage(Message message) {
        return ofNullable(message.getText())
                .map(encoding::countTokens)
                .orElse(0);
    }

    @Override
    public int calculateTokenUsage(List<Message> messages) {
        return messages.stream()
                .mapToInt(this::calculateTokenUsage)
                .sum();
    }
}
