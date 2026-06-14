import java.nio.file.Path;
import java.nio.file.Paths;

import kz.awsstudio.pixai.GeneratedImage;
import kz.awsstudio.pixai.GenerationParameters;
import kz.awsstudio.pixai.PixAIClient;

/**
 * Runnable sample for the pixai-api-java library.
 *
 * <p>This file is documentation only and is intentionally NOT part of the Maven
 * build. To run it, build/install the library first ({@code ./mvnw install}) and
 * compile this file against the produced jar, or paste the body into your own project.
 *
 * <p>The API key is read from the {@code PIXAI_API_KEY} environment variable so it is
 * never hardcoded or committed.
 */
public class PixAIExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("PIXAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Set the PIXAI_API_KEY environment variable first.");
            return;
        }

        PixAIClient client = PixAIClient.builder()
                .apiKey(apiKey)
                .build();

        GenerationParameters params = GenerationParameters.builder()
                .prompts("a fox in a forest, digital art")
                .negativePrompt("low quality, blurry")
                .modelId("1648918127446573124")
                .samplingSteps(50)
                .width(768)
                .height(1280)
                .build();

        GeneratedImage image = client.generate(params); // blocks until completion
        Path saved = image.saveTo(Paths.get("out"));
        System.out.println("Image saved to: " + saved);
    }
}
