package com.mediafire.sdk.uploader;

import com.mediafire.sdk.MFApiRequest;
import com.mediafire.sdk.MediaFireApiRequest;
import com.mediafire.sdk.MediaFireClient;
import com.mediafire.sdk.MediaFireException;
import com.mediafire.sdk.response_models.MediaFireApiResponse;
import com.mediafire.sdk.response_models.data_models.DoUploadResumableModel;
import com.mediafire.sdk.response_models.data_models.ResumableBitmapModel;
import com.mediafire.sdk.response_models.data_models.ResumableUploadModel;
import com.mediafire.sdk.response_models.upload.UploadResumableResponse;
import com.mediafire.sdk.util.TextUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

class MFRunnableResumableUpload implements Runnable {

    private static final String PARAM_FOLDER_KEY = "folder_key";
    private static final String PARAM_FOLDER_PATH = "path";
    private static final String PARAM_ACTION_ON_DUPLICATE = "action_on_duplicate";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_X_FILENAME = "x-filename";
    private static final String HEADER_X_FILESIZE = "x-filesize";
    private static final String HEADER_X_FILEHASH = "x-filehash";
    private static final String HEADER_X_UNIT_HASH = "x-unit-hash";
    private static final String HEADER_X_UNIT_ID = "x-unit-id";
    private static final String HEADER_X_UNIT_SIZE = "x-unit-size";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private List<Boolean> uploadUnits = new LinkedList<>();

    private final MediaFireClient mediaFire;
    private final MediaFireFileUpload upload;
    private final ResumableUploadModel resumableUpload;
    private final OnResumableUploadStatusListener callback;

    public MFRunnableResumableUpload(MediaFireClient mediaFire, MediaFireFileUpload upload, ResumableUploadModel resumableUpload, MFRunnableResumableUpload.OnResumableUploadStatusListener callback) {
        this.mediaFire = mediaFire;
        this.upload = upload;
        this.resumableUpload = resumableUpload;
        this.callback = callback;
    }

    @Override
    public void run() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put(PARAM_ACTION_ON_DUPLICATE, "keep");
        if (!TextUtils.isEmpty(this.upload.getFolderKey())) {
            params.put(PARAM_FOLDER_KEY, this.upload.getFolderKey());
        }

        if (!TextUtils.isEmpty(this.upload.getMediaFirePath())) {
            params.put(PARAM_FOLDER_PATH, this.upload.getMediaFirePath());
        }

        int numUnits = resumableUpload.getNumberOfUnits();
        int unitSize = resumableUpload.getUnitSize();

        Map<String, Object> headers = new HashMap<>();
        if (this.upload.getFileSize() == 0) {
            headers.put(HEADER_X_FILESIZE, this.upload.getFile().length());
        } else {
            headers.put(HEADER_X_FILESIZE, this.upload.getFileSize());
        }

        if (!TextUtils.isEmpty(this.upload.getSha256Hash())) {
            headers.put(HEADER_X_FILEHASH, this.upload.getSha256Hash());
        } else {
            headers.put(HEADER_X_FILEHASH, mediaFire.getHasher().sha256(this.upload.getFile()));
        }

        headers.put(HEADER_CONTENT_TYPE, CONTENT_TYPE_OCTET_STREAM);
        headers.put(HEADER_X_FILENAME, this.upload.getFileName());

