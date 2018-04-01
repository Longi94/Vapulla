package `in`.dragonbra.vapulla.view

import `in`.dragonbra.vapulla.extension.maxLineWidth
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView

class ChatLayout : RelativeLayout {
    private lateinit var viewPartMain: TextView
    private lateinit var viewPartSlave: View

    private var viewPartSlaveWidth: Int = 0
    private var viewPartSlaveHeight: Int = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (childCount != 2) {
            throw IllegalStateException("there must be 2 children views")
        }

        if (getChildAt(0) !is TextView) {
            throw IllegalStateException("first child must be a TextView element")
        }

        viewPartMain = getChildAt(0) as TextView
        viewPartSlave = getChildAt(1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var widthSize = View.MeasureSpec.getSize(widthMeasureSpec)

        if (widthSize <= 0) {
            return
        }

        val availableWidth = widthSize - paddingLeft - paddingRight
        //val availableHeight = heightSize - paddingTop - paddingBottom

        val viewPartMainLayoutParams = viewPartMain.layoutParams as RelativeLayout.LayoutParams
        val viewPartMainWidth = viewPartMain.maxLineWidth().toInt() + viewPartMainLayoutParams.leftMargin + viewPartMainLayoutParams.rightMargin
        val viewPartMainHeight = viewPartMain.measuredHeight + viewPartMainLayoutParams.topMargin + viewPartMainLayoutParams.bottomMargin

        val viewPartSlaveLayoutParams = viewPartSlave.layoutParams as RelativeLayout.LayoutParams
        viewPartSlaveWidth = viewPartSlave.measuredWidth + viewPartSlaveLayoutParams.leftMargin + viewPartSlaveLayoutParams.rightMargin
        viewPartSlaveHeight = viewPartSlave.measuredHeight + viewPartSlaveLayoutParams.topMargin + viewPartSlaveLayoutParams.bottomMargin

        val viewPartMainLineCount = viewPartMain.lineCount
        val viewPartMainLastLineWidth = if (viewPartMainLineCount > 0) viewPartMain.layout.getLineWidth(viewPartMainLineCount - 1) else 0.0f

        widthSize = paddingLeft + paddingRight
        var heightSize = paddingTop + paddingBottom + viewPartMainHeight

        if (viewPartMainLineCount > 1 && viewPartMainLastLineWidth + viewPartSlaveWidth < viewPartMain.measuredWidth) {
            widthSize += viewPartMainWidth
        } else if (viewPartMainLineCount > 1 && viewPartMainLastLineWidth + viewPartSlaveWidth >= availableWidth) {
            widthSize += viewPartMainWidth
            heightSize += + viewPartSlaveHeight
        } else if (viewPartMainLineCount == 1 && viewPartMainWidth + viewPartSlaveWidth >= availableWidth) {
            widthSize += viewPartMain.measuredWidth
            heightSize += + viewPartSlaveHeight
        } else {
            widthSize += viewPartMainWidth + viewPartSlaveWidth
        }

        setMeasuredDimension(widthSize, heightSize)
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(heightSize, View.MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        viewPartMain.layout(
                paddingLeft,
                paddingTop,
                viewPartMain.width + paddingLeft,
                viewPartMain.height + paddingTop)

        viewPartSlave.layout(
                right - left - viewPartSlaveWidth - paddingRight,
                bottom - top - paddingBottom - viewPartSlaveHeight,
                right - left - paddingRight,
                bottom - top - paddingBottom)
    }
}