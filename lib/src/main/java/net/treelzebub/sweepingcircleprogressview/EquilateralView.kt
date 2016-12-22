package net.treelzebub.sweepingcircleprogressview

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Created by Tre Murillo on 12/21/16
 *
 * Port from original Java by Aaron Sarazan (https://github.com/asarazan)
 */
open class EquilateralView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
): View(context, attrs, defStyle) {

    protected fun getDimension() = Math.min(width, height)

    /**
     * This view insists on a square layout, so it will always take the lesser of the two dimensions.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = View.MeasureSpec.getMode(widthMeasureSpec)
        var w     = View.MeasureSpec.getSize(widthMeasureSpec)
        val hMode = View.MeasureSpec.getMode(heightMeasureSpec)
        var h     = View.MeasureSpec.getSize(heightMeasureSpec)

        val dim = Math.min(
                if (wMode == View.MeasureSpec.UNSPECIFIED) Integer.MAX_VALUE else w,
                if (hMode == View.MeasureSpec.UNSPECIFIED) Integer.MAX_VALUE else h)
        w = dim
        h = dim
        setMeasuredDimension(w, h)
    }
}