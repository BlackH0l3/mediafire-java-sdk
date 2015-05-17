package com.mediafire.sdk.requests;

import com.mediafire.sdk.util.RequestUtil;

import java.util.HashMap;
import java.util.Map;

public class PostRequest extends ApiPostRequest {
    private static final String CHARSET = "UTF-8";
    private final String url;
    private final Map<String, Object> headers = new HashMap<String, Object>();
    private final byte[] payload;


    public PostRequest(ApiPostRequest apiPostRequest) {
        super(apiPostRequest.getScheme(), apiPostRequest.getDomain(), apiPostRequest.getPath(), apiPostRequest.getQueryMap());
        this.url = RequestUtil.makeUrlFromApiRequest(apiPostRequest);
        this.payload = RequestUtil.makeQueryPayloadFromApiRequest(apiPostRequest);
        this.headers.put("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);
        this.headers.put("Content-Length", payload.length);
        this.headers.put("Accept-Charset", "UTF-8");
    }

    public PostRequest(UploadPostRequest uploadRequest, byte[] payload) {
        super(uploadRequest.getScheme(), uploadRequest.getDomain(), uploadRequest.getPath(), uploadRequest.getQueryMap());
        this.url = RequestUtil.makeUrlFromUploadRequest(uploadRequest);
        this.payload = payload;
        this.headers.put("Content-Type", "application/octet-stream");
        this.headers.put("Content-Length", payload.length);
        this.headers.put("Accept-Charset", "UTF-8");
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public byte[] getPayload() {
        return payload;
    }
}
