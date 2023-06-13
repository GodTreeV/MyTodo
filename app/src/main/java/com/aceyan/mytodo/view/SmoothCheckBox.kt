package com.aceyan.mytodo.view

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.LinearInterpolator
import android.widget.Checkable
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.aceyan.mytodo.R

class SmoothCheckBox : View, Checkable {
    private var mPaint: Paint? = null
    private var mTickPaint: Paint? = null
    private var mFloorPaint: Paint? = null
    private var mTickPoints: MutableList<Point> = mutableListOf()
    private var mCenterPoint: Point? = null
    private var mTickPath: Path? = null
    private var mLeftLineDistance = 0f
    private var mRightLineDistance = 0f
    private var mDrewDistance = 0f
    private var mScaleVal = 1.0f
    private var mFloorScale = 1.0f
    private var mWidth = 0
    private var mAnimDuration = 0
    private var mStrokeWidth = 0
    private var mCheckedColor = 0
    private var mUnCheckedColor = 0
    private var mFloorColor = 0
    private var mFloorUnCheckedColor = 0
    private var mChecked = false
    private var mTickDrawing = false
    private var mListener: OnCheckedChangeListener? = null

    var animationChangeListener: OnAnimationChangeListener? = null

    @JvmOverloads
    constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SmoothCheckBox)
        val tickColor = ta.getColor(R.styleable.SmoothCheckBox_color_tick, COLOR_TICK)
        mAnimDuration = ta.getInt(R.styleable.SmoothCheckBox_duration, DEF_ANIM_DURATION)
        mFloorColor =
            ta.getColor(R.styleable.SmoothCheckBox_color_unchecked_stroke, COLOR_FLOOR_UNCHECKED)
        mCheckedColor = ta.getColor(R.styleable.SmoothCheckBox_color_checked, COLOR_CHECKED)
        mUnCheckedColor = ta.getColor(R.styleable.SmoothCheckBox_color_unchecked, COLOR_UNCHECKED)
        mStrokeWidth = ta.getDimensionPixelSize(
            R.styleable.SmoothCheckBox_stroke_width, dp2px(
                context, 0f
            )
        )
        ta.recycle()
        mFloorUnCheckedColor = mFloorColor
        mTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTickPaint!!.style = Paint.Style.STROKE
        mTickPaint!!.strokeCap = Paint.Cap.ROUND
        mTickPaint!!.color = tickColor
        mFloorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mFloorPaint!!.style = Paint.Style.FILL
        mFloorPaint!!.color = mFloorColor
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint!!.style = Paint.Style.FILL
        mPaint!!.color = mCheckedColor
        mTickPath = Path()
        mCenterPoint = Point()
        mTickPoints.add(Point())
        mTickPoints.add(Point())
        mTickPoints.add(Point())
        setOnClickListener(OnClickListener {
            toggle()
            mTickDrawing = false
            mDrewDistance = 0f
            if (isChecked) {
                startCheckedAnimation()
            } else {
                startUnCheckedAnimation()
            }
        })
    }

    private fun dp2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(KEY_INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putBoolean(KEY_INSTANCE_STATE, isChecked)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            val bundle = state
            val isChecked = bundle.getBoolean(KEY_INSTANCE_STATE)
            setChecked(isChecked)
            super.onRestoreInstanceState(bundle.getParcelable(KEY_INSTANCE_STATE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun toggle() {
        this.isChecked = !isChecked
    }

    override fun setChecked(checked: Boolean) {
        mChecked = checked
        reset()
        invalidate()
        if (mListener != null) {
            mListener!!.onCheckedChanged(this@SmoothCheckBox, mChecked)
        }
    }

    /**
     * checked with animation
     *
     * @param checked checked
     * @param animate change with animation
     */
    fun setChecked(checked: Boolean, animate: Boolean) {
        if (animate) {
            mTickDrawing = false
            mChecked = checked
            mDrewDistance = 0f
            if (checked) {
                startCheckedAnimation()
            } else {
                startUnCheckedAnimation()
            }
            if (mListener != null) {
                mListener!!.onCheckedChanged(this@SmoothCheckBox, mChecked)
            }
        } else {
            this.isChecked = checked
        }
    }

    private fun reset() {
        mTickDrawing = true
        mFloorScale = 1.0f
        mScaleVal = if (isChecked) 0f else 1.0f
        mFloorColor = if (isChecked) mCheckedColor else mFloorUnCheckedColor
        mDrewDistance = if (isChecked) (mLeftLineDistance + mRightLineDistance) else 0f
    }

    private fun measureSize(measureSpec: Int): Int {
        val defSize = dp2px(context, DEF_DRAW_SIZE.toFloat())
        val specSize = MeasureSpec.getSize(measureSpec)
        val specMode = MeasureSpec.getMode(measureSpec)
        var result = 0
        when (specMode) {
            MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST -> result = Math.min(defSize, specSize)
            MeasureSpec.EXACTLY -> result = specSize
        }
        return result
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measureSize(widthMeasureSpec), measureSize(heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mWidth = measuredWidth
        mStrokeWidth = (if (mStrokeWidth == 0) measuredWidth / 10 else mStrokeWidth)
        mStrokeWidth = if (mStrokeWidth > measuredWidth / 5) measuredWidth / 5 else mStrokeWidth
        mStrokeWidth = if ((mStrokeWidth < 3)) 3 else mStrokeWidth
        mCenterPoint!!.x = mWidth / 2
        mCenterPoint!!.y = measuredHeight / 2
        mTickPoints[0]!!.x = Math.round(measuredWidth.toFloat() / 30 * 7)
        mTickPoints[0]!!.y = Math.round(measuredHeight.toFloat() / 30 * 14)
        mTickPoints[1]!!.x = Math.round(measuredWidth.toFloat() / 30 * 13)
        mTickPoints[1]!!.y = Math.round(measuredHeight.toFloat() / 30 * 20)
        mTickPoints[2]!!.x = Math.round(measuredWidth.toFloat() / 30 * 22)
        mTickPoints[2]!!.y = Math.round(measuredHeight.toFloat() / 30 * 10)
        mLeftLineDistance = Math.sqrt(
            Math.pow((mTickPoints[1]!!.x - mTickPoints[0]!!.x).toDouble(), 2.0) +
                    Math.pow((mTickPoints[1]!!.y - mTickPoints[0]!!.y).toDouble(), 2.0)
        ).toFloat()
        mRightLineDistance = Math.sqrt(
            Math.pow((mTickPoints[2]!!.x - mTickPoints[1]!!.x).toDouble(), 2.0) +
                    Math.pow((mTickPoints[2]!!.y - mTickPoints[1]!!.y).toDouble(), 2.0)
        ).toFloat()
        mTickPaint!!.strokeWidth = mStrokeWidth.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        drawBorder(canvas)
        drawCenter(canvas)
        drawTick(canvas)
    }

    private fun drawCenter(canvas: Canvas) {
        mPaint!!.color = mUnCheckedColor
        val radius = (mCenterPoint!!.x - mStrokeWidth) * mScaleVal
        canvas.drawCircle(
            mCenterPoint!!.x.toFloat(),
            mCenterPoint!!.y.toFloat(),
            radius,
            (mPaint)!!
        )
    }

    private fun drawBorder(canvas: Canvas) {
        mFloorPaint!!.color = mFloorColor
        val radius = mCenterPoint!!.x
        canvas.drawCircle(
            mCenterPoint!!.x.toFloat(),
            mCenterPoint!!.y.toFloat(),
            radius * mFloorScale,
            (mFloorPaint)!!
        )
    }

    private fun drawTick(canvas: Canvas) {
        if (mTickDrawing && isChecked) {
            drawTickPath(canvas)
        }
    }

    private fun drawTickPath(canvas: Canvas) {
        mTickPath!!.reset()
        // draw left of the tick
        if (mDrewDistance < mLeftLineDistance) {
            val step: Float = if ((mWidth / 20.0f) < 3) 3f else (mWidth / 20.0f)
            mDrewDistance += step
            val stopX =
                mTickPoints[0]!!.x + (mTickPoints[1]!!.x - mTickPoints[0]!!.x) * mDrewDistance / mLeftLineDistance
            val stopY =
                mTickPoints[0]!!.y + (mTickPoints[1]!!.y - mTickPoints[0]!!.y) * mDrewDistance / mLeftLineDistance
            mTickPath!!.moveTo(mTickPoints[0]!!.x.toFloat(), mTickPoints[0]!!.y.toFloat())
            mTickPath!!.lineTo(stopX, stopY)
            canvas.drawPath((mTickPath)!!, (mTickPaint)!!)
            if (mDrewDistance > mLeftLineDistance) {
                mDrewDistance = mLeftLineDistance
            }
        } else {
            mTickPath!!.moveTo(mTickPoints[0]!!.x.toFloat(), mTickPoints[0]!!.y.toFloat())
            mTickPath!!.lineTo(mTickPoints[1]!!.x.toFloat(), mTickPoints[1]!!.y.toFloat())
            canvas.drawPath((mTickPath)!!, (mTickPaint)!!)

            // draw right of the tick
            if (mDrewDistance < mLeftLineDistance + mRightLineDistance) {
                val stopX =
                    mTickPoints[1]!!.x + (mTickPoints[2]!!.x - mTickPoints[1]!!.x) * (mDrewDistance - mLeftLineDistance) / mRightLineDistance
                val stopY =
                    mTickPoints[1]!!.y - (mTickPoints[1]!!.y - mTickPoints[2]!!.y) * (mDrewDistance - mLeftLineDistance) / mRightLineDistance
                mTickPath!!.reset()
                mTickPath!!.moveTo(mTickPoints[1]!!.x.toFloat(), mTickPoints[1]!!.y.toFloat())
                mTickPath!!.lineTo(stopX, stopY)
                canvas.drawPath((mTickPath)!!, (mTickPaint)!!)
                val step = (if ((mWidth / 20) < 3) 3 else (mWidth / 20)).toFloat()
                mDrewDistance += step
            } else {
                mTickPath!!.reset()
                mTickPath!!.moveTo(mTickPoints[1]!!.x.toFloat(), mTickPoints[1]!!.y.toFloat())
                mTickPath!!.lineTo(mTickPoints[2]!!.x.toFloat(), mTickPoints[2]!!.y.toFloat())
                canvas.drawPath((mTickPath)!!, (mTickPaint)!!)
            }
        }

        // invalidate
        if (mDrewDistance < mLeftLineDistance + mRightLineDistance) {
            postDelayed(object : Runnable {
                override fun run() {
                    postInvalidate()
                }
            }, 10)
        } else {
            animationChangeListener?.onCheckAnimationChanged(true, false)
        }
    }

    private fun startCheckedAnimation() {
        val animator = ValueAnimator.ofFloat(1.0f, 0f)
        animator.duration = (mAnimDuration / 3 * 2).toLong()
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                mScaleVal = animation.animatedValue as Float
                mFloorColor = getGradientColor(mUnCheckedColor, mCheckedColor, 1 - mScaleVal)
                postInvalidate()
            }
        })
        animator.start()
        val floorAnimator = ValueAnimator.ofFloat(1.0f, 0.8f, 1.0f)
        floorAnimator.duration = mAnimDuration.toLong()
        floorAnimator.interpolator = LinearInterpolator()
        floorAnimator.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                mFloorScale = animation.animatedValue as Float
                postInvalidate()
            }
        })

        floorAnimator.doOnStart {
            animationChangeListener?.onCheckAnimationChanged(true, true)
        }
        floorAnimator.start()
        drawTickDelayed()
    }

    private fun startUnCheckedAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1.0f)
        animator.duration = mAnimDuration.toLong()
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                mScaleVal = animation.animatedValue as Float
                mFloorColor = getGradientColor(mCheckedColor, mFloorUnCheckedColor, mScaleVal)
                postInvalidate()
            }
        })
        animator.start()
        val floorAnimator = ValueAnimator.ofFloat(1.0f, 0.8f, 1.0f)
        floorAnimator.duration = mAnimDuration.toLong()
        floorAnimator.interpolator = LinearInterpolator()
        floorAnimator.addUpdateListener(object : AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                mFloorScale = animation.animatedValue as Float
                postInvalidate()
            }
        })
        floorAnimator.doOnEnd {
            animationChangeListener?.onCheckAnimationChanged(false, false)
        }
        floorAnimator.doOnStart {
            animationChangeListener?.onCheckAnimationChanged(false, true)
        }
        floorAnimator.start()
    }

    private fun drawTickDelayed() {
        postDelayed(object : Runnable {
            override fun run() {
                mTickDrawing = true
                postInvalidate()
            }
        }, mAnimDuration.toLong())
    }

    fun setOnCheckedChangeListener(l: OnCheckedChangeListener?) {
        mListener = l
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(checkBox: SmoothCheckBox?, isChecked: Boolean)
    }

    fun interface OnAnimationChangeListener {
        fun onCheckAnimationChanged(isChecked: Boolean, start: Boolean)
    }

    companion object {
        private val KEY_INSTANCE_STATE = "InstanceState"
        private val COLOR_TICK = Color.WHITE
        private val COLOR_UNCHECKED = Color.WHITE
        private val COLOR_CHECKED = Color.parseColor("#FB4846")
        private val COLOR_FLOOR_UNCHECKED = Color.parseColor("#DFDFDF")
        private val DEF_DRAW_SIZE = 25
        private val DEF_ANIM_DURATION = 350
        private fun getGradientColor(startColor: Int, endColor: Int, percent: Float): Int {
            val startA = Color.alpha(startColor)
            val startR = Color.red(startColor)
            val startG = Color.green(startColor)
            val startB = Color.blue(startColor)
            val endA = Color.alpha(endColor)
            val endR = Color.red(endColor)
            val endG = Color.green(endColor)
            val endB = Color.blue(endColor)
            val currentA = (startA * (1 - percent) + endA * percent).toInt()
            val currentR = (startR * (1 - percent) + endR * percent).toInt()
            val currentG = (startG * (1 - percent) + endG * percent).toInt()
            val currentB = (startB * (1 - percent) + endB * percent).toInt()
            return Color.argb(currentA, currentR, currentG, currentB)
        }
    }
}