package com.mediafire.uploader.manager;

import com.mediafire.sdk.config.MFConfiguration;
import com.mediafire.uploader.interfaces.UploadListener;
import com.mediafire.uploader.process.CheckProcess;
import com.mediafire.uploader.uploaditem.UploadItem;

import java.util.concurrent.BlockingQueue;

/**
 * UploadManager moves UploadItems from a Collection into Threads.
 * Number of threads that will be started is limited to the maximumThreadCount (default = 5)
 *
 * @author
 */
public class UploadManager extends UploadManagerWorker {
    private static final String TAG = UploadManager.class.getCanonicalName();
    private UploadListener uiListener;

    /**
     * Constructor that takes a SessionManager, HttpInterface, and a maximum thread count.
     *
     * @param mediaFire     The SessionManager to use for API operations.
     * @param maximumThreadCount The maximum number of threads to use for uploading.
     */
    public UploadManager(MediaFire mediaFire, int maximumThreadCount, int maximumUploadAttempts) {
        super(mediaFire, maximumUploadAttempts, maximumUploadAttempts);
    }

    public void setUploadListener(UploadListener uiListener) {
        MFConfiguration.getErrorTracker().i(TAG, "setUploadListener");
        this.uiListener = uiListener;
    }

    public int getAllItems() {
        MFConfiguration.getErrorTracker().i(TAG, "getAllItems()");
        return workQueue.size() + executor.getActiveCount();
    }

    public BlockingQueue<Runnable> getAllWaitingRunnables() {
        MFConfiguration.getErrorTracker().i(TAG, "getAllWaitingRunnables()");
        return workQueue;
    }

    public void clearUploadQueue() {
        MFConfiguration.getErrorTracker().i(TAG, "clearUploadQueue()");
        boolean isPaused = isPaused();
        if (!isPaused) {
            pause();
        }
        executor.purge();
        if (!isPaused) {
            resume();
        }
    }

    /**
     * adds an UploadItem to the backlog queue.
     * If the UploadItem already exists in the backlog queue then we do not add the item.
     *
     * @param uploadItem The UploadItem to add to the backlog queue.
     */
    public void addUploadRequest(UploadItem uploadItem) {
        MFConfiguration.getErrorTracker().i(TAG, "addUploadRequest()");
        //don't add the item to the backlog queue if it is null or the path is null
        if (uploadItem == null) {
            MFConfiguration.getErrorTracker().i(TAG, "one or more required parameters are invalid, not adding item to queue");
            return;
        }

        if (uploadItem.getFileData() == null
                || uploadItem.getFileData().getFilePath() == null
                || uploadItem.getFileData().getFilePath().isEmpty()
                || uploadItem.getFileData().getFileHash().isEmpty()
                || uploadItem.getFileData().getFileSize() == 0) {
            MFConfiguration.getErrorTracker().i(TAG, "one or more required parameters are invalid, not adding item to queue");
            return;
        }

        if (uploadItem.getUploadAttemptCount() < MAX_UPLOAD_ATTEMPTS) {
            CheckProcess process = new CheckProcess(mediaFire, this, uploadItem);
            executor.execute(process);
        }
    }

    protected void notifyUploadListenerStarted(UploadItem uploadItem) {
        MFConfiguration.getErrorTracker().i(TAG, "notifyUploadListenerStarted()");
        if (uiListener != null) {
            uiListener.onStarted(uploadItem);
        }
    }

    protected void notifyUploadListenerCompleted(UploadItem uploadItem) {
        MFConfiguration.getErrorTracker().i(TAG, "notifyUploadListenerCompleted()");
        if (uiListener != null) {
            uiListener.onCompleted(uploadItem);
        }
    }

    protected void notifyUploadListenerOnProgressUpdate(UploadItem uploadItem, int chunkNumber, int numChunks) {
        MFConfiguration.getErrorTracker().i(TAG, "notifyUploadListenerOnProgressUpdate()");
        if (uiListener != null) {
            uiListener.onProgressUpdate(uploadItem, chunkNumber, numChunks);
        }
    }

    protected void notifyUploadListenerCancelled(UploadItem uploadItem) {
        MFConfiguration.getErrorTracker().i(TAG, "notifyUploadListenerCancelled()");
        if (uiListener != null) {
            uiListener.onCancelled(uploadItem);
        }
    }
}
