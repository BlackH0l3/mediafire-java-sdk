package com.mediafire.sdk.uploader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mediafire.sdk.clients.ClientHelperNoToken;
import com.mediafire.sdk.api_responses.ApiResponse;
import com.mediafire.sdk.api_responses.ResponseCode;
import com.mediafire.sdk.api_responses.upload.CheckResponse;
import com.mediafire.sdk.api_responses.upload.InstantResponse;
import com.mediafire.sdk.api_responses.upload.PollResponse;
import com.mediafire.sdk.api_responses.upload.ResumableResponse;
import com.mediafire.sdk.clients.ApiClient;
import com.mediafire.sdk.clients.ApiRequestGenerator;
import com.mediafire.sdk.clients.ClientHelperActionToken;
import com.mediafire.sdk.config.*;
import com.mediafire.sdk.http.Request;
import com.mediafire.sdk.http.Response;
import com.mediafire.sdk.http.Result;
import com.mediafire.sdk.uploader.uploaditem.*;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Chris Najar on 7/21/2014.
 * UploadRunnable uploads an UploadItem
 */
public class UploadRunnable implements Runnable {
    private static final String TAG = UploadRunnable.class.getCanonicalName();
    public static final String MSG_NO_NETWORK_CONNECTION = "no network connection";
    public static final String MSG_REQUIRED_PARAMETERS_NULL = "required parameters were null";
    public static final String MSG_REQUIRED_PARAMETERS_INVALID = "required parameters were invalid";
    public static final String MSG_STORAGE_LIMIT_EXCEEDED = "storage limit exceeded";
    public static final String MSG_CANCELLED_UPLOAD = "upload was cancelled";
    public static final String MSG_RESPONSE_ERROR = "response from server had error";
    private final int mMaxPolls;
    private final long mMillisecondsBetweenPolls;
    private final UploadItem mUploadItem;
    private final UploadListenerInterface mUploadListener;
    private final int mMaxUploadAttempts;
    private String mUtf8EncodedFileName;
    private final NetworkConnectivityMonitorInterface mNetworkConnectivityMonitor;
    private final CredentialsInterface mUserCredentials;
    private final ActionTokenManagerInterface mActionTokenManagerInterface;
    private HttpWorkerInterface mHttpWorker;

    private UploadRunnable(Builder builder) {
        mMaxPolls = builder.maxPolls;
        mMillisecondsBetweenPolls = builder.millisecondsBetweenPolls;
        mUploadItem = builder.uploadItem;
        mUploadListener = builder.mfUploadListener;
        mMaxUploadAttempts = builder.maxUploadAttempts;

        mNetworkConnectivityMonitor = builder.configuration.getNetworkConnectivityMonitor();
        mUserCredentials = builder.configuration.getUserCredentials();
        mActionTokenManagerInterface = builder.configuration.getActionTokenManager();
        mHttpWorker = builder.configuration.getHttpWorker();
    }

    /**
     * Starts the upload
     */
    @Override
    public void run() {
        System.out.printf("%s - %s", TAG, "run()");
        notifyUploadListenerStarted();
        if (!mNetworkConnectivityMonitor.haveNetworkConnection()) {
            notifyUploadListenerCancelled(MSG_NO_NETWORK_CONNECTION);
            return;
        }

        try {
            encodeFileNameUTF8();
            startOrRestartUpload();
        } catch (UnsupportedEncodingException e) {
            System.out.printf("%s - %s", TAG, "UnsupportedEncodingException during UploadRunnable", e);
            notifyUploadListenerCancelled(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.printf("%s - %s", TAG, "NoSuchAlgorithmException during UploadRunnable", e);
            notifyUploadListenerCancelled(e.getMessage());
        } catch (IOException e) {
            System.out.printf("%s - %s", TAG, "IOException during UploadRunnable", e);
            notifyUploadListenerCancelled(e.getMessage());
        }
    }

    private void encodeFileNameUTF8() throws UnsupportedEncodingException {
        System.out.printf("%s - %s", TAG, "encodeFileNameUTF8");
        mUtf8EncodedFileName = new String(mUploadItem.getFileName().getBytes("UTF-8"), "UTF-8");
    }

    private void checkUploadFinished(UploadItem uploadItem, CheckResponse checkResponse) throws NoSuchAlgorithmException, IOException {
        System.out.printf("%s - %s", TAG, "checkUploadFinished()");
        if (checkResponse == null) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        //as a preventable infinite loop measure, an upload item cannot continue after upload/check.php if it has gone through the process 20x
        //20x is high, but it should never happen and will allow for more information gathering.
        if (checkResponse.getStorageLimitExceeded()) {
            System.out.printf("%s - %s", TAG, "storage limit is exceeded");
            notifyUploadListenerCancelled(MSG_STORAGE_LIMIT_EXCEEDED);
        } else if (checkResponse.getResumableUpload().areAllUnitsReady() && !uploadItem.getPollUploadKey().isEmpty()) {
            System.out.printf("%s - %s", TAG, "all units are ready and poll upload key is not empty");
            // all units are ready and we have the poll upload key. start polling.
            doPollUpload();
        } else {
            if (checkResponse.doesHashExists()) { //hash does exist for the file
                hashExistsInCloud(checkResponse);
            } else {
                hashDoesNotExistInCloud(checkResponse);
            }
        }
    }

    private void hashExistsInCloud(CheckResponse checkResponse) {
        System.out.printf("%s - %s", TAG, "hash exists");
        if (!checkResponse.isInAccount()) { // hash which exists is not in the account
            hashExistsButDoesNotExistInAccount();
        } else { // hash exists and is in the account
            hashExistsInAccount(checkResponse);
        }
    }

    private void hashExistsInAccount(CheckResponse checkResponse) {
        System.out.printf("%s - %s", TAG, "hash is in account");
        boolean inFolder = checkResponse.isInFolder();
        System.out.printf("%s - %s", TAG, "ActionOnInAccount: " + mUploadItem.getUploadOptions().getActionOnInAccount());
        switch (mUploadItem.getUploadOptions().getActionOnInAccount()) {
            case UPLOAD_ALWAYS:
                System.out.printf("%s - %s", TAG, "uploading...");
                doInstantUpload();
                break;
            case UPLOAD_IF_NOT_IN_FOLDER:
                System.out.printf("%s - %s", TAG, "uploading if not in folder.");
                if (!inFolder) {
                    System.out.printf("%s - %s", TAG, "uploading...");
                    doInstantUpload();
                } else {
                    System.out.printf("%s - %s", TAG, "already in folder, not uploading...");
                    notifyUploadListenerCompleted(checkResponse.getDuplicateQuickkey());
                }
                break;
            case DO_NOT_UPLOAD:
            default:
                System.out.printf("%s - %s", TAG, "not uploading...");
                notifyUploadListenerCompleted(checkResponse.getDuplicateQuickkey());
                break;
        }
    }

    private void hashExistsButDoesNotExistInAccount() {
        System.out.printf("%s - %s", TAG, "hash is not in account");
        doInstantUpload();
    }

    private void hashDoesNotExistInCloud(CheckResponse checkResponse) throws IOException, NoSuchAlgorithmException {
        // hash does not exist. call resumable.
        System.out.printf("%s - %s", TAG, "hash does not exist");
        if (checkResponse.getResumableUpload().getUnitSize() == 0) {
            System.out.printf("%s - %s", TAG, "unit size received from unit_size was 0. cancelling");
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_INVALID);
            return;
        }

        if (checkResponse.getResumableUpload().getNumberOfUnits() == 0) {
            System.out.printf("%s - %s", TAG, "number of units received from number_of_units was 0. cancelling");
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_INVALID);
            return;
        }

