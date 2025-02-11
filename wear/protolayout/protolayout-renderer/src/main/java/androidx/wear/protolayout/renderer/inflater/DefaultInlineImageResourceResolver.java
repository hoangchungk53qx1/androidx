/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.wear.protolayout.proto.ResourceProto.ImageFormat;
import androidx.wear.protolayout.proto.ResourceProto.InlineImageResource;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.InlineImageResourceResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.ResourceAccessException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/** Resource resolver for inline resources. */
public class DefaultInlineImageResourceResolver implements InlineImageResourceResolver {
    private static final String TAG = "InlineImageResolver";

    private final @NonNull Context mAppContext;

    /** Constructor. */
    public DefaultInlineImageResourceResolver(@NonNull Context appContext) {
        this.mAppContext = appContext;
    }

    @Override
    public @NonNull Drawable getDrawableOrThrow(@NonNull InlineImageResource inlineImage)
            throws ResourceAccessException {
        Bitmap bitmap = null;

        if (inlineImage.getFormat() == ImageFormat.IMAGE_FORMAT_RGB_565
                || inlineImage.getFormat() == ImageFormat.IMAGE_FORMAT_ARGB_8888) {
            bitmap = loadRawBitmap(inlineImage);
        } else if (inlineImage.getFormat() == ImageFormat.IMAGE_FORMAT_UNDEFINED) {
            bitmap = loadStructuredBitmap(inlineImage);
        }

        if (bitmap == null) {
            throw new ResourceAccessException("Unsupported image format in image resource.");
        }

        // The app Context is correct here, as it's just used for display density, so it doesn't
        // depend on anything from the provider app.
        return new BitmapDrawable(mAppContext.getResources(), bitmap);
    }

    private static @Nullable Config imageFormatToBitmapConfig(ImageFormat imageFormat) {
        switch (imageFormat) {
            case IMAGE_FORMAT_RGB_565:
                return Config.RGB_565;
            case IMAGE_FORMAT_ARGB_8888:
                return Config.ARGB_8888;
            case IMAGE_FORMAT_UNDEFINED:
            case UNRECOGNIZED:
                return null;
        }
        return null;
    }

    private int getBytesPerPixel(Config config) {
        if (config == Config.RGB_565) {
            return 2;
        } else if (config == Config.ARGB_8888) {
            return 4;
        }
        return -1;
    }

    private @NonNull Bitmap loadRawBitmap(@NonNull InlineImageResource inlineImage)
            throws ResourceAccessException {
        Config config = imageFormatToBitmapConfig(inlineImage.getFormat());

        if (config == null) {
            throw new ResourceAccessException("Unknown image format in image resource.");
        }

        int widthPx = inlineImage.getWidthPx();
        int heightPx = inlineImage.getHeightPx();

        int bytesPerPixel = getBytesPerPixel(config);
        int expectedDataSize = widthPx * heightPx * bytesPerPixel;
        if (inlineImage.getData().size() != expectedDataSize) {
            throw new ResourceAccessException(
                    "Mismatch between image data size and dimensions in image resource.");
        }

        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, config);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(inlineImage.getData().toByteArray()));

        return bitmap;
    }

    private @Nullable Bitmap loadStructuredBitmap(@NonNull InlineImageResource inlineImage) {
        Bitmap bitmap =
                BitmapFactory.decodeByteArray(
                        inlineImage.getData().toByteArray(), 0, inlineImage.getData().size());
        if (bitmap == null) {
            Log.e(TAG, "Unable to load structured bitmap.");
            return null;
        }
        return Bitmap.createScaledBitmap(
                bitmap, inlineImage.getWidthPx(), inlineImage.getHeightPx(), /* filter= */ true);
    }
}
