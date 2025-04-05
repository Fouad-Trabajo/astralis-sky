package com.fouadaha.astralis.core.presentation.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.fouadaha.astralis.core.presentation.hide
import com.fouadaha.astralis.core.presentation.visible
import com.fouadaha.astralis.databinding.ViewErrorBinding


class ErrorAppView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {


    private val binding = ViewErrorBinding
        .inflate(LayoutInflater.from(context), this, true)

    init {
        hide()
    }

    fun render(errorAppUI: ErrorAppUI) {
        binding.apply {
            imageError.setImageResource(errorAppUI.getImageError())
            titleError.text = errorAppUI.getTitleError()
            descriptionError.text = errorAppUI.getDescriptionError()
            buttonRetryError.setOnClickListener {
                errorAppUI.getActionRetry()
            }
            visible()
        }
    }
}