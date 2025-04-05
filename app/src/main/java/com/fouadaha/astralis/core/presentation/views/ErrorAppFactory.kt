package com.fouadaha.astralis.core.presentation.views

import android.content.Context
import com.fouadaha.astralis.core.domain.ErrorApp
import org.koin.core.annotation.Single

@Single
class ErrorAppFactory(val context: Context) {

    fun build(errorApp: ErrorApp, onClick: (() -> Unit)): ErrorAppUI {
        return when (errorApp) {
            ErrorApp.DataErrorApp -> ServerErrorAppUI(context, onClick)
            ErrorApp.DataExpiredError -> ConnectionErrorAppUI(context, onClick)
            ErrorApp.InternetErrorApp -> ConnectionErrorAppUI(context, onClick)
            ErrorApp.ServerErrorApp -> ServerErrorAppUI(context, onClick)
            ErrorApp.UnknownErrorApp -> UnknownErrorAppUI(context, onClick)
        }
    }
}