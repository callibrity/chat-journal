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
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
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
