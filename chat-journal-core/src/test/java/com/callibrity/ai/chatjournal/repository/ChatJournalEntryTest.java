package com.callibrity.ai.chatjournal.repository;

import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.UncheckedIOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

class ChatJournalEntryTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    class ToMessage {

        @Test
        void shouldConvertUserEntry() {
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.USER.name(), "Hello", 10);

            Message message = entry.toMessage(objectMapper);

            assertThat(message).isInstanceOf(UserMessage.class);
            assertThat(message.getText()).isEqualTo("Hello");
        }

        @Test
        void shouldConvertAssistantEntry() {
            ChatJournalEntry entry = new ChatJournalEntry(2, MessageType.ASSISTANT.name(), "Hi there!", 15);

            Message message = entry.toMessage(objectMapper);

            assertThat(message).isInstanceOf(AssistantMessage.class);
            assertThat(message.getText()).isEqualTo("Hi there!");
        }

        @Test
        void shouldConvertSystemEntry() {
            ChatJournalEntry entry = new ChatJournalEntry(3, MessageType.SYSTEM.name(), "You are a helpful assistant", 20);

            Message message = entry.toMessage(objectMapper);

            assertThat(message).isInstanceOf(SystemMessage.class);
            assertThat(message.getText()).isEqualTo("You are a helpful assistant");
        }
    }

    @Nested
    class ToolResponseSerialization {

        @Test
        void shouldDeserializeSingleToolResponse() {
            String json = """
                    [{"id":"tool-1","name":"calculator","responseData":"42"}]
                    """;
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.TOOL.name(), json, 50);

            Message message = entry.toMessage(objectMapper);

            assertThat(message).isInstanceOf(ToolResponseMessage.class);
            ToolResponseMessage toolMessage = (ToolResponseMessage) message;
            List<ToolResponse> responses = toolMessage.getResponses();

            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().id()).isEqualTo("tool-1");
            assertThat(responses.getFirst().name()).isEqualTo("calculator");
            assertThat(responses.getFirst().responseData()).isEqualTo("42");
        }

        @Test
        void shouldDeserializeMultipleToolResponses() {
            String json = """
                    [
                        {"id":"tool-1","name":"search","responseData":"Result 1"},
                        {"id":"tool-2","name":"calculator","responseData":"100"}
                    ]
                    """;
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.TOOL.name(), json, 100);

            Message message = entry.toMessage(objectMapper);

            ToolResponseMessage toolMessage = (ToolResponseMessage) message;
            List<ToolResponse> responses = toolMessage.getResponses();

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).id()).isEqualTo("tool-1");
            assertThat(responses.get(0).name()).isEqualTo("search");
            assertThat(responses.get(1).id()).isEqualTo("tool-2");
            assertThat(responses.get(1).name()).isEqualTo("calculator");
        }

        @Test
        void shouldDeserializeEmptyToolResponses() {
            String json = "[]";
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.TOOL.name(), json, 10);

            Message message = entry.toMessage(objectMapper);

            ToolResponseMessage toolMessage = (ToolResponseMessage) message;
            assertThat(toolMessage.getResponses()).isEmpty();
        }

        @Test
        void shouldHandleComplexResponseData() {
            String json = """
                    [{"id":"tool-1","name":"weather","responseData":"Sunny, 72°F with 10% humidity"}]
                    """;
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.TOOL.name(), json, 50);

            Message message = entry.toMessage(objectMapper);

            ToolResponseMessage toolMessage = (ToolResponseMessage) message;
            assertThat(toolMessage.getResponses().getFirst().responseData())
                    .isEqualTo("Sunny, 72°F with 10% humidity");
        }

        @Test
        void shouldThrowUncheckedIOExceptionForInvalidJson() {
            String invalidJson = "not valid json";
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.TOOL.name(), invalidJson, 10);

            assertThatThrownBy(() -> entry.toMessage(objectMapper))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("Failed to deserialize tool responses");
        }

        @Test
        void shouldThrowUncheckedIOExceptionForMalformedJson() {
            String malformedJson = "[{\"id\":\"tool-1\",\"name\":";
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.TOOL.name(), malformedJson, 10);

            assertThatThrownBy(() -> entry.toMessage(objectMapper))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("Failed to deserialize tool responses");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class FromMessage {

        @Mock
        private TokenUsageCalculator tokenUsageCalculator;

        @BeforeEach
        void setUp() {
            lenient().when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
        }

        @Test
        void shouldCreateEntryFromUserMessage() {
            UserMessage message = new UserMessage("Hello");

            ChatJournalEntry entry = ChatJournalEntry.fromMessage(message, objectMapper, tokenUsageCalculator);

            assertThat(entry.messageIndex()).isEqualTo(0);
            assertThat(entry.messageType()).isEqualTo(MessageType.USER.name());
            assertThat(entry.content()).isEqualTo("Hello");
            assertThat(entry.tokens()).isEqualTo(100);
        }

        @Test
        void shouldCreateEntryFromAssistantMessage() {
            AssistantMessage message = new AssistantMessage("Hi there!");

            ChatJournalEntry entry = ChatJournalEntry.fromMessage(message, objectMapper, tokenUsageCalculator);

            assertThat(entry.messageType()).isEqualTo(MessageType.ASSISTANT.name());
            assertThat(entry.content()).isEqualTo("Hi there!");
        }

        @Test
        void shouldCreateEntryFromSystemMessage() {
            SystemMessage message = new SystemMessage("You are helpful");

            ChatJournalEntry entry = ChatJournalEntry.fromMessage(message, objectMapper, tokenUsageCalculator);

            assertThat(entry.messageType()).isEqualTo(MessageType.SYSTEM.name());
            assertThat(entry.content()).isEqualTo("You are helpful");
        }

        @Test
        void shouldSerializeToolResponseMessage() {
            ToolResponseMessage message = ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponse("tool-1", "calculator", "42")))
                    .build();

            ChatJournalEntry entry = ChatJournalEntry.fromMessage(message, objectMapper, tokenUsageCalculator);

            assertThat(entry.messageType()).isEqualTo(MessageType.TOOL.name());
            assertThat(entry.content()).contains("tool-1");
            assertThat(entry.content()).contains("calculator");
            assertThat(entry.content()).contains("42");
        }

        @Test
        void shouldThrowUncheckedIOExceptionWhenSerializationFails() throws JsonProcessingException {
            ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
            when(failingMapper.writeValueAsString(any()))
                    .thenThrow(new JsonProcessingException("Simulated failure") {});

            ToolResponseMessage message = ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponse("tool-1", "test", "data")))
                    .build();

            assertThatThrownBy(() -> ChatJournalEntry.fromMessage(message, failingMapper, tokenUsageCalculator))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasMessageContaining("Failed to serialize tool responses");
        }
    }
}
