# Chat Journal

Chat Journal is a Spring Boot starter for chat memory management with automatic compaction using [Spring AI](https://spring.io/projects/spring-ai). It provides persistent conversation storage with intelligent token-based compaction to keep memory within limits while preserving context.

![Maven Central Version](https://img.shields.io/maven-central/v/com.callibrity.ai/chat-journal-spring-boot-starter)
![GitHub License](https://img.shields.io/github/license/callibrity/chat-journal)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_chat-journal&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_chat-journal)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_chat-journal&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_chat-journal)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_chat-journal&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_chat-journal)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=callibrity_chat-journal&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=callibrity_chat-journal)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=callibrity_chat-journal&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=callibrity_chat-journal)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=callibrity_chat-journal&metric=coverage)](https://sonarcloud.io/summary/new_code?id=callibrity_chat-journal)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=callibrity_chat-journal&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=callibrity_chat-journal)

## Features

- **Persistent Chat Memory**: Store conversation history in a database using JDBC
- **Automatic Compaction**: Intelligently summarize older messages when token limits are exceeded
- **Token Counting**: Accurate token counting using JTokkit (supports OpenAI tokenizers)
- **Memory Usage Monitoring**: Query token usage statistics to display memory budget consumption
- **Spring Boot Auto-Configuration**: Zero-config setup with sensible defaults
- **Multi-Database Support**: Schema files for PostgreSQL, MySQL, MariaDB, Oracle, SQL Server, and H2

## Getting Started

Add the Spring Boot starter dependency to your project:

```xml
<dependency>
    <groupId>com.callibrity.ai</groupId>
    <artifactId>chat-journal-spring-boot-starter</artifactId>
    <version>${chat-journal.version}</version>
</dependency>
```

Chat Journal will automatically configure itself with Spring AI's `ChatMemory` interface. Just inject and use it:

```java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String question,
                       @RequestParam(required = false) String conversationId) {
        return chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
                        conversationId != null ? conversationId : UUID.randomUUID().toString()))
                .call()
                .content();
    }
}
```

### Streaming with WebFlux

If you're using Spring WebFlux, you can return a `Flux<String>` directly for SSE streaming:

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String question,
                           @RequestParam(required = false) String conversationId) {
    return chatClient.prompt()
            .user(question)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
                    conversationId != null ? conversationId : UUID.randomUUID().toString()))
            .stream()
            .content();
}
```

### Streaming with WebMVC

If you're using Spring WebMVC (servlet-based), use `SseEmitter` to stream the response:

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@RequestParam String question,
                         @RequestParam(required = false) String conversationId) {
    var emitter = new SseEmitter();

    chatClient.prompt()
            .user(question)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
                    conversationId != null ? conversationId : UUID.randomUUID().toString()))
            .stream()
            .content()
            .subscribe(
                    chunk -> {
                        try {
                            emitter.send(chunk);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    emitter::completeWithError,
                    emitter::complete
            );

    return emitter;
}
```

## Configuration

Configure Chat Journal using application properties:

```properties
# Maximum tokens before compaction is triggered
chat.journal.max-tokens=8192

# Minimum number of recent entries to always retain (never compacted)
chat.journal.min-retained-entries=6

# JTokkit encoding type for token counting (default: O200K_BASE)
chat.journal.encoding-type=O200K_BASE
```

### Available Encoding Types

Chat Journal uses [JTokkit](https://github.com/knuddelsgmbh/jtokkit) for token counting. Available encoding types:

- `O200K_BASE` (default) - Used by GPT-4o and newer models
- `CL100K_BASE` - Used by GPT-4 and GPT-3.5-turbo
- `P50K_BASE` - Used by older GPT-3 models
- `R50K_BASE` - Used by older GPT-3 models

## Memory Usage Monitoring

Chat Journal provides an API to monitor memory usage for conversations, allowing you to display how much of the token budget is being used before compaction occurs.

### Using ChatMemoryUsageProvider

Inject `ChatMemoryUsageProvider` to query memory usage statistics:

```java
@RestController
public class ChatController {

    private final ChatMemoryUsageProvider memoryUsageProvider;

    public ChatController(ChatMemoryUsageProvider memoryUsageProvider) {
        this.memoryUsageProvider = memoryUsageProvider;
    }

    @GetMapping("/memory-usage")
    public ChatMemoryUsage getMemoryUsage(@RequestParam String conversationId) {
        return memoryUsageProvider.getMemoryUsage(conversationId);
    }
}
```

### ChatMemoryUsage Record

The `ChatMemoryUsage` record provides:

| Method | Description |
|--------|-------------|
| `currentTokens()` | Number of tokens currently used by conversation history |
| `maxTokens()` | Maximum tokens allowed before compaction is triggered |
| `percentageUsed()` | Percentage of budget used (0.0 to 100.0+, may exceed 100 if over budget) |
| `tokensRemaining()` | Tokens remaining before reaching the maximum (0 if at or over limit) |

Example response:

```json
{
  "currentTokens": 4096,
  "maxTokens": 8192,
  "percentageUsed": 50.0,
  "tokensRemaining": 4096
}
```

## Database Setup

Chat Journal requires a `chat_journal_entry` table. Schema files are provided for common databases:

- `schema-postgresql.sql`
- `schema-mysql.sql`
- `schema-mariadb.sql`
- `schema-oracle.sql`
- `schema-sqlserver.sql`
- `schema-h2.sql`

Enable automatic schema initialization in your `application.properties`:

```properties
spring.sql.init.mode=always
spring.sql.init.platform=postgresql
```

## Running the Example Application

Chat Journal includes an example application demonstrating integration with OpenAI:

1. Navigate to the `chat-journal-example` directory:
    ```bash
    cd chat-journal-example
    ```

2. Set your OpenAI API key:
    ```bash
    export SPRING_AI_OPENAI_API_KEY=sk-...
    ```

3. Run the application using Maven:
    ```bash
    mvn spring-boot:run
    ```

4. Open your browser to `http://localhost:8080` to interact with the chat interface.

The example uses Docker Compose to automatically start a PostgreSQL database.

## Modules

| Module | Description |
|--------|-------------|
| `chat-journal-core` | Core functionality for chat memory management and compaction |
| `chat-journal-jdbc` | JDBC-based persistence for chat journal entries |
| `chat-journal-jtokkit` | JTokkit-based token counting implementation |
| `chat-journal-autoconfigure` | Spring Boot auto-configuration |
| `chat-journal-spring-boot-starter` | Starter dependency that pulls in all required modules |
| `chat-journal-example` | Example Spring Boot application with OpenAI integration |

## Building from Source

To build the project yourself, clone the repository and use [Apache Maven](https://maven.apache.org/):

```bash
mvn clean install
```

## License

This project is licensed under the Apache License 2.0—see the [LICENSE](LICENSE) file for details.
