package com.fouadaha.astralis.core.domain

sealed class ErrorApp : Throwable() {
    data object InternetErrorApp : ErrorApp() {
        private fun readResolve(): Any = InternetErrorApp
    }

    data object ServerErrorApp : ErrorApp() {
        private fun readResolve(): Any = ServerErrorApp
    }

    data object DataErrorApp : ErrorApp() {
        private fun readResolve(): Any = DataErrorApp
    }

    data object UnknownErrorApp : ErrorApp() {
        private fun readResolve(): Any = UnknownErrorApp
    }
    data object DataExpiredError : ErrorApp() {
        private fun readResolve(): Any = DataExpiredError
    }
}