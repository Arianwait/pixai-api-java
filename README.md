# pixai-api-java

A minimal, dependency-light Java client for the [PixAI](https://pixai.art) image-generation
API. Give it a prompt; it starts a generation task, waits for completion, and gives you the
resulting image as bytes or saves it to disk.

> **Note on the name:** despite "node" in the repository name, this project is pure Java and
> has no Node.js dependency.

## Features

- Fluent, immutable API — configure the client and each request with builders.
- High-level `generate(...)` (submit → wait → fetch) plus low-level task/status/media methods.
- Methods **return** results (`byte[]`, `Path`, `TaskStatus`) instead of printing/saving for you.
- A single unchecked exception type (`PixAIException`) for all API/transport failures.
- Java 17, only OkHttp and `org.json` at runtime.

## Tech stack

Java 17 · OkHttp · `org.json` · Maven

## Installation

The library is not published to a public registry yet. Until then, build and install it into
your local Maven repository, then depend on it:

```bash
./mvnw install
```

```xml
<dependency>
  <groupId>kz.awsstudio.pixai</groupId>
  <artifactId>pixai-api-java</artifactId>
  <version>1.0.0</version>
</dependency>
```

> Once published (JitPack or Maven Central), this section will be updated with the coordinates
> needed to consume it without a local install.

## Usage

```java
import kz.awsstudio.pixai.*;
import java.nio.file.Paths;

PixAIClient client = PixAIClient.builder()
        .apiKey(System.getenv("PIXAI_API_KEY")) // never hardcode the key
        .build();

GenerationParameters params = GenerationParameters.builder()
        .prompts("a fox in a forest, digital art")
        .negativePrompt("low quality")
        .modelId("1648918127446573124")
        .width(768).height(1280)
        .samplingSteps(50)
        .build();

GeneratedImage image = client.generate(params); // blocks until the task finishes
image.saveTo(Paths.get("out"));                  // -> out/picture_PixAI_<timestamp>.png
byte[] bytes = image.getBytes();                 // ...or work with the bytes directly
```

### Lower-level control

```java
String taskId       = client.createTask(params);
TaskStatus status   = client.awaitCompletion(taskId); // COMPLETED / FAILED / CANCELLED
if (status == TaskStatus.COMPLETED) {
    GeneratedImage image = client.getMedia(taskId);
}
```

### Client configuration

| Builder method        | Default                          | Description                              |
|-----------------------|----------------------------------|------------------------------------------|
| `apiKey(String)`      | — (required)                     | PixAI API key.                           |
| `baseUrl(String)`     | `https://api.pixai.art/graphql`  | GraphQL endpoint.                        |
| `pollInterval(Duration)` | 5s                            | How often status is polled while waiting.|
| `timeout(Duration)`   | 10 min                           | Max wait before giving up on a task.     |
| `httpClient(OkHttpClient)` | new `OkHttpClient()`        | Custom HTTP client (proxy, timeouts...). |

A convenience constructor `new PixAIClient(apiKey)` uses all defaults.

## Building from source

Requirements: JDK 17+ (a JDK 21/25 also works). Maven is **not** required — use the bundled
wrapper:

```bash
./mvnw clean verify   # compile + run the offline tests
./mvnw package        # build the jar (+ sources & javadoc jars) into target/
```

There is a runnable sample at [`examples/PixAIExample.java`](examples/PixAIExample.java) (not
part of the build).

## ⚠️ Security

Do not hardcode or commit your PixAI API key. Pass it from an environment variable or a local
config file listed in `.gitignore`. If a key was ever committed, rotate it in your PixAI
account — deleting the file does not remove it from git history.

## License

[MIT](LICENSE) © Yelzhan Ibragimov (Arianwait)
