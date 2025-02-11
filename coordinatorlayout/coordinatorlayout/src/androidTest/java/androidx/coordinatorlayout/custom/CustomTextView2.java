/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.coordinatorlayout.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import org.jspecify.annotations.NonNull;

public class CustomTextView2 extends TextView implements
        CoordinatorLayout.AttachedBehavior {
    public CustomTextView2(Context context) {
        super(context);
    }

    public CustomTextView2(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTextView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public CoordinatorLayout.@NonNull Behavior getBehavior() {
        return new TestFloatingBehavior();
    }
}
