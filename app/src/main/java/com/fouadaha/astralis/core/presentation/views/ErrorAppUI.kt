package com.fouadaha.astralis.core.presentation.views

import android.content.Context
import com.fouadaha.astralis.R


interface ErrorAppUI {
    fun getImageError(): Int
    fun getTitleError(): String
    fun getDescriptionError(): String
    fun getActionRetry(): Unit
}

class ConnectionErrorAppUI(val context: Context, val onClick: (() -> Unit)?) : ErrorAppUI {
    override fun getImageError(): Int {
        return R.drawable.ic_launcher_background
    }

    override fun getTitleError(): String {
        return context.getString(R.string.app_name)
    }

    override fun getDescriptionError(): String {
        return context.getString(R.string.app_name)
    }

    override fun getActionRetry() {
        onClick?.invoke()
    }
}

class ServerErrorAppUI(val context: Context, val onClick: (() -> Unit)?) : ErrorAppUI {
    override fun getImageError(): Int {
        return R.drawable.ic_launcher_background
    }

    override fun getTitleError(): String {
        return context.getString(R.string.app_name)
    }

    override fun getDescriptionError(): String {
        return context.getString(R.string.app_name)
    }

    override fun getActionRetry() {
        onClick?.invoke()
    }
}

class UnknownErrorAppUI(val context: Context, val onClick: (() -> Unit)?) : ErrorAppUI {
    override fun getImageError(): Int {
        return R.drawable.ic_launcher_background
    }

    override fun getTitleError(): String {
        return context.getString(R.string.app_name)
    }

    override fun getDescriptionError(): String {
        return context.getString(R.string.app_name)
    }

    override fun getActionRetry() {
        onClick?.invoke()
    }
}