package com.puremindit.posPrinter

import androidx.compose.ui.graphics.ImageBitmap

interface Platform {
    val name: String
    fun toImage(byteArray: ByteArray): ImageBitmap
}

expect fun getPlatform(): Platform