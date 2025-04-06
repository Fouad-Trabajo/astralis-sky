package com.fouadaha.astralis.features.celestialbodies.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.fouadaha.astralis.R
import com.fouadaha.astralis.features.celestialbodies.domain.CelestialBody


class CelestialBodiesAdapter(private val onClickListener: (CelestialBody) -> Unit) :
    ListAdapter<CelestialBody, CelestialBodiesViewHolder>(CelestialBodyDiffUtil()) {
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


class CelestialBodyDiffUtil : DiffUtil.ItemCallback<CelestialBody>() {
    override fun areItemsTheSame(oldItem: CelestialBody, newItem: CelestialBody): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CelestialBody, newItem: CelestialBody): Boolean {
        return oldItem == newItem
    }
}

