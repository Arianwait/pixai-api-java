# pixai-api-java

A minimal, dependency-light Java client for the [PixAI](https://pixai.art) image-generation
**REST API v2**. Give it a prompt; it starts a generation task, waits for completion, and gives
you the resulting image(s) as bytes or saves them to disk.

## Features

- Fluent, immutable API — configure the client and each request with builders.
- High-level `generate(...)` (submit → wait → fetch) plus low-level task methods.
- Every documented request parameter has a typed builder method, with enums for `model`,
  `aspectRatio`, `size`, `mode` and `samplingMethod`.
- Type-aware validation of model-dependent parameters.
- Returns a `Task` with status, timestamps and **multiple** images.
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
  <groupId>kz.arianwaitstudio.pixai</groupId>
  <artifactId>pixai-api-java</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Quickstart (4 steps)

The default model is **Tsubaki.2**, so the only required field is the prompt: create the client,
pass your key, set the prompt, run.

```java
import kz.arianwaitstudio.pixai.*;
import java.nio.file.Paths;

PixAIClient client = new PixAIClient(System.getenv("PIXAI_API_KEY")); // 1) client + 2) key

GenerationParameters params = GenerationParameters.builder()
        .prompt("a fox in a forest, digital art")                    // 3) prompt (required)
        .build();

Task task = client.generate(params);                                  // 4) run (blocks until done)

task.images().get(0).saveTo(Paths.get("out"));                       // -> out/picture_PixAI_<timestamp>.png
```

## Gallery

<!-- Add a couple of generated samples here, e.g.:
![sample 1](docs/images/sample1.png)
![sample 2](docs/images/sample2.png)
-->

---

# Examples

### Full configuration

```java
PixAIClient client = PixAIClient.builder()
        .apiKey(System.getenv("PIXAI_API_KEY"))
        .pollInterval(java.time.Duration.ofSeconds(3)) // status poll interval (default 5s)
        .timeout(java.time.Duration.ofMinutes(5))      // max wait (default 10 min)
        .build();

GenerationParameters params = GenerationParameters.builder()
        .model(Model.HARUKA_V2)             // or .modelVersionId("...") for a custom model
        .prompt("1girl, kimono, cherry blossoms")
        .negativePrompt("low quality, blurry")
        .aspectRatio(AspectRatio.RATIO_9_16)
        .size(Size.ONE_K)
        .seed(42L)
        .promptHelper(false)                // don't auto-enhance the prompt
        .build();

Task task = client.generate(params);
for (GeneratedImage img : task.images()) {
    byte[] bytes = img.getBytes();                              // raw bytes
    java.nio.file.Path saved = img.saveTo(Paths.get("out"), "fox.png"); // or a custom file name
    System.out.println(img.getUrl() + " -> " + saved);
}
```

### Model-dependent parameters

```java
// Tsubaki.2 (DiT) — mode and style are available:
GenerationParameters.builder()
        .model(Model.TSUBAKI_2)          // optional — this is the default
        .prompt("knight in armor")
        .mode(Mode.ULTRA)                // lite/standard/pro/ultra — Tsubaki.2 only
        .style(Style.builder().set("name", "anime").build())
        .build();

// SDXL (Haruka / Hoshino) — sampling is available:
GenerationParameters.builder()
        .model(Model.HARUKA_V2)
        .prompt("knight in armor")
        .sampling(Sampling.builder()
                .method(SamplingMethod.DPMPP_2M_KARRAS)
                .set("steps", 25)        // sampling fields are flexible (see notes)
                .build())
        .build();
```

Mixing incompatible parameters throws at build time:

```java
GenerationParameters.builder()
        .model(Model.HARUKA_V2)  // SDXL
        .prompt("x")
        .mode(Mode.PRO)          // mode is Tsubaki.2-only
        .build();                // -> IllegalStateException
```

### LoRA (up to 5)

```java
GenerationParameters.builder()
        .prompt("cyberpunk city")
        .addLora("795123456789")               // by version id
        .addLora(Lora.of("888999000111", 0.8)) // with a weight
        .build();
```

### Low-level control (no blocking `generate`)

```java
Task created = client.createTask(params);            // returns immediately, status PENDING
Task done    = client.awaitCompletion(created.getId());
if (done.getStatus() == TaskStatus.COMPLETED) {
    done.images().forEach(i -> i.saveTo(Paths.get("out")));
}

Task snapshot = client.getTask(created.getId());     // or poll manually
System.out.println(snapshot.getStatus() + " / raw: " + snapshot.getRawStatus());
```

### Error handling

```java
try {
    Task t = client.generate(params);
} catch (PixAIException e) {
    // network failures, non-2xx responses (401/500), timeout, failed/cancelled task
    System.err.println("PixAI failed: " + e.getMessage());
}
```

### Resolution helper (no API call)

```java
int w = AspectRatio.RATIO_16_9.width(Size.ONE_K);             // 1280
int h = AspectRatio.RATIO_16_9.height(Size.ONE_K);            // 720
int w2 = AspectRatio.RATIO_16_9.width(Size.ONE_AND_A_HALF_K); // 1536
```

### Webhook instead of polling

```java
GenerationParameters params = GenerationParameters.builder()
        .prompt("a fox")
        .callbackUrl("https://my-server.example/pixai-webhook") // PixAI POSTs on status changes
        .build();

Task created = client.createTask(params); // don't wait — handle the webhook on your server
```

There is a runnable sample at [`examples/PixAIExample.java`](examples/PixAIExample.java) (not part
of the Maven build).

---

# API reference

## `PixAIClient`

The entry point. Immutable and thread-safe. All methods throw `PixAIException` on failure.

**Construction**

| Member | Description |
|--------|-------------|
| `new PixAIClient(String apiKey)` | Client with all defaults. |
| `PixAIClient.builder()` | Returns a `Builder` for full configuration. |

**`PixAIClient.Builder`**

| Method | Default | Description |
|--------|---------|-------------|
| `apiKey(String)` | — (required) | PixAI API key. |
| `baseUrl(String)` | `https://api.pixai.art` | API base URL. |
| `pollInterval(Duration)` | `5s` | How often status is polled while waiting. |
| `timeout(Duration)` | `10 min` | Max wait before giving up on a task. |
| `httpClient(OkHttpClient)` | new `OkHttpClient()` | Custom HTTP client (proxy, timeouts…). |
| `build()` | | Builds the client (validates `apiKey`/`baseUrl`). |

**Methods**

| Method | Returns | Description |
|--------|---------|-------------|
| `generate(GenerationParameters)` | `Task` | Submit → wait → return completed task. Throws if not `COMPLETED`. |
| `createTask(GenerationParameters)` | `Task` | Submit and return immediately (non-terminal status). |
| `getTask(String taskId)` | `Task` | Current state of a task. |
| `awaitCompletion(String taskId)` | `Task` | Poll until terminal status (or timeout). |

## `GenerationParameters`

Immutable request body; build with `GenerationParameters.builder()`. Getters: `getPrompt()`,
`getModelVersionId()`.

**`GenerationParameters.Builder`**

| Method | API field | Notes |
|--------|-----------|-------|
| `prompt(String)` | `prompt` | **Required.** |
| `model(Model)` | `modelVersionId` | Defaults to `Model.TSUBAKI_2`. Enables type-aware validation. |
| `modelVersionId(String)` | `modelVersionId` | Custom model (last URL segment); skips type validation. |
| `modelId(String)` | `modelId` | `@Deprecated` — use `model` / `modelVersionId`. |
| `negativePrompt(String)` | `negativePrompt` | What to exclude. |
| `aspectRatio(AspectRatio)` / `aspectRatio(String)` | `aspectRatio` | Default `1:1`. |
| `size(Size)` / `size(String)` | `size` | Default `1k`. |
| `mode(Mode)` / `mode(String)` | `mode` | **Tsubaki.2 only.** |
| `style(Style)` | `style` | **Tsubaki.2 only.** |
| `addLora(String)` / `addLora(Lora)` / `loras(List<Lora>)` | `loras` | Up to 5. |
| `seed(long)` | `seed` | Reproducibility (random when unset). |
| `sampling(Sampling)` | `sampling` | **SDXL / SD v1 only.** |
| `promptHelper(boolean)` | `promptHelper` | `true`→`enable`, `false`→`disable` (default enable). |
| `callbackUrl(String)` | `callbackUrl` | Status-change webhook. |
| `build()` | | Validates prompt, LoRA count and model compatibility. |

## `Task`

| Method | Returns | Description |
|--------|---------|-------------|
| `getId()` | `String` | Task id. |
| `getStatus()` | `TaskStatus` | Parsed status. |
| `getRawStatus()` | `String` | Raw status string from the API. |
| `getCreatedAt()` / `getUpdatedAt()` / `getStartedAt()` / `getEndAt()` | `Instant` | Timestamps (nullable). |
| `images()` | `List<GeneratedImage>` | Generated images (empty until completed; unmodifiable). |

## `GeneratedImage`

| Method | Returns | Description |
|--------|---------|-------------|
| `getMediaId()` | `String` | Media id (nullable). |
| `getUrl()` | `String` | Download URL. |
| `getBytes()` | `byte[]` | Downloads once, then caches. |
| `saveTo(Path dir)` | `Path` | Saves as `picture_PixAI_<timestamp>.png`. |
| `saveTo(Path dir, String fileName)` | `Path` | Saves under a custom name. |

## Flexible objects: `Lora`, `Style`, `Sampling`

Their full sub-schemas aren't published, so they're flexible key/value wrappers.

| Type | Factories / builder |
|------|---------------------|
| `Lora` | `Lora.of(String versionId)`, `Lora.of(String versionId, double weight)`, `Lora.raw(Map)`, `Lora.builder().set(k, v).build()` |
| `Style` | `Style.of(Map)`, `Style.builder().set(k, v).build()` |
| `Sampling` | `Sampling.of(Map)`, `Sampling.builder().set(k, v).method(SamplingMethod).build()` |

> Field-name assumptions to revisit once the Models / Supported Resolutions sub-schemas are
> available: `Lora` emits `modelVersionId`/`weight`; `Sampling.method(...)` writes the
> `samplingMethod` key.

## `PixAIException`

Unchecked (`extends RuntimeException`). Wraps all network failures, non-2xx responses,
unparseable bodies, timeouts and failed/cancelled tasks.

---

# Enums & values

### `Model`

| Constant | `versionId()` | `type()` |
|----------|---------------|----------|
| `TSUBAKI_2` *(= `Model.DEFAULT`)* | `1983308862240288769` | `DIT` |
| `HARUKA_V2` | `1861558740588989558` | `SDXL` |
| `HOSHINO_V2` | `1954632828118619567` | `SDXL` |

### `ModelType`

`SDXL` · `DIT` · `SD_V1`

### `AspectRatio` — `value()`, `width(Size)`, `height(Size)`, `fromValue(String)`

| Constant | `value()` | 1k (W×H) | 1.5k (W×H) |
|----------|-----------|----------|------------|
| `RATIO_1_1` | `1:1` | 1024×1024 | 1536×1536 |
| `RATIO_2_3` | `2:3` | 848×1280 | 1024×1536 |
| `RATIO_3_2` | `3:2` | 1280×848 | 1536×1024 |
| `RATIO_3_4` | `3:4` | 864×1152 | 1152×1536 |
| `RATIO_4_3` | `4:3` | 1152×864 | 1536×1152 |
| `RATIO_3_5` | `3:5` | 768×1280 | 912×1536 |
| `RATIO_5_3` | `5:3` | 1280×768 | 1536×912 |
| `RATIO_9_16` | `9:16` | 720×1280 | 864×1536 |
| `RATIO_16_9` | `16:9` | 1280×720 | 1536×864 |
| `RATIO_1_3` | `1:3` | 512×1536 | 512×1536 |
| `RATIO_3_1` | `3:1` | 1536×512 | 1536×512 |

### `Size` — `value()`, `fromValue(String)`

| Constant | `value()` | Resolution |
|----------|-----------|------------|
| `ONE_K` | `1k` | ~1 MP (1024×1024 at 1:1) |
| `ONE_AND_A_HALF_K` | `1.5k` | ~2.36 MP (long side ≤ 1536 px) |

### `Mode` (Tsubaki.2 only) — `value()`, `fromValue(String)`

| Constant | `value()` | Meaning |
|----------|-----------|---------|
| `LITE` | `lite` | Fastest, base quality. |
| `STANDARD` | `standard` | Balanced (default). |
| `PRO` | `pro` | Higher quality, slower. |
| `ULTRA` | `ultra` | Max quality, slowest. |

### `SamplingMethod` (SDXL / SD v1) — `value()`, `fromValue(String)`

`EULER_A` (`Euler a`) · `EULER` (`Euler`) · `LMS` · `HEUN` (`Heun`) · `DPM2_KARRAS` (`DPM2 Karras`)
· `DPM2_A_KARRAS` (`DPM2 a Karras`) · `DDIM` · `DPMPP_2M_KARRAS` (`DPM++ 2M Karras`)
· `DPMPP_2S_A_KARRAS` (`DPM++ 2S a Karras`) · `DPMPP_SDE_KARRAS` (`DPM++ SDE Karras`)
· `DPMPP_2M_SDE_KARRAS` (`DPM++ 2M SDE Karras`) · `RESTART` (`Restart`)

### `TaskStatus` — `isTerminal()`, `fromApi(String)`

| Constant | Terminal? | Maps from |
|----------|-----------|-----------|
| `PENDING` | no | `waiting`, `pending` |
| `PROCESSING` | no | `started`, `running`, `processing` |
| `COMPLETED` | yes | `completed`, `success` |
| `FAILED` | yes | `failed`, `error` |
| `CANCELLED` | yes | `cancelled`, `canceled` |
| `UNKNOWN` | no | anything unrecognised |

---

## Building from source

Requirements: JDK 17+ (a JDK 21/25 also works). Maven is **not** required — use the bundled
wrapper:

```bash
./mvnw clean verify   # compile + run the offline tests
./mvnw package        # build the jar (+ sources & javadoc jars) into target/
```

## ⚠️ Security

Do not hardcode or commit your PixAI API key. Pass it from an environment variable or a local
config file listed in `.gitignore`. If a key was ever committed, rotate it in your PixAI
account — deleting the file does not remove it from git history.

## License

[MIT](LICENSE) © Yelzhan Ibragimov (Arianwait)
