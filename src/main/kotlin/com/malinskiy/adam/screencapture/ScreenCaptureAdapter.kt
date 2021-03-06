/*
 * Copyright (C) 2020 Anton Malinskiy
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

package com.malinskiy.adam.screencapture

import com.malinskiy.adam.transport.AndroidReadChannel
import io.ktor.utils.io.*

/**
 * If you reuse the adapter per device - you automatically recycle the internal buffer
 *
 *
 * @param buffer consider reusing the buffer between screencapture requests to reduce heap memory pressure
 * @param colorModelFactory reuse the color models required for image conversion
 */
abstract class ScreenCaptureAdapter<T>(
    private var buffer: ByteArray? = null,
    protected val colorModelFactory: ColorModelFactory = ColorModelFactory()
) {
    suspend fun read(channel: ByteReadChannel, size: Int): ByteArray {
        val localBuffer = buffer

        val imageBuffer = if (localBuffer != null && localBuffer.size == size) {
            localBuffer
        } else {
            ByteArray(size)
        }

        channel.readFully(imageBuffer)
        return imageBuffer
    }

    abstract suspend fun process(
        version: Int,
        bitsPerPixel: Int,
        size: Int,
        width: Int,
        height: Int,
        redOffset: Int,
        redLength: Int,
        greenOffset: Int,
        greenLength: Int,
        blueOffset: Int,
        blueLength: Int,
        alphaOffset: Int,
        alphaLength: Int,
        colorSpace: ColorSpace? = null,
        channel: AndroidReadChannel
    ): T
}
