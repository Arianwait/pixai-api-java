package kz.awsstudio.pixai.internal;

import java.io.IOException;

import org.json.JSONArray;
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
 * Thin transport layer over the PixAI GraphQL endpoint.
 *
 * <p>Centralises request building, authentication, resource management
 * (try-with-resources on every {@link Response}) and error handling, including
 * inspection of the GraphQL {@code errors} field which the original client ignored.
 *
 * <p>This is an internal class and is not part of the public API.
 */
public final class GraphQLClient {

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String endpoint;

    public GraphQLClient(OkHttpClient httpClient, String apiKey, String endpoint) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    /**
     * Executes a GraphQL query/mutation and returns its {@code data} object.
     *
     * @param query     the GraphQL document.
     * @param variables the variables for the document (may be empty, not null).
     * @return the {@code data} object from the response.
     * @throws PixAIException on transport failure, a non-2xx response, a GraphQL
     *                        {@code errors} payload, or a missing/invalid {@code data} field.
     */
    public JSONObject query(String query, JSONObject variables) {
        JSONObject body = new JSONObject();
        body.put("query", query);
        body.put("variables", variables);

        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String payload = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new PixAIException("PixAI API request failed with HTTP "
                        + response.code() + ": " + payload);
            }
            return extractData(payload);
        } catch (IOException e) {
            throw new PixAIException("Network error while calling the PixAI API", e);
        }
    }

    /**
     * Downloads the raw bytes of a media URL, authenticating with the API key.
     *
     * @param url the URL to download.
     * @return the downloaded bytes.
     * @throws PixAIException on transport failure or a non-2xx response.
     */
    public byte[] download(String url) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new PixAIException("Failed to download media: HTTP " + response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new PixAIException("Failed to download media: empty response body");
            }
            return responseBody.bytes();
        } catch (IOException e) {
            throw new PixAIException("Network error while downloading media", e);
        }
    }

    private JSONObject extractData(String payload) {
        JSONObject json;
        try {
            json = new JSONObject(payload);
        } catch (JSONException e) {
            throw new PixAIException("Could not parse PixAI API response: " + payload, e);
        }

        JSONArray errors = json.optJSONArray("errors");
        if (errors != null && errors.length() > 0) {
            throw new PixAIException("PixAI API returned errors: " + errors);
        }

        JSONObject data = json.optJSONObject("data");
        if (data == null) {
            throw new PixAIException("PixAI API response had no 'data' field: " + payload);
        }
        return data;
    }
}
