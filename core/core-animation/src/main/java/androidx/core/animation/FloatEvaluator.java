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

package androidx.core.animation;

import android.annotation.SuppressLint;

import org.jspecify.annotations.NonNull;

/**
 * This evaluator can be used to perform type interpolation between <code>float</code> values.
 */
public final class FloatEvaluator implements TypeEvaluator<Float> {

    private static final FloatEvaluator sInstance = new FloatEvaluator();

    /**
     * Returns an instance of <code>FloatEvaluator</code> that may be used in
     * {@link ValueAnimator#setEvaluator(TypeEvaluator)}. The same instance may
     * be used in multiple <code>Animator</code>s because it holds no state.
     * @return An instance of <code>FloatEvaluator</code>.
     */
    public static @NonNull FloatEvaluator getInstance() {
        return sInstance;
    }

    private FloatEvaluator() {
    }

    /**
     * This function returns the result of linearly interpolating the start and end values, with
     * <code>fraction</code> representing the proportion between the start and end values. The
     * calculation is a simple parametric calculation: <code>result = x0 + t * (v1 - v0)</code>,
     * where <code>x0</code> is <code>startValue</code>, <code>x1</code> is <code>endValue</code>,
     * and <code>t</code> is <code>fraction</code>.
     *
     * @param fraction   The fraction from the starting to the ending values
     * @param startValue The start value; should be of type <code>float</code> or
     *                   <code>Float</code>
     * @param endValue   The end value; should be of type <code>float</code> or <code>Float</code>
     * @return A linear interpolation between the start and end values, given the
     *         <code>fraction</code> parameter.
     */
    @SuppressLint("AutoBoxing") /* Generics */
    @Override
    public @NonNull Float evaluate(
            float fraction,
            @SuppressLint("AutoBoxing") @NonNull Float startValue,
            @SuppressLint("AutoBoxing") @NonNull Float endValue
    ) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }
}
