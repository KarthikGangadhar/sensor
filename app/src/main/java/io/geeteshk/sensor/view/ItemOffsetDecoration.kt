package io.geeteshk.sensor.view

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

class ItemOffsetDecoration(offset: Int) : RecyclerView.ItemDecoration() {

    private var itemOffset = offset

    constructor(context: Context, itemOffsetId: Int)
            : this(context.resources.getDimensionPixelSize(itemOffsetId))

    override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect!!.set(0, 0, 0, itemOffset)
    }
}