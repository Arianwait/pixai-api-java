import java.nio.file.Path;
import java.nio.file.Paths;

import kz.arianwaitstudio.pixai.AspectRatio;
import kz.arianwaitstudio.pixai.GeneratedImage;
import kz.arianwaitstudio.pixai.GenerationParameters;
import kz.arianwaitstudio.pixai.Model;
import kz.arianwaitstudio.pixai.PixAIClient;
import kz.arianwaitstudio.pixai.Size;
import kz.arianwaitstudio.pixai.Task;

/**
 * Runnable sample for the pixai-api-java library (PixAI API v2).
 *
 * <p>This file is documentation only and is intentionally NOT part of the Maven build. To run it,
 * build/install the library first ({@code ./mvnw install}) and compile this file against the
 * produced jar, or paste the body into your own project.
 *
 * <p>The API key is read from the {@code PIXAI_API_KEY} environment variable so it is never
 * hardcoded or committed.
 */
public class PixAIExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("PIXAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Set the PIXAI_API_KEY environment variable first.");
            return;
        }

        // ---- Simplest path: 4 steps (client, key, prompt, run) ----
        PixAIClient client = new PixAIClient(apiKey);            // 1) client + 2) key
        GenerationParameters minimal = GenerationParameters.builder()
                .prompt("a fox in a forest, digital art")        // 3) prompt (required)
                .build();
        Task task = client.generate(minimal);                    // 4) run
        for (GeneratedImage image : task.images()) {
            Path saved = image.saveTo(Paths.get("out"));
            System.out.println("Image saved to: " + saved);
        }

        // ---- Optional: full configuration ----
        GenerationParameters params = GenerationParameters.builder()
                .model(Model.HARUKA_V2)                  // or .modelVersionId("...") for a custom model
                .prompt("a fox in a forest, digital art")
                .negativePrompt("low quality, blurry")
                .aspectRatio(AspectRatio.RATIO_9_16)
                .size(Size.ONE_K)
                .seed(42L)
                .build();

        Task configured = client.generate(params); // blocks until completion
        configured.images().forEach(img -> System.out.println(img.getUrl()));
    }
}
