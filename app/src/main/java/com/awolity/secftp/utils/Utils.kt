package com.awolity.secftp.utils

import android.graphics.drawable.Drawable
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import java.text.SimpleDateFormat
import java.util.*

fun epochToDateTime(epoch: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd - HH:mm:ss")
    return sdf.format(Date(epoch))
}

fun humanReadableByteCount(bytes: Long, si: Boolean): String {
    val unit = if (si) 1000 else 1024
    if (bytes < unit) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}

fun getInitial(text: String): Drawable {
    val generator = ColorGenerator.MATERIAL
    val firstLetter = text.substring(0, 1)
    return TextDrawable.builder().buildRound(firstLetter, generator.getColor(text))
}

fun getInitial(firstLetter: String, colorBase: String, widthInPixels: Int): Drawable {
    val generator = ColorGenerator.MATERIAL
    return TextDrawable.builder()
        .beginConfig()
        .width(widthInPixels)  // width in px
        .height(widthInPixels) // height in px
        .endConfig()
        .buildRound(firstLetter, generator.getColor(colorBase))
}


