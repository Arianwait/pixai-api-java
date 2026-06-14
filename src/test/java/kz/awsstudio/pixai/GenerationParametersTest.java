package kz.awsstudio.pixai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class GenerationParametersTest {

    @Test
    void includesOnlyTheFieldsThatWereSet() {
        GenerationParameters params = GenerationParameters.builder()
                .prompts("a fox in a forest")
                .negativePrompt("low quality")
                .modelId("123")
                .width(768)
                .height(1280)
                .samplingSteps(50)
                .build();

        JSONObject json = params.toJson();

        // set fields are present with the exact API key names
        assertEquals("a fox in a forest", json.getString("prompts"));
        assertEquals("low quality", json.getString("negative_prompt")); // snake_case
        assertEquals("123", json.getString("modelId"));
        assertEquals(768, json.getInt("width"));
        assertEquals(1280, json.getInt("height"));
        assertEquals(50, json.getInt("samplingSteps"));

        // unset fields are omitted
        assertFalse(json.has("cfgScale"));
        assertFalse(json.has("upscale"));
        assertFalse(json.has("sampler"));
        assertFalse(json.has("enableTile"));
    }

    @Test
    void serialisesNumericAndBooleanTypes() {
        JSONObject json = GenerationParameters.builder()
                .prompts("p")
                .cfgScale(7.5)
                .upscale(2.0)
                .enableTile(true)
                .sampler("Euler a")
                .build()
                .toJson();

        assertEquals(7.5, json.getDouble("cfgScale"));
        assertEquals(2.0, json.getDouble("upscale"));
        assertTrue(json.getBoolean("enableTile"));
        assertEquals("Euler a", json.getString("sampler"));
    }

    @Test
    void requiresPrompts() {
        assertThrows(IllegalStateException.class,
                () -> GenerationParameters.builder().build());
        assertThrows(IllegalStateException.class,
                () -> GenerationParameters.builder().prompts("   ").build());
    }
}
