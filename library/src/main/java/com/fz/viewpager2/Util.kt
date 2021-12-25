package com.fz.viewpager2

import android.view.View

fun View?.dip2px(dpValue: Float): Float {
    val scale: Float = this?.context?.resources?.displayMetrics?.density ?: 0f
    return (dpValue * scale + 0.5f)
}