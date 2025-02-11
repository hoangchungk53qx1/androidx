/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.viewer;

import androidx.annotation.RestrictTo;
import androidx.pdf.util.ObservableValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SearchQueryObserver implements ObservableValue.ValueObserver<String> {
    private final PaginatedView mPaginatedView;

    public SearchQueryObserver(@NonNull PaginatedView paginatedView) {
        mPaginatedView = paginatedView;
    }

    @Override
    public void onChange(@Nullable String oldQuery, @Nullable String newQuery) {
        mPaginatedView.clearAllOverlays();
    }
}
