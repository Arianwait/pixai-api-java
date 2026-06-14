package kz.awsstudio.pixai.internal;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import kz.awsstudio.pixai.PixAIException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Thin transport layer over the PixAI REST API (v2).
 *
 * <p>Centralises request building, bearer authentication, resource management
 * (try-with-resources on every {@link Response}) and error handling. This is an internal
 * class and is not part of the public API.
 */
public final class RestClient {

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    public RestClient(OkHttpClient httpClient, String apiKey, String baseUrl) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    /**
     * Performs a {@code POST} with a JSON body and returns the parsed JSON response.
     *
     * @param path the path appended to the base URL (e.g. {@code "/v2/image/create"}).
     * @param body the JSON request body.
     * @return the parsed response object.
     * @throws PixAIException on transport failure, a non-2xx response, or an unparseable body.
     */
    public JSONObject post(String path, JSONObject body) {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .build();
        return executeJson(request);
    }

    /**
     * Performs a {@code GET} and returns the parsed JSON response.
     *
     * @param path the path appended to the base URL (e.g. {@code "/v2/image/abc"}).
     * @return the parsed response object.
     * @throws PixAIException on transport failure, a non-2xx response, or an unparseable body.
     */
    public JSONObject get(String path) {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .build();
        return executeJson(request);
    }

    /**
     * Downloads the raw bytes of an (absolute) media URL, authenticating with the API key.
     *
     * @param url the absolute URL to download.
     * @return the downloaded bytes.
     * @throws PixAIException on transport failure or a non-2xx response.
     */
    public byte[] download(String url) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new PixAIException("Failed to download media: HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new PixAIException("Failed to download media: empty response body");
            }
            return body.bytes();
        } catch (IOException e) {
            throw new PixAIException("Network error while downloading media", e);
        }
    }

    private JSONObject executeJson(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            String payload = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new PixAIException("PixAI API request failed with HTTP "
                        + response.code() + ": " + payload);
            }
            try {
                return new JSONObject(payload);
            } catch (JSONException e) {
                throw new PixAIException("Could not parse PixAI API response: " + payload, e);
            }
        } catch (IOException e) {
            throw new PixAIException("Network error while calling the PixAI API", e);
        }
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
