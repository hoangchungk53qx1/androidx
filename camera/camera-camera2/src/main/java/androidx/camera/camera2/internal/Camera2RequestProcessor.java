/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureFailure;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.RequestProcessor;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.SessionProcessorSurface;
import androidx.camera.core.impl.TagBundle;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Camera2 implementation of {@link RequestProcessor} which offers the capability of
 * sending capture request by a {@link RequestProcessor.Request} consisting of target output
 * config id, parameters and template.
 *
 * <p>The {@link CaptureSession} is used for sending capture requests.
 * The {@link SessionProcessorSurface} contains the output config id. The target output config ids
 * specified in {@link RequestProcessor.Request}s can be mapped to the target Surface by checking
 * the {@link SessionProcessorSurface#getOutputConfigId()}.
 *
 * <p>{@link #close()} is expected to be called before capture session is closed. All methods will
 * be no-op once {@link #close()} is invoked.
 *
 * <p>This class is thread-safe. It is safe to invoke methods of {@link Camera2RequestProcessor}
 * from any threads.
 */
public class Camera2RequestProcessor implements RequestProcessor {
    private static final String TAG = "Camera2RequestProcessor";
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private @Nullable CaptureSession mCaptureSession;
    @GuardedBy("mLock")
    private @Nullable List<SessionProcessorSurface> mProcessorSurfaces;
    @GuardedBy("mLock")
    private volatile boolean mIsClosed = false;
    @GuardedBy("mLock")
    private volatile @Nullable SessionConfig mSessionConfig;

    public Camera2RequestProcessor(@NonNull CaptureSession captureSession,
            @NonNull List<SessionProcessorSurface> processorSurfaces) {
        Preconditions.checkArgument(captureSession.mState == CaptureSession.State.OPENED,
                "CaptureSession state must be OPENED. Current state:" + captureSession.mState);
        mCaptureSession = captureSession;
        mProcessorSurfaces =  Collections.unmodifiableList(new ArrayList<>(processorSurfaces));
    }

    /**
     * After close(), all submit / setRepeating will be disabled.
     */
    public void close() {
        synchronized (mLock) {
            mIsClosed = true;
            mCaptureSession = null;
            mSessionConfig = null;
            mProcessorSurfaces = null;
        }
    }

    /**
     * Update the SessionConfig to get the tags and the capture callback attached to the
     * repeating request.
     */
    public void updateSessionConfig(@Nullable SessionConfig sessionConfig) {
        synchronized (mLock) {
            mSessionConfig = sessionConfig;
        }
    }

    private boolean areRequestsValid(@NonNull List<RequestProcessor.Request> requests) {
        for (Request request : requests) {
            if (!isRequestValid(request)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRequestValid(RequestProcessor.@NonNull Request request) {
        if (request.getTargetOutputConfigIds().isEmpty()) {
            Logger.e(TAG, "Unable to submit the RequestProcessor.Request: "
                    + "empty targetOutputConfigIds");
            return false;
        }
        for (Integer outputConfigId : request.getTargetOutputConfigIds()) {
            if (findSurface(outputConfigId) == null) {
                Logger.e(TAG, "Unable to submit the RequestProcessor.Request: "
                        + "targetOutputConfigId(" + outputConfigId + ") is not a valid id");
                return false;
            }
        }

        return true;
    }

    @Override
    public int submit(
            RequestProcessor.@NonNull Request request,
            RequestProcessor.@NonNull Callback callback) {
        return submit(Arrays.asList(request), callback);
    }

    @Override
    public int submit(
            @NonNull List<RequestProcessor.Request> requests,
            RequestProcessor.@NonNull Callback callback) {
        synchronized (mLock) {
            if (mIsClosed || !areRequestsValid(requests) || mCaptureSession == null) {
                return -1;
            }

            ArrayList<CaptureConfig> captureConfigs = new ArrayList<>();
            boolean shouldInvokeSequenceCallback = true;
            for (RequestProcessor.Request request : requests) {
                CaptureConfig.Builder builder = new CaptureConfig.Builder();
                builder.setTemplateType(request.getTemplateId());
                builder.setImplementationOptions(request.getParameters());
                builder.addCameraCaptureCallback(
                        CaptureCallbackContainer.create(
                                new Camera2CallbackWrapper(request, callback,
                                        shouldInvokeSequenceCallback)));
                // Only invoke the sequence callback on the first callback wrapper to avoid
                // duplicate calls on this RequestProcessor.Callback.
                shouldInvokeSequenceCallback = false;

                for (Integer outputConfigId : request.getTargetOutputConfigIds()) {
                    builder.addSurface(findSurface(outputConfigId));
                }
                captureConfigs.add(builder.build());
            }
            return mCaptureSession.issueBurstCaptureRequest(captureConfigs);
        }
    }

    @Override
    public int setRepeating(
            RequestProcessor.@NonNull Request request,
            RequestProcessor.@NonNull Callback callback) {
        synchronized (mLock) {
            if (mIsClosed || !isRequestValid(request) || mCaptureSession == null) {
                return -1;
            }

            SessionConfig.Builder sessionConfigBuilder = new SessionConfig.Builder();
            sessionConfigBuilder.setTemplateType(request.getTemplateId());
            sessionConfigBuilder.setImplementationOptions(request.getParameters());
            sessionConfigBuilder.addCameraCaptureCallback(CaptureCallbackContainer.create(
                    new Camera2CallbackWrapper(request, callback, true)));

            if (mSessionConfig != null) {
                // Attach the CameraX camera capture callback so that CameraControl can get the
                // capture results it needs.
                for (CameraCaptureCallback cameraCaptureCallback :
                        mSessionConfig.getRepeatingCameraCaptureCallbacks()) {
                    sessionConfigBuilder.addCameraCaptureCallback(cameraCaptureCallback);
                }

                // Set the tag (key, value) from CameraX.
                TagBundle tagBundle = mSessionConfig.getRepeatingCaptureConfig().getTagBundle();
                for (String key : tagBundle.listKeys()) {
                    sessionConfigBuilder.addTag(key, tagBundle.getTag(key));
                }
            }

            for (Integer outputConfigId : request.getTargetOutputConfigIds()) {
                sessionConfigBuilder.addSurface(findSurface(outputConfigId));
            }

            return mCaptureSession.issueRepeatingCaptureRequests(sessionConfigBuilder.build());
        }
    }

    @Override
    public void abortCaptures() {
        synchronized (mLock) {
            if (mIsClosed || mCaptureSession == null) {
                return;
            }
            mCaptureSession.abortCaptures();
        }
    }

    @Override
    public void stopRepeating() {
        synchronized (mLock) {
            if (mIsClosed || mCaptureSession == null) {
                return;
            }
            mCaptureSession.stopRepeating();
        }
    }

    /**
     * A wrapper for redirect camera2 CameraCaptureSession.CaptureCallback to the
     * {@link RequestProcessor.Callback}. Due to the CaptureSession design, each request has
     * its own CaptureCallback which could lead to onCaptureSequenceCompleted() being called
     * multiple times. Thus the parameter invokeSequenceCallback is added for specifying which
     * CaptureCallback should redirect onCaptureSequenceCompleted and onCaptureSequenceAborted to
     * {@link RequestProcessor.Callback} to avoid duplicate invoking.
     */
    private class Camera2CallbackWrapper extends CameraCaptureSession.CaptureCallback {
        private final RequestProcessor.Callback mCallback;
        private final RequestProcessor.Request mRequest;
        private final boolean mInvokeSequenceCallback;

        Camera2CallbackWrapper(RequestProcessor.@NonNull Request captureRequest,
                RequestProcessor.@NonNull Callback callback, boolean invokeSequenceCallback) {
            mCallback = callback;
            mRequest = captureRequest;
            mInvokeSequenceCallback = invokeSequenceCallback;
        }

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            mCallback.onCaptureStarted(mRequest, frameNumber, timestamp);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            mCallback.onCaptureProgressed(mRequest, new Camera2CameraCaptureResult(partialResult));
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            mCallback.onCaptureCompleted(mRequest, new Camera2CameraCaptureResult(result));
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            mCallback.onCaptureFailed(mRequest, new Camera2CameraCaptureFailure(
                    CameraCaptureFailure.Reason.ERROR, failure));
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session,
                int sequenceId, long frameNumber) {
            if (mInvokeSequenceCallback) {
                mCallback.onCaptureSequenceCompleted(sequenceId, frameNumber);
            }
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session,
                int sequenceId) {
            if (mInvokeSequenceCallback) {
                mCallback.onCaptureSequenceAborted(sequenceId);
            }
        }

        @Override
        public void onCaptureBufferLost(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
            mCallback.onCaptureBufferLost(mRequest, frameNumber,
                    findOutputConfigId(target));
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    int findOutputConfigId(@NonNull Surface surface) {
        synchronized (mLock) {
            if (mProcessorSurfaces == null) {
                return -1;
            }
            for (SessionProcessorSurface sessionProcessorSurface : mProcessorSurfaces) {
                try {
                    if (sessionProcessorSurface.getSurface().get() == surface) {
                        return sessionProcessorSurface.getOutputConfigId();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // This will not happen since SessionProcessorSurface.get() will always
                    // succeed.
                }
            }

            return -1;
        }
    }

    private @Nullable DeferrableSurface findSurface(int outputConfigId) {
        synchronized (mLock) {
            if (mProcessorSurfaces == null) {
                return null;
            }
            for (SessionProcessorSurface sessionProcessorSurface : mProcessorSurfaces) {
                if (sessionProcessorSurface.getOutputConfigId() == outputConfigId) {
                    return sessionProcessorSurface;
                }
            }
            return null;
        }
    }
}
