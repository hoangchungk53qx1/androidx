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

package androidx.kruth

import kotlin.test.assertEquals
import kotlin.test.fail

internal fun assertFailsWithMessage(message: String, block: () -> Unit) {
    try {
        block()
        fail("Expected to fail but didn't")
    } catch (e: AssertionError) {
        assertEquals(expected = message, actual = e.message)
    }
}

internal expect fun Float.nextUp(): Float

internal expect fun Float.nextDown(): Float

internal inline fun <reified E : Throwable> assertFailsWith(
    assert: (E) -> Unit,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: Throwable) {
        if (e::class == E::class) {
            assert(e as E)
        } else {
            throw e
        }
    }
}

/**
 * Runs the given block only if we are not on JS. This is used to skip certain test blocks on JS
 * when they cannot be supported.
 *
 * @see https://youtrack.jetbrains.com/issue/KT-73309/number-types-in-kotlin-js
 */
internal expect inline fun assumeNotJs(block: () -> Unit)
