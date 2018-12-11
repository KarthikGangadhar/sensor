package io.geeteshk.sensor.view.ui.table

import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.geeteshk.sensor.R
import kotlinx.android.synthetic.main.item_row.view.*

class TableAdapter(private var map: Map<Float, Float>) : RecyclerView.Adapter<TableAdapter.RowHolder>() {

    private val keys = map.keys.toFloatArray()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val rootView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_row, parent, false)
        return RowHolder(rootView)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        if (position == 0) {
            holder.setupHeader()
        } else {
            holder.setupRow(keys[position - 1].toInt(), map[keys[position - 1]]!!)
        }
    }

    override fun getItemCount() = map.size + 1

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    class RowHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun setupHeader() {
            itemView.xView.text = "Time (s)"
            itemView.xView.typeface = Typeface.DEFAULT_BOLD

            itemView.yView.text = "Resistance (Kohm)"
            itemView.yView.typeface = Typeface.DEFAULT_BOLD
        }

        fun setupRow(x: Int, y: Float) {
            itemView.xView.text = x.toString()
            itemView.yView.text = y.toString()
        }
    }
}