        if (checkResponse.getResumableUpload().areAllUnitsReady() && !mUploadItem.getPollUploadKey().isEmpty()) {
            System.out.printf("%s - %s", TAG, "all units ready and have a poll upload key");
            // all units are ready and we have the poll upload key. start polling.
            doPollUpload();
        } else {
            System.out.printf("%s - %s", TAG, "all units not ready or do not have poll upload key");
            // either we don't have the poll upload key or all units are not ready
            doResumableUpload();
        }
    }

    private void instantUploadFinished(String quickKey) {
        System.out.printf("%s - %s", TAG, "instantUploadFinished()");
        notifyUploadListenerCompleted(quickKey);
    }

    private void resumableUploadFinished(ResumableResponse response) throws IOException, NoSuchAlgorithmException {
        System.out.printf("%s - %s", TAG, "resumableUploadFinished()");
        if (response != null && response.getResumableUpload().areAllUnitsReady() && !response.getDoUpload().getPollUploadKey().isEmpty()) {
            doPollUpload();
        } else {
            doCheckUpload();
        }
    }

    private void pollUploadFinished(PollResponse pollResponse) throws IOException, NoSuchAlgorithmException {
        System.out.printf("%s - %s", TAG, "pollUploadFinished()");
        // if this method is called then file error and result codes are fine, but we may not have received status 99 so
        // check status code and then possibly send item to the backlog queue.
        PollResponse.DoUpload doUpload = pollResponse.getDoUpload();
        PollResponse.Status pollStatusCode = doUpload.getStatusCode();
        PollResponse.Result pollResultCode = doUpload.getResultCode();
        PollResponse.FileError pollFileErrorCode = doUpload.getFileErrorCode();

        System.out.printf("%s - %s", TAG, "status code: " + pollStatusCode);
        System.out.printf("%s - %s", TAG, "result code: " + pollResultCode);
        System.out.printf("%s - %s", TAG, "file error code: " + pollFileErrorCode);

        if (pollStatusCode == PollResponse.Status.NO_MORE_REQUESTS_FOR_THIS_KEY && pollResultCode == PollResponse.Result.SUCCESS && pollFileErrorCode == PollResponse.FileError.NO_ERROR) {
            System.out.printf("%s - %s", TAG, "done polling");
            notifyUploadListenerCompleted(doUpload.getQuickKey());
        } else if (pollStatusCode != PollResponse.Status.NO_MORE_REQUESTS_FOR_THIS_KEY && pollResultCode == PollResponse.Result.SUCCESS && pollFileErrorCode == PollResponse.FileError.NO_ERROR) {
            System.out.printf("%s - %s", TAG, "still waiting for status code " + PollResponse.Status.NO_MORE_REQUESTS_FOR_THIS_KEY + ", but was " + pollStatusCode + " so restarting upload");
            startOrRestartUpload();
        } else {
            System.out.printf("%s - %s", TAG, "cancelling upload");
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_INVALID);
        }
    }

    private void doCheckUpload() throws IOException, NoSuchAlgorithmException {
        System.out.printf("%s - %s", TAG, "doCheckUpload()");
        if (!haveStoredCredentials()) {
            System.out.printf("%s - %s", TAG, "no credentials stored, cancelling upload for: " + mUploadItem.getFileName());
            mUploadItem.cancelUpload();
            return;
        }

        if (!mNetworkConnectivityMonitor.haveNetworkConnection()) {
            System.out.printf("%s - %s", TAG, "no network connection, cancelling upload for " + mUploadItem.getFileName());
            notifyUploadListenerCancelled(MSG_NO_NETWORK_CONNECTION);
            return;
        }

        if (mUploadItem.isCancelled()) {
            System.out.printf("%s - %s", TAG, "upload was cancelled for " + mUploadItem.getFileName());
            notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
            return;
        }

        // generate map with request parameters
        Map<String, String> keyValue = generateCheckUploadRequestParameters();

        Request request = new ApiRequestGenerator("1.0").createRequestObjectFromPath("upload/check.php");

        for (String key : keyValue.keySet()) {
            request.addQueryParameter(key, keyValue.get(key));
        }

        ClientHelperActionToken clientHelperActionToken = new ClientHelperActionToken("upload", mActionTokenManagerInterface);
        ApiClient apiClient = new ApiClient(clientHelperActionToken, mHttpWorker);
        Result result = apiClient.doRequest(request);
        Response mfResponse = result.getResponse();

        if (mfResponse == null) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (mfResponse.getBytes() == null) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (mfResponse.getBytes().length == 0) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (getResponseObject(new String(mfResponse.getBytes()), ApiResponse.class) == null) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        CheckResponse response = getResponseObject(new String(mfResponse.getBytes()), CheckResponse.class);

        if (response == null) {
            notifyUploadListenerCancelled(MSG_RESPONSE_ERROR);
            return;
        }

        // if there is an error code, cancel the upload
        if (response.getErrorCode() != ResponseCode.NO_ERROR) {
            notifyUploadListenerCancelled(MSG_RESPONSE_ERROR);
            return;
        }

        if (response.hasError()) {
            notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
            return;
        }

        mUploadItem.getChunkData().setNumberOfUnits(response.getResumableUpload().getNumberOfUnits());
        mUploadItem.getChunkData().setUnitSize(response.getResumableUpload().getUnitSize());
        int count = response.getResumableUpload().getBitmap().getCount();
        List<Integer> words = response.getResumableUpload().getBitmap().getWords();
        ResumableBitmap bitmap = new ResumableBitmap(count, words);
        mUploadItem.setBitmap(bitmap);
        System.out.printf("%s - %s", TAG, mUploadItem.getFileData().getFilePath() + " upload item bitmap: " + mUploadItem.getBitmap().getCount() + " count, " + mUploadItem.getBitmap().getWords() + " words.");

        // notify listeners that check has completed
        checkUploadFinished(mUploadItem, response);
    }

    private void doInstantUpload() {
        System.out.printf("%s - %s", TAG, "doInstantUpload()");

        if (!haveStoredCredentials()) {
            System.out.printf("%s - %s", TAG, "no credentials stored, task cancelling()");
            mUploadItem.cancelUpload();
            return;
        }

        if (mUploadItem.isCancelled()) {
            System.out.printf("%s - %s", TAG, "upload was cancelled for " + mUploadItem.getFileName());
            notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
            return;
        }

        if (!mNetworkConnectivityMonitor.haveNetworkConnection()) {
            notifyUploadListenerCancelled(MSG_NO_NETWORK_CONNECTION);
            return;
        }

        // generate map with request parameters
        Map<String, String> keyValue = generateInstantUploadRequestParameters();
        Request request = new ApiRequestGenerator("1.0").createRequestObjectFromPath("upload/instant.php");

        for (String key : keyValue.keySet()) {
            request.addQueryParameter(key, keyValue.get(key));
        }

        ClientHelperActionToken clientHelperActionToken = new ClientHelperActionToken("upload", mActionTokenManagerInterface);
        ApiClient apiClient = new ApiClient(clientHelperActionToken, mHttpWorker);
        Result result = apiClient.doRequest(request);

        Response mfResponse = result.getResponse();

        if (mfResponse == null) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (mfResponse.getBytes() == null) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (mfResponse.getBytes().length == 0) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (getResponseObject(new String(mfResponse.getBytes()), ApiResponse.class) == null) {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        InstantResponse response = getResponseObject(new String(mfResponse.getBytes()), InstantResponse.class);

        if (response == null) {
            notifyUploadListenerCancelled(MSG_RESPONSE_ERROR);
            return;
        }

        if (response.getErrorCode() != ResponseCode.NO_ERROR) {
            notifyUploadListenerCancelled(MSG_RESPONSE_ERROR);
            return;
        }

        if (!response.getQuickkey().isEmpty()) {
            // notify listeners that check has completed
            instantUploadFinished(response.getQuickkey());
        } else {
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
        }
    }

    private void doResumableUpload() throws IOException, NoSuchAlgorithmException {
        System.out.printf("%s - %s", TAG, "doResumableUpload()");

        //get file size. this will be used for chunks.
        FileData fileData = mUploadItem.getFileData();
        long fileSize = fileData.getFileSize();

        // get chunk. these will be used for chunks.
        ChunkData ChunkData = mUploadItem.getChunkData();
        int numChunks = ChunkData.getNumberOfUnits();
        int unitSize = ChunkData.getUnitSize();

        // loop through our chunks and create http post with header data and send after we are done looping,
        // let the listener know we are completed
        ResumableResponse response = null;
        for (int chunkNumber = 0; chunkNumber < numChunks; chunkNumber++) {
            if (!haveStoredCredentials()) {
                System.out.printf("%s - %s", TAG, "no credentials stored, task cancelling()");
                mUploadItem.cancelUpload();
                return;
            }

            if (mUploadItem.isCancelled()) {
                System.out.printf("%s - %s", TAG, "upload was cancelled for " + mUploadItem.getFileName());
                notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
                return;
            }

            if (!mNetworkConnectivityMonitor.haveNetworkConnection()) {
                notifyUploadListenerCancelled(MSG_NO_NETWORK_CONNECTION);
                return;
            }

            // if the bitmap says this chunk number is uploaded then we can just skip it, if not, we upload it.
            if (!mUploadItem.getBitmap().isUploaded(chunkNumber)) {
                // get the chunk size for this chunk
                int chunkSize = getChunkSize(chunkNumber, numChunks, fileSize, unitSize);

                ResumableChunkInfo resumableChunkInfo = createResumableChunkInfo(unitSize, chunkNumber);
                if (resumableChunkInfo == null) {
                    notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                    return;
                }

                String chunkHash = resumableChunkInfo.getChunkHash();
                byte[] uploadChunk = resumableChunkInfo.getUploadChunk();

                printDebugCurrentChunk(chunkNumber, numChunks, chunkSize, unitSize, fileSize, chunkHash, uploadChunk);

                // generate the post headers
                Map<String, String> headers = generatePostHeaders(mUtf8EncodedFileName, fileSize, chunkNumber, chunkHash, chunkSize);
                // generate the get parameters
                Map<String, String> parameters = generateResumableRequestParameters();

                printDebugRequestData(headers, parameters);


                Request request = new ApiRequestGenerator().createRequestObjectFromPath("upload/resumable.php");

                for (String key : parameters.keySet()) {
                    request.addQueryParameter(key, parameters.get(key));
                }

                for (String key : headers.keySet()) {
                    request.addHeader(key, headers.get(key));
                }

                request.addPayload(uploadChunk);

                ClientHelperActionToken clientHelperActionToken = new ClientHelperActionToken("upload", mActionTokenManagerInterface);
                ApiClient apiClient = new ApiClient(clientHelperActionToken, mHttpWorker);
                Result result = apiClient.doRequest(request);

                Response mfResponse = result.getResponse();

                if (mfResponse == null) {
                    notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                    return;
                }

                if (mfResponse.getBytes() == null) {
                    notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                    return;
                }

                if (mfResponse.getBytes().length == 0) {
                    notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                    return;
                }

                if (getResponseObject(new String(mfResponse.getBytes()), ApiResponse.class) == null) {
                    notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                    return;
                }

                response = getResponseObject(new String(mfResponse.getBytes()), ResumableResponse.class);

                // set poll upload key if possible
                if (shouldSetPollUploadKey(response)) {
                    mUploadItem.setPollUploadKey(response.getDoUpload().getPollUploadKey());
                }

                if (shouldCancelUpload(response)) {
                    notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_INVALID);
                    return;
                }

                // update the response bitmap
                int count = response.getResumableUpload().getBitmap().getCount();
                List<Integer> words = response.getResumableUpload().getBitmap().getWords();
                ResumableBitmap bitmap = new ResumableBitmap(count, words);
                mUploadItem.setBitmap(bitmap);
                System.out.printf("%s - %s", TAG, "(" + mUploadItem.getFileData().getFilePath() + ") upload item bitmap: " + mUploadItem.getBitmap().getCount() + " count, (" + mUploadItem.getBitmap().getWords() + ") words.");

                clearReferences(chunkSize, chunkHash, uploadChunk, headers, parameters);
            }

            notifyUploadListenerOnProgressUpdate(numChunks);

        } // end loop

        // let the listeners know that upload has attempted to upload all chunks.
        resumableUploadFinished(response);
    }

    private void doPollUpload() throws IOException, NoSuchAlgorithmException {
        System.out.printf("%s - %s", TAG, "doPollUpload()");
        //generate our request string
        Map<String, String> keyValue = generatePollRequestParameters();

        int pollCount = 0;
        do {
            if (!haveStoredCredentials()) {
                System.out.printf("%s - %s", TAG, "no credentials stored, task cancelling()");
                mUploadItem.cancelUpload();
                return;
            }

            if (mUploadItem.isCancelled()) {
                System.out.printf("%s - %s", TAG, "upload was cancelled for " + mUploadItem.getFileName());
                notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
                return;
            }

            if (!mNetworkConnectivityMonitor.haveNetworkConnection()) {
                notifyUploadListenerCancelled(MSG_NO_NETWORK_CONNECTION);
                return;
            }
            // increment counter
            pollCount++;
            // get api response.

            Request request = new ApiRequestGenerator("1.0").createRequestObjectFromPath("upload/poll.php");

            for (String key : keyValue.keySet()) {
                request.addQueryParameter(key, keyValue.get(key));
            }

            ClientHelperNoToken clientHelperActionToken = new ClientHelperNoToken();
            ApiClient apiClient = new ApiClient(clientHelperActionToken, mHttpWorker);
            Result result = apiClient.doRequest(request);
            Response mfResponse = result.getResponse();

            if (mfResponse == null) {
                notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                return;
            }

            if (mfResponse.getBytes() == null) {
                notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                return;
            }

            if (mfResponse.getBytes().length == 0) {
                notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                return;
            }


            if (getResponseObject(new String(mfResponse.getBytes()), ApiResponse.class) == null) {
                notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
                return;
            }

            PollResponse response = getResponseObject(new String(mfResponse.getBytes()), PollResponse.class);

            System.out.printf("%s - %s", TAG, "received error code: " + response.getErrorCode());
            //check to see if we need to call pollUploadCompleted or loop again
            switch (response.getErrorCode()) {
                case NO_ERROR:
                    //just because we had response/result "Success" doesn't mean everything is good.
                    //we need to find out if we should continue polling or not
                    //  conditions to check:
                    //      first   -   result code no error? yes, keep calm and poll on. no, cancel upload because error.
                    //      second  -   fileerror code no error? yes, carry on old chap!. no, cancel upload because error.
                    //      third   -   status code 99 (no more requests)? yes, done. no, continue.
                    if (response.getDoUpload().getResultCode() != PollResponse.Result.SUCCESS) {
                        System.out.printf("%s - %s", TAG, "result code: " + response.getDoUpload().getResultCode() + " need to cancel");
                        notifyUploadListenerCancelled(MSG_RESPONSE_ERROR);
                        return;
                    }

                    if (response.getDoUpload().getFileErrorCode() != PollResponse.FileError.NO_ERROR) {
                        System.out.printf("%s - %s", TAG, "result code: " + response.getDoUpload().getFileErrorCode() + " need to cancel");
                        notifyUploadListenerCancelled(MSG_RESPONSE_ERROR);
                        return;
                    }

                    if (response.getDoUpload().getStatusCode() == PollResponse.Status.NO_MORE_REQUESTS_FOR_THIS_KEY) {
                        System.out.printf("%s - %s", TAG, "status code: " + response.getDoUpload().getStatusCode());
                        pollUploadFinished(response);
                        return;
                    }
                    break;
                default:
                    // stop polling and inform listeners we cancel because API result wasn't "Success"
                    notifyUploadListenerCancelled(MSG_RESPONSE_ERROR);
                    return;
            }

            notifyUploadListenerOnPolling(response.getDoUpload().getDescription());

            //wait before next api call
            try {
                Thread.sleep(mMillisecondsBetweenPolls);
            } catch (InterruptedException e) {
                System.out.printf("%s - %s", TAG, "Exception: " + e);
                notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
                return;
            }

            if (mUploadItem.isCancelled()) {
                pollCount = mMaxPolls;
            }
        } while (pollCount < mMaxPolls);

        startOrRestartUpload();
    }

    private Map<String, String> generatePollRequestParameters() {
        System.out.printf("%s - %s", TAG, "generatePollRequestParameters()");
        LinkedHashMap<String, String> keyValue = new LinkedHashMap<String, String>();
        keyValue.put("key", mUploadItem.getPollUploadKey());
        keyValue.put("response_format", "json");
        return keyValue;
    }

    @SuppressWarnings({"ParameterCanBeLocal", "UnusedParameters", "UnusedAssignment", "AssignmentToNull"})
    private void clearReferences(int chunkSize, String chunkHash, byte[] uploadChunk, Map<String, String> headers, Map<String, String> parameters) {
        chunkSize = 0;
        chunkHash = null;
        uploadChunk = null;
        headers = null;
        parameters = null;
    }

    private void printDebugRequestData(Map<String, String> headers, Map<String, String> parameters) {
        System.out.printf("%s - %s", TAG, "printDebugRequestData()");
        System.out.printf("%s - %s", TAG, "headers: " + headers);
        System.out.printf("%s - %s", TAG, "parameters: " + parameters);
    }

    @SuppressWarnings("UnusedParameters")
    private void printDebugCurrentChunk(int chunkNumber, int numChunks, int chunkSize, int unitSize, long fileSize, String chunkHash, byte[] uploadChunk) {
        System.out.printf("%s - %s", TAG, "printDebugCurrentChunk()");
        System.out.printf("%s - %s", TAG, "current thread: " + Thread.currentThread().getName());
        System.out.printf("%s - %s", TAG, "current chunk: " + chunkNumber);
        System.out.printf("%s - %s", TAG, "total chunks: " + numChunks);
        System.out.printf("%s - %s", TAG, "current chunk size: " + chunkSize);
        System.out.printf("%s - %s", TAG, "normal chunk size: " + unitSize);
        System.out.printf("%s - %s", TAG, "total file size: " + fileSize);
        System.out.printf("%s - %s", TAG, "current chunk hash: " + chunkHash);
        System.out.printf("%s - %s", TAG, "upload chunk ");
    }

    private boolean shouldCancelUpload(ResumableResponse response) {
        System.out.printf("%s - %s", TAG, "shouldCancelUpload()");
        // if API response code OR Upload Response Result code have an error then we need to terminate the process
        if (response.hasError()) {
            return true;
        }

        if (response.getDoUpload().getResultCode() != ResumableResponse.Result.NO_ERROR) {
            if (response.getDoUpload().getResultCode() != ResumableResponse.Result.SUCCESS_FILE_MOVED_TO_ROOT) {
                return true;
            }
        }

        return false;
    }

    private ResumableChunkInfo createResumableChunkInfo(int unitSize, int chunkNumber) throws IOException, NoSuchAlgorithmException {
        System.out.printf("%s - %s", TAG, "createResumableChunkInfo");
        ResumableChunkInfo resumableChunkInfo;
        // generate the chunk
        FileInputStream fis;
        BufferedInputStream bis;
        String chunkHash;
        byte[] uploadChunk;
        fis = new FileInputStream(mUploadItem.getFileData().getFilePath());
        bis = new BufferedInputStream(fis);
        uploadChunk = createUploadChunk(unitSize, chunkNumber, bis);
        if (uploadChunk == null) {
            return null;
        }

        chunkHash = getSHA256(uploadChunk);
        resumableChunkInfo = new ResumableChunkInfo(chunkHash, uploadChunk);
        fis.close();
        bis.close();
        return resumableChunkInfo;
    }

    private Map<String, String> generateResumableRequestParameters() {
        System.out.printf("%s - %s", TAG, "generateResumableRequestParameters()");
        // get upload mOptions. these will be passed as request parameters
        UploadItemOptions uploadItemOptions = mUploadItem.getUploadOptions();
        String actionOnDuplicate = uploadItemOptions.getActionOnDuplicate();
        String versionControl = uploadItemOptions.getVersionControl();
        String uploadFolderKey = uploadItemOptions.getUploadFolderKey();
        String uploadPath = uploadItemOptions.getUploadPath();

        String actionToken = mActionTokenManagerInterface.borrowUploadActionToken().getTokenString();
        LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("session_token", actionToken);
        parameters.put("action_on_duplicate", actionOnDuplicate);
        parameters.put("response_format", "json");
        parameters.put("version_control", versionControl);
        if (uploadPath != null && !uploadPath.isEmpty()) {
            parameters.put("path", uploadPath);
        } else {
            parameters.put("folder_key", uploadFolderKey);
        }

        return parameters;
    }

    private Map<String, String> generatePostHeaders(String encodedShortFileName, long fileSize, int chunkNumber, String chunkHash, int chunkSize) {
        System.out.printf("%s - %s", TAG, "generatePostHeaders()");
        LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
        // these headers are related to the entire file
        headers.put("x-filename", encodedShortFileName);
        headers.put("x-filesize", String.valueOf(fileSize));
        headers.put("x-filehash", mUploadItem.getFileData().getFileHash());
        // these headers are related to the individual chunk
        headers.put("x-unit-id", Integer.toString(chunkNumber));
        headers.put("x-unit-hash", chunkHash);
        headers.put("x-unit-size", Integer.toString(chunkSize));
        return headers;
    }

    private boolean shouldSetPollUploadKey(ResumableResponse response) {
        System.out.printf("%s - %s", TAG, "shouldSetPollUploadKey()");
        switch (response.getDoUpload().getResultCode()) {
            case NO_ERROR:
            case SUCCESS_FILE_MOVED_TO_ROOT:
                return true;
            default:
                return false;
        }
    }

    private int getChunkSize(int chunkNumber, int numChunks, long fileSize, int unitSize) {
        System.out.printf("%s - %s", TAG, "getChunkSize()");
        int chunkSize;
        if (chunkNumber >= numChunks) {
            chunkSize = 0; // represents bad size
        } else {
            if (fileSize % unitSize == 0) { // all units will be of unitSize
                chunkSize = unitSize;
            } else if (chunkNumber < numChunks - 1) { // this unit is of unitSize
                chunkSize = unitSize;
            } else { // this unit is "special" and is the modulo of fileSize and unitSize;
                chunkSize = (int) (fileSize % unitSize);
            }
        }

        return chunkSize;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private byte[] createUploadChunk(long unitSize, int chunkNumber, BufferedInputStream fileStream) throws IOException {
        System.out.printf("%s - %s", TAG, "createUploadChunk()");

        System.out.printf("%s - %s", TAG, "starting read using fileStream.read()");
//        byte[] readBytes = new byte[(int) unitSize];
        int offset = (int) (unitSize * chunkNumber);
        fileStream.skip(offset);

        ByteArrayOutputStream output = new ByteArrayOutputStream( (int) unitSize);
        int bufferSize = 65536;

        System.out.printf("%s - %s", TAG, "starting read using ByteArrayOutputStream with buffer size: " + bufferSize);

        byte[] buffer = new byte[bufferSize];
        int readSize;
        int t = 0;

        while ((readSize = fileStream.read(buffer)) > 0 && t <= unitSize) {
            if (!haveStoredCredentials()) {
                System.out.printf("%s - %s", TAG, "no credentials stored, task cancelling()");
                mUploadItem.cancelUpload();
                return null;
            }

            if (mUploadItem.isCancelled()) {
                System.out.printf("%s - %s", TAG, "upload was cancelled for " + mUploadItem.getFileName());
                notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
                return null;
            }

            if (!mNetworkConnectivityMonitor.haveNetworkConnection()) {
                notifyUploadListenerCancelled(MSG_NO_NETWORK_CONNECTION);
                return null;
            }

            if (output.size() + readSize > unitSize) {
                int actualReadSize = (int) unitSize - output.size();
                output.write(buffer, 0, actualReadSize);
            } else {
                output.write(buffer, 0, readSize);
            }

            if (readSize > 0) {
                t += readSize;
            }
        }

        byte[] data = output.toByteArray();

        System.out.printf("%s - %s", TAG, "total bytes read: " + t);
        System.out.printf("%s - %s", TAG, "data size: " + data.length);
        System.out.printf("%s - %s", TAG, "expected size: " + unitSize);

//        System.out.printf("%s - %s", TAG, "data size matches readBytes size: " + (data.length == readBytes.length));

        return data;
    }

    private String getSHA256(byte[] chunkData) throws NoSuchAlgorithmException, IOException {
        System.out.printf("%s - %s", TAG, "getSHA256()");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        //test code
        InputStream in = new ByteArrayInputStream(chunkData, 0, chunkData.length);
        byte[] bytes = new byte[8192];
        int byteCount;
        while ((byteCount = in.read(bytes)) > 0) {
            md.update(bytes, 0, byteCount);
        }
        byte[] hashBytes = md.digest();
        //test code
        //byte[] hashBytes = md.digest(chunkData); //original code

        return convertHashBytesToString(hashBytes);
    }

    private String convertHashBytesToString(byte[] hashBytes) {
        System.out.printf("%s - %s", TAG, "convertHashBytesToString()");
        StringBuilder sb = new StringBuilder();
        for (byte hashByte : hashBytes) {
            String tempString = Integer.toHexString((hashByte & 0xFF) | 0x100).substring(1, 3);
            sb.append(tempString);
        }

        return sb.toString();
    }

    private Map<String, String> generateInstantUploadRequestParameters() {
        System.out.printf("%s - %s", TAG, "generateInstantUploadRequestParameters()");
        // generate map with request parameters
        Map<String, String> keyValue = new LinkedHashMap<String, String>();
        keyValue.put("filename", mUtf8EncodedFileName);
        keyValue.put("hash", mUploadItem.getFileData().getFileHash());
        keyValue.put("size", Long.toString(mUploadItem.getFileData().getFileSize()));
        keyValue.put("response_format", "json");
        if (mUploadItem.getUploadOptions().getUploadPath() != null && !mUploadItem.getUploadOptions().getUploadPath().isEmpty()) {
            keyValue.put("path", mUploadItem.getUploadOptions().getUploadPath());
        } else {
            keyValue.put("folder_key", mUploadItem.getUploadOptions().getUploadFolderKey());
        }

        keyValue.put("action_on_duplicate", mUploadItem.getUploadOptions().getActionOnDuplicate());
        return keyValue;
    }

    private Map<String, String> generateCheckUploadRequestParameters() {
        System.out.printf("%s - %s", TAG, "generateCheckUploadRequestParameters()");
        // generate map with request parameters
        Map<String, String> keyValue = new LinkedHashMap<String, String>();
        keyValue.put("filename", mUtf8EncodedFileName);
        keyValue.put("hash", mUploadItem.getFileData().getFileHash());
        keyValue.put("size", Long.toString(mUploadItem.getFileData().getFileSize()));
        keyValue.put("resumable", mUploadItem.getUploadOptions().getResumable());
        keyValue.put("response_format", "json");
        if (mUploadItem.getUploadOptions().getUploadPath() != null && !mUploadItem.getUploadOptions().getUploadPath().isEmpty()) {
            keyValue.put("path", mUploadItem.getUploadOptions().getUploadPath());
        } else {
            keyValue.put("folder_key", mUploadItem.getUploadOptions().getUploadFolderKey());
        }
        return keyValue;
    }

    private boolean haveStoredCredentials() {
        System.out.printf("%s - %s", TAG, "haveStoredCredentials()");
        return !mUserCredentials.getCredentials().isEmpty();
    }

    private void startOrRestartUpload() throws IOException, NoSuchAlgorithmException {
        System.out.printf("%s - %s", TAG, "startOrRestartUpload()");
        int uploadAttemptCount = mUploadItem.getUploadAttemptCount();
        // don't start upload if cancelled or attempts > mMaxUploadAttempts
        if (uploadAttemptCount > mMaxUploadAttempts || mUploadItem.isCancelled()) {
            if (uploadAttemptCount > mMaxUploadAttempts) {
                System.out.printf("%s - %s", TAG, "upload attempt count > mMaxUploadAttempts");
            }

            if (mUploadItem.isCancelled()) {
                System.out.printf("%s - %s", TAG, "upload item was cancelled");
            }

            notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
            return;
        }
        //don't add the item to the backlog queue if it is null or the path is null
        System.out.printf("%s - %s", TAG, "getFileData() path: " + mUploadItem.getFileData().getFilePath());
        System.out.printf("%s - %s", TAG, "getFileData() path: " + mUploadItem.getFileData().getFilePath());
        System.out.printf("%s - %s", TAG, "getFileData() null: " + (mUploadItem.getFileData() == null));
        System.out.printf("%s - %s", TAG, "getFileData().getFilePath() null: " + (mUploadItem.getFileData().getFilePath() == null));
        System.out.printf("%s - %s", TAG, "getFileData().getFilePath().isEmpty(): " + (mUploadItem.getFileData().getFilePath().isEmpty()));
        System.out.printf("%s - %s", TAG, "getFileData().getFileHash().isEmpty(): " + (mUploadItem.getFileData().getFileHash().isEmpty()));
        System.out.printf("%s - %s", TAG, "getFileData().getFileSize() == 0: " + (mUploadItem.getFileData().getFileSize() == 0));
        if (mUploadItem.getFileData() == null) {
            System.out.printf("%s - %s", TAG, "one or more required parameters are invalid, not adding item to queue");
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (mUploadItem.getFileData().getFilePath() == null || mUploadItem.getFileData().getFilePath().isEmpty()) {
            System.out.printf("%s - %s", TAG, "one or more required parameters are invalid, not adding item to queue");
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (mUploadItem.getFileData() == null || mUploadItem.getFileData().getFileHash().isEmpty()) {
            System.out.printf("%s - %s", TAG, "one or more required parameters are invalid, not adding item to queue");
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (mUploadItem.getFileData().getFileSize() == 0) {
            System.out.printf("%s - %s", TAG, "one or more required parameters are invalid, not adding item to queue");
            notifyUploadListenerCancelled(MSG_REQUIRED_PARAMETERS_NULL);
            return;
        }

        if (uploadAttemptCount <= mMaxUploadAttempts) {
            doCheckUpload();
        } else {
            System.out.printf("%s - %s", TAG, "upload attempt count > mMaxUploadAttempts");
            notifyUploadListenerCancelled(MSG_CANCELLED_UPLOAD);
        }
    }

    private void notifyUploadListenerStarted() {
        System.out.printf("%s - %s", TAG, "notifyUploadListenerStarted()");
        if (mUploadListener != null) {
            mUploadListener.onStarted(mUploadItem);
        }
    }

    private void notifyUploadListenerCompleted(String quickKey) {
        System.out.printf("%s - %s", TAG, "notifyUploadListenerCompleted()");
        if (mUploadListener != null) {
            mUploadListener.onCompleted(mUploadItem, quickKey);
        }
    }

    private void notifyUploadListenerOnProgressUpdate(int totalChunks) {
        System.out.printf("%s - %s", TAG, "notifyUploadListenerOnProgressUpdate()");
        if (mUploadListener != null) {
            // give number of chunks/numChunks for onProgressUpdate
            int numUploaded = 0;
            for (int chunkNumber = 0; chunkNumber < totalChunks; chunkNumber++) {
                if (mUploadItem.getBitmap().isUploaded(chunkNumber)) {
                    numUploaded++;
                }
            }
            System.out.printf("%s - %s", TAG, numUploaded + "/" + totalChunks + " chunks uploaded");
            mUploadListener.onProgressUpdate(mUploadItem, numUploaded, totalChunks);
        }
    }

    private void notifyUploadListenerOnPolling(String message) {
        System.out.printf("%s - %s", TAG, "notifyUploadListenerOnPolling()");
        if (mUploadListener != null) {
            mUploadListener.onPolling(mUploadItem, message);
        }
    }

    private void notifyUploadListenerCancelled(String reasonForCancel) {
        System.out.printf("%s - %s", TAG, "notifyUploadListenerCancelled()");
        mUploadItem.cancelUpload();
        if (mUploadListener != null) {
            mUploadListener.onCancelled(mUploadItem, reasonForCancel);
        }
    }

    /**
     * Builder is a class used to build a UploadRunnable (see builder pattern)
     */
    public static class Builder {
        private static final int DEFAULT_MAX_POLLS = 60;
        private static final int DEFAULT_MILLISECONDS_BETWEEN_POLLS = 2000;
        private static final int DEFAULT_MAX_UPLOAD_ATTEMPTS = 3;

        private int maxPolls = DEFAULT_MAX_POLLS;
        private long millisecondsBetweenPolls = DEFAULT_MILLISECONDS_BETWEEN_POLLS;
        private int maxUploadAttempts = DEFAULT_MAX_UPLOAD_ATTEMPTS;
        private final Configuration configuration;
        private final UploadItem uploadItem;
        private UploadListenerInterface mfUploadListener;

        /**
         * Constructor used to create an UploadRunnable.
         * @param configuration - an MFDefaultTokenFarm to use.
         * @param uploadItem - an UploadItem to use.
         */
        public Builder(Configuration configuration, UploadItem uploadItem) {
            if (configuration == null) {
                throw new IllegalArgumentException("MFDefaultTokenFarm cannot be null");
            }

            if (uploadItem == null) {
                throw new IllegalArgumentException("UploadItem cannot be null");
            }

            this.configuration = configuration;
            this.uploadItem = uploadItem;
        }

        /**
         * sets the max poll attempts for the upload.
         * @param maxPolls the max poll attempts.
         * @return a static UploadRunnable.Builder object to allow chaining calls.
         */
        public Builder maxPolls(int maxPolls) {
            if (maxPolls < 1) {
                throw new IllegalArgumentException("max polls cannot be less than 0");
            }
            this.maxPolls = maxPolls;
            return this;
        }

        /**
         * sets milliseconds between poll calls.
         * @param millisecondsBetweenPolls milliseconds between polls.
         * @return a static UploadRunnable.Builder object to allow chaining calls.
         */
        public Builder millisecondsBetweenPolls(long millisecondsBetweenPolls) {
            if (millisecondsBetweenPolls < 0) {
                throw new IllegalArgumentException("time between polls cannot be less than 0");
            }
            this.millisecondsBetweenPolls = millisecondsBetweenPolls;
            return this;
        }

        /**
         * sets the max attempts to try to upload the file.
         * @param maxUploadAttempts the max upload attempts.
         * @return a static UploadRunnable.Builder object to allow chaining calls.
         */
        public Builder maxUploadAttempts(int maxUploadAttempts) {
            if (maxUploadAttempts < 1) {
                throw new IllegalArgumentException("max upload attempts cannot be less than 1");
            }
            this.maxUploadAttempts = maxUploadAttempts;
            return this;
        }

        /**
         * sets the UploadListenerInterface used for callbacks.
         * @param mfUploadListener the UploadListenerInterface implementation.
         * @return a static UploadRunnable.Builder object to allow chaining calls.
         */
        public Builder uploadListener(UploadListenerInterface mfUploadListener) {
            this.mfUploadListener = mfUploadListener;
            return this;
        }

        /**
         * constructs a new UploadRunnable.
         * @return a new UploadRunnable.
         */
        public UploadRunnable build() {
            return new UploadRunnable(this);
        }
    }

    /**
     * Gets a response object from a response string and response class
     * @param responseString String response to create response form
     * @param responseClass ResponseClass to return
     * @param <ResponseClass> ResponseClass to return and create response from
     * @return ResponseClass
     */
    public <ResponseClass extends ApiResponse> ResponseClass getResponseObject(String responseString, Class<ResponseClass> responseClass) {
        if (responseString == null) {
            return null;
        }
        return new Gson().fromJson(getResponseStringForGson(responseString), responseClass);
    }

    private String getResponseStringForGson(String response) {
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
}
