package com.i69.ui.views

import android.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration


class SimpleDividerItemDecoration(context: Context) : ItemDecoration() {
    private val mDivider: Drawable

    init {
        mDivider = context.getResources().getDrawable(com.i69.R.drawable.chat_divider_line)
    }

   override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child: View = parent.getChildAt(i)
            val params = child.getLayoutParams() as RecyclerView.LayoutParams
            val top: Int = child.getBottom() + params.bottomMargin
            val bottom: Int = top + mDivider.getIntrinsicHeight()
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(c)
        }
    }
}