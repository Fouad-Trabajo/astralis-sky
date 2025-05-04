package com.fouadaha.astralis.features.celestialbodies.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.fouadaha.astralis.R
import com.fouadaha.astralis.core.domain.model.CelestialBodyCore


class CelestialBodiesAdapter(private val onClickListener: (CelestialBodyCore) -> Unit) :
    ListAdapter<CelestialBodyCore, CelestialBodiesViewHolder>(CelestialBodyDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CelestialBodiesViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_celestial_body, parent, false)
        return CelestialBodiesViewHolder(view)
    }

    override fun onBindViewHolder(holder: CelestialBodiesViewHolder, position: Int) {
        val body = getItem(position)
        holder.bind(body, onClickListener)
    }
}


class CelestialBodyDiffUtil : DiffUtil.ItemCallback<CelestialBodyCore>() {
    override fun areItemsTheSame(oldItem: CelestialBodyCore, newItem: CelestialBodyCore): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: CelestialBodyCore,
        newItem: CelestialBodyCore
    ): Boolean {
        return oldItem == newItem
    }
}

