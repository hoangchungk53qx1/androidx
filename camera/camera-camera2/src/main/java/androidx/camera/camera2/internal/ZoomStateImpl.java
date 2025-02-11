/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.AdapterCameraInfo;

/** An implementation of {@link ZoomState} where the values can be set. */
class ZoomStateImpl implements ZoomState {
    private float mZoomRatio;
    private final float mMaxZoomRatio;
    private final float mMinZoomRatio;
    private float mLinearZoom;

    ZoomStateImpl(float maxZoomRatio, float minZoomRatio) {
        mMaxZoomRatio = maxZoomRatio;
        mMinZoomRatio = minZoomRatio;
    }

    void setZoomRatio(float zoomRatio) throws IllegalArgumentException {
        if (zoomRatio > mMaxZoomRatio || zoomRatio < mMinZoomRatio) {
            String outOfRangeDesc = "Requested zoomRatio " + zoomRatio + " is not within valid "
                    + "range [" + mMinZoomRatio + " , "
                    + mMaxZoomRatio + "]";

            throw new IllegalArgumentException(outOfRangeDesc);
        }
        mZoomRatio = zoomRatio;
        mLinearZoom = AdapterCameraInfo.getPercentageByRatio(
                mZoomRatio, mMinZoomRatio, mMaxZoomRatio);
    }

    void setLinearZoom(float linearZoom) throws IllegalArgumentException {
        if (linearZoom > 1.0f || linearZoom < 0f) {
            String outOfRangeDesc = "Requested linearZoom " + linearZoom + " is not within"
                    + " valid range [0..1]";
            throw new IllegalArgumentException(outOfRangeDesc);
        }
        mLinearZoom = linearZoom;
        mZoomRatio = AdapterCameraInfo.getZoomRatioByPercentage(
                mLinearZoom, mMinZoomRatio, mMaxZoomRatio);
    }

    @Override
    public float getZoomRatio() {
        return mZoomRatio;
    }

    @Override
    public float getMaxZoomRatio() {
        return mMaxZoomRatio;
    }

    @Override
    public float getMinZoomRatio() {
        return mMinZoomRatio;
    }

    @Override
    public float getLinearZoom() {
        return mLinearZoom;
    }
}
