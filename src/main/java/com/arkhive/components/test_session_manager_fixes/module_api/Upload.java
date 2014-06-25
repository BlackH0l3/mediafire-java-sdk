package com.arkhive.components.test_session_manager_fixes.module_api;

import com.arkhive.components.test_session_manager_fixes.module_api.responses.*;
import com.arkhive.components.test_session_manager_fixes.module_api_descriptor.ApiRequestObject;
import com.arkhive.components.test_session_manager_fixes.module_api_descriptor.interfaces.ApiRequestRunnableCallback;
import com.arkhive.components.test_session_manager_fixes.module_api_descriptor.requests.*;
import com.google.gson.Gson;

import java.util.Map;

/**
 * Created by Chris Najar on 6/18/2014.
 */
public class Upload {
    public UploadCheckResponse checkUpload(
            Map<String, String> requiredParameters,
            Map<String, String> optionalParameters) {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_UPLOAD_CHECK);
        apiRequestObject.setOptionalParameters(optionalParameters);
        apiRequestObject.setRequiredParameters(requiredParameters);
        BlockingApiGetRequest apiGetRequestRunnable = Api.createBlockingApiGetRequest(apiRequestObject);
        apiGetRequestRunnable.sendRequest();
        String response = apiRequestObject.getHttpResponseString();
        return new Gson().fromJson(Api.getResponseString(response), UploadCheckResponse.class);
    }

    public Runnable checkUpload(
            ApiRequestRunnableCallback callback,
            Map<String, String> requiredParameters,
            Map<String, String> optionalParameters) {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_UPLOAD_CHECK);
        apiRequestObject.setOptionalParameters(optionalParameters);
        apiRequestObject.setRequiredParameters(requiredParameters);
        RunnableApiGetRequest runnableApiGetRequest = Api.createApiGetRequestRunnable(UploadCheckResponse.class, callback, apiRequestObject);
        return runnableApiGetRequest;
    }

    public UploadInstantResponse instantUpload(
            Map<String, String> requiredParameters,
            Map<String, String> optionalParameters) {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_UPLOAD_INSTANT);
        apiRequestObject.setOptionalParameters(optionalParameters);
        apiRequestObject.setRequiredParameters(requiredParameters);
        BlockingApiGetRequestUploadToken apiGetRequestRunnable = Api.createBlockingApiGetRequestUploadToken(apiRequestObject);
        apiGetRequestRunnable.sendRequest();
        String response = apiRequestObject.getHttpResponseString();
        return new Gson().fromJson(Api.getResponseString(response), UploadInstantResponse.class);
    }

    public Runnable instantUpload(
            ApiRequestRunnableCallback callback,
            Map<String, String> requiredParameters,
            Map<String, String> optionalParameters) {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_UPLOAD_INSTANT);
        apiRequestObject.setOptionalParameters(optionalParameters);
        apiRequestObject.setRequiredParameters(requiredParameters);
        RunnableApiGetRequestUploadToken runnableApiGetRequest = Api.createApiGetRequestRunnableUploadToken(UploadInstantResponse.class, callback, apiRequestObject);
        return runnableApiGetRequest;
    }

    public Runnable resumableUpload(
            ApiRequestRunnableCallback callback,
            Map<String, String> requiredParameters,
            Map<String, String> optionalParameters) {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_UPLOAD_RESUMABLE);
        apiRequestObject.setOptionalParameters(optionalParameters);
        apiRequestObject.setRequiredParameters(requiredParameters);
        RunnableApiPostRequestUploadToken runnableApiGetRequest = Api.createApiPostRequestRunnableUploadToken(UploadResumableResponse.class, callback, apiRequestObject);
        return runnableApiGetRequest;
    }

    public UploadResumableResponse resumableUpload(
            Map<String, String> requiredParameters,
            Map<String, String> optionalParameters,
            Map<String, String> headers,
            byte[] payload) {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_UPLOAD_RESUMABLE);
        apiRequestObject.setOptionalParameters(optionalParameters);
        apiRequestObject.setRequiredParameters(requiredParameters);
        apiRequestObject.setPostHeaders(headers);
        apiRequestObject.setPayload(payload);
        BlockingApiPostRequestUploadToken apiGetRequestRunnable = Api.createBlockingApiPostRequestUploadToken(apiRequestObject);
        apiGetRequestRunnable.sendRequest();
        String response = apiRequestObject.getHttpResponseString();
        return new Gson().fromJson(Api.getResponseString(response), UploadResumableResponse.class);
    }

    public UploadPollResponse pollUpload(
            Map<String, String> requiredParameters,
            Map<String, String> optionalParameters) {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_UPLOAD_POLL_UPLOAD);
        apiRequestObject.setOptionalParameters(optionalParameters);
        apiRequestObject.setRequiredParameters(requiredParameters);
        BlockingApiGetRequest apiGetRequestRunnable = Api.createBlockingApiGetRequest(apiRequestObject);
        apiGetRequestRunnable.sendRequest();
        String response = apiRequestObject.getHttpResponseString();
        return new Gson().fromJson(Api.getResponseString(response), UploadPollResponse.class);
    }

    public Runnable pollUpload(
            ApiRequestRunnableCallback callback,
            Map<String, String> requiredParameters,
            Map<String, String> optionalParameters) {
        ApiRequestObject apiRequestObject = new ApiRequestObject(ApiUris.LIVE_HTTP, ApiUris.URI_UPLOAD_POLL_UPLOAD);
        apiRequestObject.setOptionalParameters(optionalParameters);
        apiRequestObject.setRequiredParameters(requiredParameters);
        RunnableApiGetRequest runnableApiGetRequest = Api.createApiGetRequestRunnable(UploadPollResponse.class, callback, apiRequestObject);
        return runnableApiGetRequest;
    }
}