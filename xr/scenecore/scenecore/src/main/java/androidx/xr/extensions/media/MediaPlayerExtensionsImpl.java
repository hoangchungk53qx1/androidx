/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions.media;

import static java.util.Objects.requireNonNull;

import android.media.MediaPlayer;

import androidx.annotation.NonNull;

/** Wraps a {@link com.android.extensions.xr.media.MediaPlayerExtensions}. */
class MediaPlayerExtensionsImpl implements MediaPlayerExtensions {
    @NonNull final com.android.extensions.xr.media.MediaPlayerExtensions mMediaPlayer;

    MediaPlayerExtensionsImpl(
            @NonNull com.android.extensions.xr.media.MediaPlayerExtensions mediaPlayer) {
        requireNonNull(mediaPlayer);
        mMediaPlayer = mediaPlayer;
    }

    @Override
    @NonNull
    public MediaPlayer setPointSourceAttributes(
            MediaPlayer mediaPlayer, PointSourceAttributes attributes) {
        return mMediaPlayer.setPointSourceAttributes(
                mediaPlayer, PointSourceAttributesHelper.convertToFramework(attributes));
    }

    @Override
    @NonNull
    public MediaPlayer setSoundFieldAttributes(
            MediaPlayer mediaPlayer, SoundFieldAttributes attributes) {
        return mMediaPlayer.setSoundFieldAttributes(
                mediaPlayer, SoundFieldAttributesHelper.convertToFramework(attributes));
    }
}
