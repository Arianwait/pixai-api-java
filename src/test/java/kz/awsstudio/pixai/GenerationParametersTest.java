package kz.awsstudio.pixai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class GenerationParametersTest {

    @Test
    void includesOnlyTheFieldsThatWereSet() {
        GenerationParameters params = GenerationParameters.builder()
                .modelVersionId("ver-1")
                .prompt("a fox in a forest")
                .negativePrompt("low quality")
                .aspectRatio(AspectRatio.RATIO_16_9)
                .size(Size.ONE_K)
                .seed(42L)
                .promptHelper(true)
                .build();

        JSONObject json = params.toJson();

        assertEquals("ver-1", json.getString("modelVersionId"));
        assertEquals("a fox in a forest", json.getString("prompt"));
        assertEquals("low quality", json.getString("negativePrompt"));
        assertEquals("16:9", json.getString("aspectRatio"));
        assertEquals("1k", json.getString("size"));
        assertEquals(42L, json.getLong("seed"));
        assertEquals("enable", json.getString("promptHelper"));

        // unset fields are omitted
        assertFalse(json.has("modelId"));
        assertFalse(json.has("mode"));
        assertFalse(json.has("style"));
        assertFalse(json.has("sampling"));
        assertFalse(json.has("loras"));
        assertFalse(json.has("callbackUrl"));
    }

    @Test
    void promptHelperDisableSerialisesToDisable() {
        JSONObject json = GenerationParameters.builder()
                .modelVersionId("v").prompt("p").promptHelper(false).build()
                .toJson();
        assertEquals("disable", json.getString("promptHelper"));
    }

    @Test
    void serialisesLoraArray() {
        JSONObject json = GenerationParameters.builder()
                .modelVersionId("v").prompt("p")
                .addLora("lora-1")
                .addLora(Lora.of("lora-2", 0.7))
                .build()
                .toJson();

        JSONArray loras = json.getJSONArray("loras");
        assertEquals(2, loras.length());
        assertEquals("lora-1", loras.getJSONObject(0).getString("modelVersionId"));
        assertEquals("lora-2", loras.getJSONObject(1).getString("modelVersionId"));
        assertEquals(0.7, loras.getJSONObject(1).getDouble("weight"));
    }

    @Test
    void serialisesNestedStyleAndSampling() {
        JSONObject json = GenerationParameters.builder()
                .modelVersionId("v").prompt("p")
                .style(Style.builder().set("name", "anime").build())
                .sampling(Sampling.builder().set("steps", 25).build())
                .mode(Mode.PRO)
                .build()
                .toJson();

        assertEquals("anime", json.getJSONObject("style").getString("name"));
        assertEquals(25, json.getJSONObject("sampling").getInt("steps"));
        assertEquals("pro", json.getString("mode"));
    }

    @Test
    void requiresPromptOnly() {
        // prompt is required...
        assertThrows(IllegalStateException.class,
                () -> GenerationParameters.builder().modelVersionId("v").build());
        // ...but the model is optional and defaults to Tsubaki.2
        JSONObject json = GenerationParameters.builder().prompt("p").build().toJson();
        assertEquals(Model.TSUBAKI_2.versionId(), json.getString("modelVersionId"));
    }

    @Test
    void modelSelectionSetsVersionId() {
        JSONObject json = GenerationParameters.builder()
                .model(Model.HARUKA_V2).prompt("p").build().toJson();
        assertEquals(Model.HARUKA_V2.versionId(), json.getString("modelVersionId"));
    }

    @Test
    void validatesModelDependentFields() {
        // sampling is SDXL/SD-v1 only -> illegal on the default (Tsubaki.2 / DiT)
        assertThrows(IllegalStateException.class, () -> GenerationParameters.builder()
                .prompt("p").sampling(Sampling.builder().set("steps", 20).build()).build());
        // mode is Tsubaki.2 only -> illegal on an SDXL model
        assertThrows(IllegalStateException.class, () -> GenerationParameters.builder()
                .model(Model.HARUKA_V2).prompt("p").mode(Mode.PRO).build());
        // valid combos build fine
        GenerationParameters.builder().prompt("p").mode(Mode.ULTRA).build();
        GenerationParameters.builder().model(Model.HARUKA_V2).prompt("p")
                .sampling(Sampling.builder().method(SamplingMethod.EULER_A).build()).build();
    }

    @Test
    void aspectRatioResolvesPixelDimensions() {
        assertEquals(1280, AspectRatio.RATIO_16_9.width(Size.ONE_K));
        assertEquals(720, AspectRatio.RATIO_16_9.height(Size.ONE_K));
        assertEquals(1536, AspectRatio.RATIO_16_9.width(Size.ONE_AND_A_HALF_K));
        assertEquals(864, AspectRatio.RATIO_16_9.height(Size.ONE_AND_A_HALF_K));
    }

    @Test
    void rejectsMoreThanFiveLoras() {
        GenerationParameters.Builder b = GenerationParameters.builder()
                .modelVersionId("v").prompt("p");
        for (int i = 0; i < 6; i++) {
            b.addLora("lora-" + i);
        }
        assertThrows(IllegalStateException.class, b::build);
    }
}
