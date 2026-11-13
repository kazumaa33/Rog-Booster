package com.rogbooster.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rogbooster.app.databinding.ItemGameBinding
import com.rogbooster.app.model.GameApp

class GameAdapter(private val onLaunch: (GameApp) -> Unit) :
    RecyclerView.Adapter<GameAdapter.VH>() {

    private var items: List<GameApp> = emptyList()

    fun submit(list: List<GameApp>) { items = list; notifyDataSetChanged() }

    class VH(val b: ItemGameBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemGameBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val g = items[i]
        h.b.imgIcon.setImageDrawable(g.icon)
        h.b.tvName.text = g.name
        h.b.tvPkg.text = g.packageName
        h.b.btnLaunch.setOnClickListener { onLaunch(g) }
    }

    override fun getItemCount() = items.size
}
