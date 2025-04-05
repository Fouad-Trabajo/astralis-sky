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

    data object UnknowErrorApp : ErrorApp() {
        private fun readResolve(): Any = UnknowErrorApp
    }
    data object DataExpiredError : ErrorApp() {
        private fun readResolve(): Any = DataExpiredError
    }
}