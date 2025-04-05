package com.fouadaha.astralis.features.celestialobodies.presentation.adapter


import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.fouadaha.astralis.databinding.ItemCelestialBodyBinding
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBody

class CelestialBodiesViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private lateinit var binding: ItemCelestialBodyBinding

    fun bind(body: CelestialBody, onClick: (bodyId: String) -> Unit) {
        binding = ItemCelestialBodyBinding.bind(view)
        binding.apply {
            nameBodyItem.text = body.name
            imageBodyItem.load(body.imageUrl)
            view.setOnClickListener {
                onClick(body.id)
            }
        }
    }


}