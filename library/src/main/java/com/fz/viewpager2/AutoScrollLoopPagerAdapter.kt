package com.fz.viewpager2

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup

internal class AutoScrollLoopPagerAdapter<VH : RecyclerView.ViewHolder?>(var adapter: RecyclerView.Adapter<VH>) :
    RecyclerView.Adapter<VH>() {
    override fun getItemCount(): Int {
        val itemCount = realItemCount
        return if (itemCount > 1) itemCount + 2 else itemCount
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        adapter.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        adapter.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: VH) {
       adapter.onViewAttachedToWindow(holder)
    }

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(hasStableIds)
        adapter.setHasStableIds(hasStableIds)
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        adapter.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: VH): Boolean {
        return adapter.onFailedToRecycleView(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
       adapter.onViewDetachedFromWindow(holder)
    }

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.registerAdapterDataObserver(observer)
        adapter.registerAdapterDataObserver(observer)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        adapter.unregisterAdapterDataObserver(observer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return adapter.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        adapter.onBindViewHolder(holder, getRealPosition(position))
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        adapter.onBindViewHolder(holder, getRealPosition(position), payloads)
    }

    override fun getItemViewType(position: Int): Int {
        return adapter.getItemViewType(getRealPosition(position))
    }

    override fun getItemId(position: Int): Long {
        return adapter.getItemId(getRealPosition(position))
    }

    val realItemCount: Int
        get() = adapter.itemCount

    fun getRealPosition(position: Int): Int {
        return getRealPosition(position, realItemCount)
    }

    fun getRealPosition(position: Int, realItemCount: Int): Int {
        return when (position) {
            0 -> {
                realItemCount - 1
            }
            realItemCount + 1 -> {
                0
            }
            else -> {
                position - 1
            }
        }
    }
}