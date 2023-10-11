/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import org.joda.time.field.FieldUtils

internal actual fun safeMultiply(a: Long, b: Long): Long = FieldUtils.safeMultiply(a, b)
internal actual fun safeMultiply(a: Int, b: Int): Int = FieldUtils.safeMultiply(a, b)
internal actual fun safeAdd(a: Int, b: Int): Int = FieldUtils.safeAdd(a, b)
internal actual fun safeAdd(a: Long, b: Long): Long = FieldUtils.safeAdd(a, b)