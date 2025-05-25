package com.fouadaha.astralis.features.celestialbodies.presentation.adapter


import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.fouadaha.astralis.core.domain.model.CelestialBody
import com.fouadaha.astralis.databinding.ItemCelestialBodyBinding

class CelestialBodiesViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private lateinit var binding: ItemCelestialBodyBinding

    fun bind(body: CelestialBody, onClickListener: (CelestialBody) -> Unit) {
        binding = ItemCelestialBodyBinding.bind(view)
        binding.apply {
            imageBodyItem.load(body.imageUrl)
            nameBodyItem.text = body.name
            view.setOnClickListener {
                onClickListener(body)
            }
        }
    }
}