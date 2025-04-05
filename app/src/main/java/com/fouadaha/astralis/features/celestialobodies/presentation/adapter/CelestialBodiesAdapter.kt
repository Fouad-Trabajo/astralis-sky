package com.fouadaha.astralis.features.celestialobodies.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.fouadaha.astralis.R
import com.fouadaha.astralis.features.celestialobodies.domain.CelestialBody


class CelestialBodiesAdapter :
    ListAdapter<CelestialBody, CelestialBodiesViewHolder>(CelestialBodyDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CelestialBodiesViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_celestial_body, parent, false)
        return CelestialBodiesViewHolder(view)
    }

    override fun onBindViewHolder(holder: CelestialBodiesViewHolder, position: Int) {
        holder.bind(currentList[position], onClick)
    }

    private lateinit var onClick: (bodyId: String) -> Unit
    fun setEvent(onClick: (bodyId: String) -> Unit) {
        this.onClick = onClick
    }
}


class CelestialBodyDiffUtil : DiffUtil.ItemCallback<CelestialBody>() {
    override fun areItemsTheSame(oldItem: CelestialBody, newItem: CelestialBody): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CelestialBody, newItem: CelestialBody): Boolean {
        return oldItem == newItem
    }


}

