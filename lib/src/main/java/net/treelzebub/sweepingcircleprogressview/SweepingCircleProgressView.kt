package net.treelzebub.sweepingcircleprogressview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * Created by Tre Murillo on 12/21/16
 */
class SweepingCircleProgressView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : EquilateralView(context, attrs, defStyle) {

    private val INDETERMINANT_MIN_SWEEP = 15f
    private val ANIM_DUR = 4000L
    private val ANIM_SWEEP_DUR = 5000L
    private val ANIM_SYNC_DUR = 500L
    private val ANIM_STEPS = 3
    private val INIT_ANGLE = 0f

    interface Listener {
        fun onModeChanged(isIndeterminate: Boolean) {}
        fun onAnimationReset() {}
        fun onProgressUpdate(progress: Float) {}
        fun onProgressEnd(progress: Float) {}
    }

    private val paint: Paint
    private var size = 0
    private val bounds: RectF
    private val thickness: Float
    private val color: Int

    private var currentProgress: Float
    private val maxProgress: Float
    private val autostartAnimation: Boolean

    private var indeterminateSweep = 0f
    private var indeterminateRotateOffset = 0f
    private var startAngle = 0f
    private var actualProgress = 0f

    private var startAngleRotate: ValueAnimator? = null
    private var progressAnimator: ValueAnimator? = null
    private var indeterminateAnimator: AnimatorSet? = null

    private val listeners = mutableListOf<Listener>()

    var isIndeterminate: Boolean = false
        set(value) {
            val reset = field == value
            field = value
            if (reset) {
                resetAnimation()
            } else {
                listeners.forEach { it.onModeChanged(isIndeterminate) }
            }
        }

