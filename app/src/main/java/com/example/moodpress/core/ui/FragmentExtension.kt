package com.example.moodpress.core.ui

import androidx.fragment.app.Fragment
import com.example.moodpress.MainActivity

fun Fragment.showLoading(message: String = "Đang xử lý...") {
    (activity as? MainActivity)?.showLoading(message)
}

fun Fragment.hideLoading() {
    (activity as? MainActivity)?.hideLoading()
}