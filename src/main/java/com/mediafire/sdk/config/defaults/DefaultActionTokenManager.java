package com.mediafire.sdk.config.defaults;

import com.mediafire.sdk.clients.ApiClient;
import com.mediafire.sdk.config.ActionTokenManagerInterface;
import com.mediafire.sdk.config.Configuration;
import com.mediafire.sdk.http.*;
import com.mediafire.sdk.token.ImageActionToken;
import com.mediafire.sdk.token.UploadActionToken;

/**
 * Created by Jonathan Harrison on 10/21/2014.
 */
public class DefaultActionTokenManager implements ActionTokenManagerInterface {
    private static final String TAG = DefaultActionTokenManager.class.getCanonicalName();
    private ApiClient mApiClient;

    private ImageActionToken mImageActionToken;
    private UploadActionToken mUploadActionToken;

    private static final Object mImageActionTokenLock = new Object();
    private static final Object mUploadActionTokenLock = new Object();
    private Configuration mConfiguration;

    public DefaultActionTokenManager() { }

    @Override
    public void initialize(Configuration configuration) {
        DefaultLogger.log().v(TAG, "initialize");
        mConfiguration = configuration;
        mApiClient = new ApiClient(configuration);
    }

    @Override
    public void shutdown() {
        DefaultLogger.log().v(TAG, "shutdown");
    }

    @Override
    public void receiveImageActionToken(ImageActionToken token) {
        DefaultLogger.log().v(TAG, "receiveImageActionToken");
        synchronized (mImageActionTokenLock) {
            mImageActionToken = token;
            mImageActionTokenLock.notifyAll();
        }
    }

    @Override
    public void receiveUploadActionToken(UploadActionToken token) {
        DefaultLogger.log().v(TAG, "receiveUploadActionToken");
        synchronized (mUploadActionTokenLock) {
            mUploadActionToken = token;
            mUploadActionTokenLock.notifyAll();
        }
    }

    @Override
    public ImageActionToken borrowImageActionToken() {
        DefaultLogger.log().v(TAG, "borrowImageActionToken");
        synchronized (mImageActionTokenLock) {
            if (mImageActionToken == null) {
                new NewImageActionTokenThread().start();
            }

            if (mImageActionToken != null) {
                return mImageActionToken;
            }

            try {
                mImageActionTokenLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return mImageActionToken;
        }
    }

    private class NewImageActionTokenThread extends Thread {
        @Override
        public void run(){
            DefaultLogger.log().v(TAG, "NewImageActionTokenThread - run");
            HostObject hostObject = new HostObject("http", "www", "mediafire.com", "post");
            ApiObject apiObject = new ApiObject("user", "get_action_token.php");
            InstructionsObject instructionsObject = new InstructionsObject(BorrowTokenType.V2, SignatureType.API_REQUEST, ReturnTokenType.NEW_IMAGE, true);
            VersionObject versionObject = new VersionObject(null);
            Request request = new Request(hostObject, apiObject, instructionsObject, versionObject);
            request.addQueryParameter("type", "image");
            request.addQueryParameter("response_format", "json");

            Result result = mApiClient.doRequest(request);
            Response response = result.getResponse();
            if(response.getClass() == ResponseApiClientError.class) {
                ResponseApiClientError responseApiClientError = (ResponseApiClientError) result.getResponse();
                DefaultLogger.log().e(TAG, responseApiClientError.getErrorMessage());
                receiveUploadActionToken(null);
            }
        }
    }

    @Override
    public UploadActionToken borrowUploadActionToken() {
        DefaultLogger.log().v(TAG, "borrowUploadActionToken");
        synchronized (mUploadActionTokenLock) {
            if (mUploadActionToken == null) {
                new NewUploadActionTokenThread().start();
            }

            if (mUploadActionToken != null) {
                return mUploadActionToken;
            }

            try {
                mUploadActionTokenLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return mUploadActionToken;
        }
    }

    private class NewUploadActionTokenThread extends Thread {
        @Override
        public void run(){
            DefaultLogger.log().v(TAG, "NewUploadActionTokenThread - run");
            HostObject hostObject = new HostObject("http", "www", "mediafire.com", "post");
            ApiObject apiObject = new ApiObject("user", "get_action_token.php");
            InstructionsObject instructionsObject = new InstructionsObject(BorrowTokenType.V2, SignatureType.API_REQUEST, ReturnTokenType.NEW_UPLOAD, true);
            VersionObject versionObject = new VersionObject(null);
            Request request = new Request(hostObject, apiObject, instructionsObject, versionObject);
            request.addQueryParameter("type", "upload");
            request.addQueryParameter("response_format", "json");

            Result result = mApiClient.doRequest(request);
            Response response = result.getResponse();
            if(response.getClass() == ResponseApiClientError.class) {
                ResponseApiClientError responseApiClientError = (ResponseApiClientError) result.getResponse();
                DefaultLogger.log().e(TAG, responseApiClientError.getErrorMessage());
                receiveUploadActionToken(null);
            }
        }
    }

    @Override
    public void tokensFailed() {
        DefaultLogger.log().v(TAG, "tokensFailed");
        synchronized (mUploadActionTokenLock) {
            mUploadActionToken = null;
        }
        synchronized (mImageActionTokenLock) {
            mImageActionToken = null;
        }

        borrowImageActionToken();
        borrowUploadActionToken();
    }
}