    var progress: Float
        @Synchronized get() = currentProgress
        @Synchronized set(value) {
            this.currentProgress = value
            if (!isIndeterminate) {
                if (progressAnimator?.isRunning.orFalse()) progressAnimator!!.cancel()
                progressAnimator = ValueAnimator.ofFloat(actualProgress, currentProgress)
                progressAnimator!!.duration = ANIM_SYNC_DUR
                progressAnimator!!.interpolator = LinearInterpolator()
                progressAnimator!!.addUpdateListener {
                    actualProgress = it.animatedValue as Float
                    invalidate()
                }
                progressAnimator!!.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        listeners.forEach { it.onProgressEnd(currentProgress) }
                    }
                })
                progressAnimator!!.start()
            }
            invalidate()
            listeners.forEach { it.onProgressUpdate(currentProgress) }
        }

    init {
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.SweepingCircleProgressView, defStyle, 0)
        color               = styledAttrs.getColor(R.styleable.SweepingCircleProgressView_progressColor, Color.BLACK)
        currentProgress     = styledAttrs.getFloat(R.styleable.SweepingCircleProgressView_progress, 0f)
        maxProgress         = styledAttrs.getFloat(R.styleable.SweepingCircleProgressView_maxProgress, 100f)
        thickness           = styledAttrs.getDimension(R.styleable.SweepingCircleProgressView_stroke, 8f)
        isIndeterminate     = styledAttrs.getBoolean(R.styleable.SweepingCircleProgressView_isIndeterminate, false)
        autostartAnimation  = styledAttrs.getBoolean(R.styleable.SweepingCircleProgressView_autostart, isIndeterminate)
        styledAttrs.recycle()

        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        updatePaint()
        bounds = RectF()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val xPad   = paddingLeft + paddingRight
        val yPad   = paddingTop + paddingBottom
        val width  = measuredWidth - xPad
        val height = measuredHeight - yPad
        size = if (width < height) width else height
        setMeasuredDimension(size + xPad, size + yPad)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        size = if (w < h) w else h
        updateBounds()
    }

    private fun updateBounds() {
        val _paddingLeft = paddingLeft
        val _paddingTop  = paddingTop
        bounds.set(_paddingLeft + thickness, _paddingTop + thickness, size - _paddingLeft - thickness, size - _paddingTop - thickness)
    }

    private fun updatePaint() {
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = thickness
        paint.strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sweepAngle = actualProgress / maxProgress * 360
        if (isIndeterminate) {
            canvas.drawArc(bounds, startAngle + indeterminateRotateOffset, indeterminateSweep, false, paint)
        } else {
            canvas.drawArc(bounds, startAngle, sweepAngle, false, paint)
        }
    }

    fun listen(listener: Listener) = listeners.add(listener)

    fun unlisten(listener: Listener) = listeners.remove(listener)

    fun startAnimation() = resetAnimation()

    fun resetAnimation() {
        if (startAngleRotate?.isRunning.orFalse()) startAngleRotate!!.cancel()
        if (progressAnimator?.isRunning.orFalse()) progressAnimator!!.cancel()
        if (indeterminateAnimator?.isRunning.orFalse()) indeterminateAnimator!!.cancel()

        if (isIndeterminate) {
            indeterminateSweep = INDETERMINANT_MIN_SWEEP
            indeterminateAnimator = AnimatorSet()
            var prevSet: AnimatorSet? = null
            var nextSet: AnimatorSet
            for (k in 0..ANIM_STEPS - 1) {
                nextSet = createIndeterminateAnimator(k.toFloat())
                val builder = indeterminateAnimator!!.play(nextSet)
                if (prevSet != null) builder.after(prevSet)
                prevSet = nextSet
            }

            // Infinitely loop
            indeterminateAnimator!!.addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }
                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) resetAnimation()
                }
            })
            indeterminateAnimator!!.start()
            listeners.forEach { it.onAnimationReset() }
        } else {
            startAngle = INIT_ANGLE
            startAngleRotate = ValueAnimator.ofFloat(startAngle, startAngle + 360)
            startAngleRotate!!.duration = ANIM_SWEEP_DUR
            startAngleRotate!!.interpolator = DecelerateInterpolator(2f)
            startAngleRotate!!.addUpdateListener {
                startAngle = it.animatedValue as Float
                invalidate()
            }
            startAngleRotate!!.start()

            actualProgress = 0f
            progressAnimator = ValueAnimator.ofFloat(actualProgress, currentProgress)
            progressAnimator!!.duration = ANIM_SYNC_DUR
            progressAnimator!!.interpolator = LinearInterpolator()
            progressAnimator!!.addUpdateListener {
                actualProgress = it.animatedValue as Float
                invalidate()
            }
            progressAnimator!!.start()
        }
    }

    fun stopAnimation() {
        startAngleRotate?.cancel()
        startAngleRotate = null
        progressAnimator?.cancel()
        progressAnimator = null
        indeterminateAnimator?.cancel()
        indeterminateAnimator = null
    }

    private fun createIndeterminateAnimator(step: Float): AnimatorSet {
        val maxSweep = 360f * (ANIM_STEPS - 1) / ANIM_STEPS + INDETERMINANT_MIN_SWEEP
        val start = -90f + step * (maxSweep - INDETERMINANT_MIN_SWEEP)

        // Sweep the front of the arc... pew! pew! pew!
        val frontEndExtend = ValueAnimator.ofFloat(INDETERMINANT_MIN_SWEEP, maxSweep)
        frontEndExtend.duration = ANIM_DUR / ANIM_STEPS / 2
        frontEndExtend.interpolator = DecelerateInterpolator(1f)
        frontEndExtend.addUpdateListener {
            indeterminateSweep = it.animatedValue as Float
            invalidate()
        }

        val rotateAnimator1 = ValueAnimator.ofFloat(step * 720f / ANIM_STEPS, (step + .5f) * 720f / ANIM_STEPS)
        rotateAnimator1.duration = ANIM_DUR / ANIM_STEPS / 2
        rotateAnimator1.interpolator = LinearInterpolator()
        rotateAnimator1.addUpdateListener {
            indeterminateRotateOffset = it.animatedValue as Float
        }

        // Retract the back end of the arc
        val backEndRetract = ValueAnimator.ofFloat(start, start + maxSweep - INDETERMINANT_MIN_SWEEP)
        backEndRetract.duration = ANIM_DUR / ANIM_STEPS / 2
        backEndRetract.interpolator = DecelerateInterpolator(1f)
        backEndRetract.addUpdateListener {
            startAngle = it.animatedValue as Float
            indeterminateSweep = maxSweep - startAngle + start
            invalidate()
        }

        val rotateAnimator2 = ValueAnimator.ofFloat((step + .5f) * 720f / ANIM_STEPS, (step + 1) * 720f / ANIM_STEPS)
        rotateAnimator2.duration = ANIM_DUR / ANIM_STEPS / 2
        rotateAnimator2.interpolator = LinearInterpolator()
        rotateAnimator2.addUpdateListener {
            indeterminateRotateOffset = it.animatedValue as Float
        }

        val set = AnimatorSet()
        set.play(frontEndExtend).with(rotateAnimator1)
        set.play(backEndRetract).with(rotateAnimator2).after(rotateAnimator1)
        return set
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autostartAnimation) startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun setVisibility(visibility: Int) {
        val currentVisibility = getVisibility()
        super.setVisibility(visibility)
        if (visibility != currentVisibility) {
            if (visibility == View.VISIBLE) {
                resetAnimation()
            } else if (visibility == View.GONE || visibility == View.INVISIBLE) {
                stopAnimation()
            }
        }
    }
}