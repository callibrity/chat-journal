package com.callibrity.ai.chatjournal.autoconfigure;

import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.journal")
public class ChatJournalProperties {

    /**
     * Maximum number of tokens before compaction is triggered.
     */
    private int maxTokens = 8192;

    /**
     * Minimum number of entries to retain after compaction.
     */
    private int minRetainedEntries = 6;

    /**
     * Characters per token for simple token calculator.
     */
    private int charactersPerToken = 4;

    /**
     * Encoding type for JTokkit token calculator.
     */
    private EncodingType encodingType = EncodingType.O200K_BASE;

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getMinRetainedEntries() {
        return minRetainedEntries;
    }

    public void setMinRetainedEntries(int minRetainedEntries) {
        this.minRetainedEntries = minRetainedEntries;
    }

    public int getCharactersPerToken() {
        return charactersPerToken;
    }

    public void setCharactersPerToken(int charactersPerToken) {
        this.charactersPerToken = charactersPerToken;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }
}
