package com.mediafire.sdk.uploading;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mediafire.sdk.api.clients.UploadClient;
import com.mediafire.sdk.config.HttpHandler;
import com.mediafire.sdk.config.TokenManager;
import com.mediafire.sdk.http.Result;

import java.util.List;
import java.util.Map;

/**
 * Created by Chris on 12/22/2014.
 */
abstract class UploadRunnable implements Runnable {

    private final UploadClient mUploadClient;
    private boolean mDebug;

    public UploadRunnable(HttpHandler http, TokenManager tokenManager) {
        mUploadClient = new UploadClient(http, tokenManager);
    }

    public final UploadClient getUploadClient() {
        return mUploadClient;
    }

    public final String getResponseStringForGson(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(response);
        if (element.isJsonObject()) {
            JsonObject jsonResponse = element.getAsJsonObject().get("response").getAsJsonObject();
            return jsonResponse.toString();
        } else {
            return null;
        }
    }

    public boolean resultValid(Result result) {
        if (result == null){
            return false;
        }

        if (result.getResponse() == null) {
            return false;
        }

        if (result.getResponse().getBytes() == null) {
            return false;
        }

        if (result.getResponse().getHeaderFields() == null) {
            return false;
        }

        if (!result.getResponse().getHeaderFields().containsKey("Content-Type")) {
            return false;
        }

        List<String> contentTypeHeaders = result.getResponse().getHeaderFields().get("Content-Type");

        if (!contentTypeHeaders.contains("application/json")) {
            return false;
        }

        return true;
    }

    abstract Map<String, Object> makeQueryParams() throws Exception;

    @Override
    public abstract void run();

    public void debug(boolean debug) {
        mDebug = debug;
    }

    public boolean isDebugging() {
        return mDebug;
    }
}
