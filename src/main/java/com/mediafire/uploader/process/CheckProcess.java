package com.mediafire.uploader.process;

import com.mediafire.sdk.api_responses.ApiResponse;
import com.mediafire.sdk.api_responses.upload.CheckResponse;
import com.mediafire.sdk.config.MFConfiguration;
import com.mediafire.sdk.http.MFApi;
import com.mediafire.sdk.http.MFHost;
import com.mediafire.sdk.http.MFRequest;
import com.mediafire.sdk.http.MFResponse;
import com.mediafire.sdk.token.MFTokenFarm;
import com.mediafire.uploader.manager.UploadManager;
import com.mediafire.uploader.uploaditem.ResumableBitmap;
import com.mediafire.uploader.uploaditem.UploadItem;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckProcess extends UploadProcess {

    private static final String TAG = CheckProcess.class.getCanonicalName();

    public CheckProcess(MFTokenFarm mfTokenFarm, UploadManager uploadManager, UploadItem uploadItem) {
        super(mfTokenFarm, uploadItem, uploadManager);
    }

    @Override
    protected void doUploadProcess() {
        MFConfiguration.getStaticMFLogger().v(TAG, "doUploadProcess()");
        uploadItem.getFileData().setFileSize();
        uploadItem.getFileData().setFileHash();
        //notify listeners that check started
        notifyListenerUploadStarted();

        // url encode the filename
        String filename;
        try {
            filename = URLEncoder.encode(uploadItem.getFileName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            MFConfiguration.getStaticMFLogger().v(TAG, "Exception: " + e);
            notifyListenerException(e);
            return;
        }

        // generate map with request parameters
        Map<String, String> keyValue = generateRequestParameters(filename);
        MFRequest.MFRequestBuilder mfRequestBuilder = new MFRequest.MFRequestBuilder(MFHost.LIVE_HTTP, MFApi.UPLOAD_CHECK);
        mfRequestBuilder.requestParameters(keyValue);
        MFRequest mfRequest = mfRequestBuilder.build();
        MFResponse mfResponse = mfTokenFarm.getMFHttpRunner().doRequest(mfRequest);
        CheckResponse response = mfResponse.getResponseObject(CheckResponse.class);

        if (response == null) {
            notifyListenerLostConnection();
            return;
        }

        // if there is an error code, cancel the upload
        if (response.getErrorCode() != ApiResponse.ResponseCode.NO_ERROR) {
            notifyListenerCancelled(response);
            return;
        }

        uploadItem.getChunkData().setNumberOfUnits(response.getResumableUpload().getNumberOfUnits());
        uploadItem.getChunkData().setUnitSize(response.getResumableUpload().getUnitSize());
        int count = response.getResumableUpload().getBitmap().getCount();
        List<Integer> words = response.getResumableUpload().getBitmap().getWords();
        ResumableBitmap bitmap = new ResumableBitmap(count, words);
        uploadItem.setBitmap(bitmap);
        MFConfiguration.getStaticMFLogger().v(TAG, uploadItem.getFileData().getFilePath() + " upload item bitmap: " + uploadItem.getBitmap().getCount() + " count, " + uploadItem.getBitmap().getWords().toString() + " words.");

        // notify listeners that check has completed
        notifyListenerCompleted(response);
    }

    /**
     * generates the request parameter after we receive a UTF encoded filename.
     *
     * @param filename - the name of hte file.
     * @return - a map of request parameters.
     */
    private Map<String, String> generateRequestParameters(String filename) {
        // generate map with request parameters
        Map<String, String> keyValue = new HashMap<String, String>();
        keyValue.put("filename", filename);
        keyValue.put("hash", uploadItem.getFileData().getFileHash());
        keyValue.put("size", Long.toString(uploadItem.getFileData().getFileSize()));
        keyValue.put("resumable", uploadItem.getUploadOptions().getResumable());
        keyValue.put("response_format", "json");
        if (!uploadItem.getUploadOptions().getUploadPath().isEmpty()) {
            keyValue.put("path", uploadItem.getUploadOptions().getUploadPath());
        } else {
            keyValue.put("folder_key", uploadItem.getUploadOptions().getUploadFolderKey());
        }
        return keyValue;
    }
}
