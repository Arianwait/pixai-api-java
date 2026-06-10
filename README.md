# pixai-api-java

A minimal, single-class Java client for the [PixAI](https://pixai.art) image-generation
API. Given a prompt, it starts a generation task, waits for completion, and downloads the
resulting image. This is a streamlined version of the PixAI integration with a clean,
documented `PixAIClient` class.

> **Note on the name:** despite "node" in the repository name, this project is pure Java
> and has no Node.js dependency. Consider renaming the repo (e.g. `pixai-api-java`) to
> avoid confusion.

## Features

- Single `PixAIClient` class — construct it with your API key and go.
- Generate an image from a text prompt.
- Configurable generation parameters (model, size, sampler, steps, etc.).
- Automatic download of the generated image, saved with a timestamped filename.

## Tech stack

Java · OkHttp · `org.json` · Maven

## Getting started

**Requirements:** JDK 8+ and Maven.

```bash
mvn clean compile
```

Basic usage:

```java
PixAIClient client = new PixAIClient("YOUR_PIXAI_API_KEY");
client.generateImage("a fox in a forest, digital art");
```

See `example/.../PixAIExample.java` for a runnable sample.

## ⚠️ Security

Do not hardcode or commit your PixAI API key. Pass it from an environment variable or a
local config file that is listed in `.gitignore`. If a key was ever committed, rotate it
in your PixAI account — deleting the file does not remove it from git history.