        for (int chunkNumber = 0; chunkNumber < numUnits; chunkNumber++) {
            try {
                if (isChunkUploaded(chunkNumber)) {
                    continue;
                }

                int chunkSize = getChunkSize(chunkNumber, numUnits, this.upload.getFileSize(), unitSize);
                byte[] chunk = makeChunk(unitSize, chunkNumber);
                String chunkHash = mediaFire.getHasher().sha256(chunk);

                headers.put(HEADER_X_UNIT_ID, chunkNumber);
                headers.put(HEADER_X_UNIT_SIZE, chunkSize);
                headers.put(HEADER_X_UNIT_HASH, chunkHash);

                MediaFireApiRequest request = new MFApiRequest("/upload/resumable.php", params, chunk, headers);
                UploadResumableResponse response = mediaFire.uploadRequest(request, UploadResumableResponse.class);

                if (response.hasError()) {
                    if (callback != null) {
                        this.callback.onResumableUploadApiError(this.upload, response);
                    }
                    return;
                }

                DoUploadResumableModel doUpload = response.getDoUpload();
                ResumableUploadModel newResumableUpload = response.getResumableUpload();
                String allUnitsReady = newResumableUpload.getAllUnitsReady();

                if (allUnitsReady != null && "yes".equals(allUnitsReady) && doUpload != null) {
                    String uploadKey = doUpload.getKey();
                    if (this.callback != null) {
                        this.callback.onResumableUploadReadyToPoll(this.upload, uploadKey);
                    }
                    return;
                }

                ResumableBitmapModel bitmap = resumableUpload.getBitmap();
                if (bitmap != null) {
                    int count = bitmap.getCount();
                    List<Integer> words = bitmap.getWords();

                    updateUploadBitmap(count, words);
                }

                int numUploaded = 0;
                for (int chunkCount = 0; chunkCount < numUnits; chunkCount++) {
                    if (isChunkUploaded(chunkCount)) {
                        numUploaded++;
                    }
                }

                double percentFinished = (double) numUploaded / (double) numUnits;
                percentFinished *= 100;
                if (this.callback != null) {
                    this.callback.onResumableUploadProgress(this.upload, percentFinished);
                }
            } catch (MediaFireException e) {
                if (callback != null) {
                    callback.onResumableUploadSdkException(upload, e);
                }
                return;
            } catch (IOException e) {
                if (callback != null) {
                    callback.onResumableUploadIOException(upload, e);
                }
                return;
            }
        }

        if (callback != null) {
            callback.onResumableUploadFinishedIncomplete(this.upload);
        }
    }

    private boolean isChunkUploaded(int chunkId) {
        if (this.uploadUnits.isEmpty()) {
            return false;
        }
        return this.uploadUnits.get(chunkId);
    }

    private void updateUploadBitmap(int count, List<Integer> words) {
        List<Boolean> uploadUnits = new LinkedList<>();

        if (words == null || words.isEmpty()) {
            this.uploadUnits = uploadUnits;
            return;
        }

        //loop count times
        for (int i = 0; i < count; i++) {
            //convert words to binary string
            String word = Integer.toBinaryString(words.get(i));

            //ensure number is 16 bit by adding 0 until there are 16 bits
            while (word.length() < 16) {
                word = "0" + word;
            }

            //add boolean to collection depending on bit value
            for (int j = 0; j < word.length(); j++) {
                uploadUnits.add(i * 16 + j, word.charAt(15 - j) == '1');
            }
        }

        this.uploadUnits = uploadUnits;
    }

    private byte[] makeChunk(int unitSize, int chunkNumber) throws IOException {
        FileInputStream fis = new FileInputStream(this.upload.getFile());
        BufferedInputStream bis = new BufferedInputStream(fis);
        byte[] uploadChunk = createUploadChunk(unitSize, chunkNumber, bis);
        fis.close();
        bis.close();
        return uploadChunk;
    }

    private int getChunkSize(int chunkNumber, int numChunks, long fileSize, int unitSize) {
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

    private byte[] createUploadChunk(long unitSize, int chunkNumber, BufferedInputStream fileStream) throws IOException {
        int offset = (int) (unitSize * chunkNumber);
        fileStream.skip(offset);

        ByteArrayOutputStream output = new ByteArrayOutputStream((int) unitSize);
        int bufferSize = 65536;

        byte[] buffer = new byte[bufferSize];
        int readSize;
        int t = 0;

        while ((readSize = fileStream.read(buffer)) > 0 && t <= unitSize) {
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

        return output.toByteArray();
    }

    public interface OnResumableUploadStatusListener {
        void onResumableUploadReadyToPoll(MediaFireFileUpload upload, String uploadKey);
        void onResumableUploadProgress(MediaFireFileUpload upload, double percentFinished);
        void onResumableUploadSdkException(MediaFireFileUpload upload, MediaFireException e);
        void onResumableUploadFinishedIncomplete(MediaFireFileUpload upload);
        void onResumableUploadIOException(MediaFireFileUpload upload, IOException e);
        void onResumableUploadApiError(MediaFireFileUpload upload, MediaFireApiResponse response);
    }
}
