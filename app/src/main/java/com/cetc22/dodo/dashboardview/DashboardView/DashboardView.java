package com.cetc22.dodo.dashboardview.DashboardView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.cetc22.dodo.dashboardview.R;

public class DashboardView extends View {

    private static final String TAG = "DashboardView";

    private int mBackgroundColor;
    private int mThemeColor;
    private int mColorProgressed;
    private int mColorNotProgressed;


    private String mUpperText;
    private String mUnitText;
    private float mUpperTextSize;
    private String mLowerText;
    private float mLowerTextSize;

    private boolean isShowUpperText = true;
    private boolean isShowLowerText = true;
    private boolean mIsLowerTextAsValue = false;
    private boolean isShowPointer = false;
    private boolean isShowValue = true;

    private int mPresent;
    private int mMarkCount;

    private static final int PRESENT_SHOW_POINTER_ONLY = 0;
    private static final int PRESENT_SHOW_VALUE_ONLY = 1;
    private static final int PRESENT_SHOW_POINTER_AND_VALUE = 2;
    private String mValueFontPath;
    private boolean isCustomFont = false;

    private float mValue;
    private float mRange;
    private float mSweepAngle;


    private Paint paintMark;
    private Paint paintPointer;
    private Paint paintText;

    private float startAngle;
    private float arcDiameter;
    private float arcRadius;
    private int layoutWidth;
    private int layoutHeight;
    private float offset;
    private float centerX;
    private float centerY;

    public DashboardView(Context context) {
        this(context, null);
    }

    public DashboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DashboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DashboardView, defStyleAttr, 0);
        mValue = ta.getFloat(R.styleable.DashboardView_value, 0.0f);
        mRange = ta.getFloat(R.styleable.DashboardView_range, 100f);
        mSweepAngle = ta.getFloat(R.styleable.DashboardView_sweepAngle, 300f);
        mMarkCount = ta.getInt(R.styleable.DashboardView_markCount, 100);

        mThemeColor = ta.getColor(R.styleable.DashboardView_themeColor, getResources().getColor(R.color.colorWhite));
        mColorProgressed = ta.getColor(R.styleable.DashboardView_colorProgressed, getResources().getColor(R.color.colorWhite));
        mColorNotProgressed = ta.getColor(R.styleable.DashboardView_colorNotProgressed, getResources().getColor(R.color.colorGrayLight));
        mBackgroundColor = ta.getColor(R.styleable.DashboardView_backgroundColor, getResources().getColor(R.color.colorTransparent));
        mUnitText = ta.getString(R.styleable.DashboardView_unitText);
        mValueFontPath = ta.getString(R.styleable.DashboardView_valueFontPath);
        if (mValueFontPath != null) {
            isCustomFont = true;
        }
        mPresent = ta.getInt(R.styleable.DashboardView_present, PRESENT_SHOW_POINTER_AND_VALUE);
        mUpperText = ta.getString(R.styleable.DashboardView_upperText);
        if (mUpperText == null) isShowUpperText = false;
        mUpperTextSize = ta.getDimensionPixelSize(R.styleable.DashboardView_upperTextSize, 16);
        mLowerText = ta.getString(R.styleable.DashboardView_lowerText);
        mLowerTextSize = ta.getDimensionPixelSize(R.styleable.DashboardView_lowerTextSize, 24);
        mIsLowerTextAsValue = ta.getBoolean(R.styleable.DashboardView_lowerTextAsValue, false);



        ta.recycle();
    }

    public void setValue(float value) {
         mValue = value;
        invalidate();

    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calcParams(canvas);
        init();
        //draw the background
        canvas.drawColor(mBackgroundColor);
        //draw things that not change
        drawUpperText(canvas, paintText, isShowUpperText);
        drawLowerText(canvas, paintText, mLowerText ,isShowLowerText);
        canvas.save();
        invalidateValueChange(canvas);

        //draw things that change

    }

    @Override
    public void onMeasure(int widthMeasuredSpec, int heightMeasuredSpec) {
        setMeasuredDimension(measureWidth(widthMeasuredSpec), measureHeight(heightMeasuredSpec));
    }


    private void init() {

        paintMark = new Paint();
        paintMark.setAntiAlias(true);
        paintMark.setColor(mThemeColor);
        paintMark.setStyle(Paint.Style.STROKE);
        paintMark.setStrokeWidth(arcDiameter/20);

        paintPointer = new Paint();
        paintPointer.setAntiAlias(true);
        paintPointer.setColor(mThemeColor);
        paintPointer.setStyle(Paint.Style.FILL);

        paintText = new Paint();
        paintText.setColor(mThemeColor);
    }

    private void calcParams(Canvas canvas) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();
        arcDiameter = Math.min(layoutWidth - paddingLeft - paddingRight, layoutHeight - paddingBottom - paddingTop);   //arc diameter;
        arcRadius = arcDiameter / 2;
        startAngle = 270f - mSweepAngle /2;
        if (mSweepAngle < 220f)
            offset = layoutHeight * 0.14f;
        else
            offset = 0;
        canvas.translate((layoutWidth - paddingLeft - paddingRight)/2 + paddingLeft, (layoutHeight - paddingBottom - paddingBottom) / 2 + paddingTop + offset);

        if (mPresent == PRESENT_SHOW_POINTER_AND_VALUE) {
            isShowPointer = true;
            isShowValue = true;
        } else if (mPresent == PRESENT_SHOW_POINTER_ONLY) {
            isShowPointer = true;
            isShowValue = false;
        } else {
            isShowPointer = false;
            isShowValue = true;
        }

        if (mLowerText == null || mIsLowerTextAsValue)
            isShowLowerText = false;
        else isShowLowerText = true;

    }

    private void drawMarks(Canvas canvas, Paint paint, float value) {

        //draw the mark line
        float percent = value/mRange;
        if (percent > 1) percent = 1;
        if (percent < 0) percent = 0;

        RectF markOval = new RectF(centerX -  arcRadius, centerY - arcRadius, centerX + arcRadius, centerY + arcRadius);
        float start = startAngle;
        float end = start + mSweepAngle + 1;
        float interval = mSweepAngle / mMarkCount;
        float markChangePoint = start + mSweepAngle * percent;
        float lineWidth = 1f;
        float position = start;
        paint.setColor(mColorProgressed);
        for (; position < markChangePoint;) {

            canvas.drawArc(markOval, position, lineWidth, false, paint);
            position = position + interval;
        }
        paint.setColor(mColorNotProgressed);
        for (; position < end; ) {
            canvas.drawArc(markOval, position, lineWidth, false, paint);
            position = position + interval;
        }
    }

    private void invalidateValueChange(Canvas canvas) {
        canvas.restore();
        drawPointer(canvas, paintPointer, mValue, isShowPointer);
        drawValue(canvas, paintText, mValue, isShowValue);
        drawMarks(canvas, paintMark, mValue);
    }

    private void drawValue(Canvas canvas, Paint paint, float value, boolean doNeed) {
        if (!doNeed)
            return;

        Log.i(TAG, "draw value " + mValue);


        float tX, tY;
        String valueString = String.format("%02.0f", value);

        if (!mIsLowerTextAsValue) {
            tX = centerX;
            paint.setTextSize(0.35f * arcDiameter);
            if (isCustomFont) {
                Typeface tf = Typeface.createFromAsset(this.getContext().getAssets(), mValueFontPath);
                paint.setTypeface(tf);
            }
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            float top = fontMetrics.top;//为基线到字体上边框的距离,即上图中的top
            float bottom = fontMetrics.bottom;//为基线到字体下边框的距离,即上图中的bottom
            tY = centerY - (top + bottom) / 1.8f;//基线中间点的y轴计算公式

            if (value > mRange) {
                value = mRange;
            }
            paint.setTextAlign(Paint.Align.CENTER);

            float measuredTextWidth = paint.measureText(valueString);

            canvas.drawText(valueString, tX, tY, paint);
            if (mUnitText != null) {
                paint.setTextSize(0.1f * arcDiameter);
                paint.setTextAlign(Paint.Align.LEFT);
                tX += measuredTextWidth / 2;

                canvas.drawText(mUnitText, tX, tY, paint);
            }
        } else {
            if (mUnitText != null)
                valueString += mUnitText;
            drawLowerText(canvas, paint, valueString, true);
        }



    }

    private void drawUpperText(Canvas canvas, Paint paint, boolean doNeed) {
        if (!doNeed)
            return;
        float tx = centerX;
        float ty = centerY - arcRadius * 0.3f;
        paint.setTextSize(mUpperTextSize);
        canvas.drawText(mUpperText, tx, ty, paint);

    }


    private void drawLowerText(Canvas canvas, Paint paint, String lowerText, boolean doNeed) {
        if (!doNeed)
            return;
        float tx = centerX;
        float ty = centerY + arcRadius * 0.58f;
        paint.setTextSize(mLowerTextSize);
        canvas.drawText(lowerText, tx, ty, paint);
    }

    private void drawPointer(Canvas canvas, Paint paint, float value, boolean doNeed) {
        if (!doNeed)
            return;
        canvas.save();
        float percent = value/mRange;
        if (percent > 1) percent = 1;
        if (percent < 0) percent = 0;
        float angle = 270f - startAngle -  (1 - percent) * mSweepAngle;
        float pointerTipAngle = 0.2f;
        float pointerL = arcRadius * 0.8f;
        float circleR = pointerL * 0.06f;
        canvas.rotate(angle);
        Path path = new Path();
        path.moveTo(0,  -pointerL);
        path.lineTo(0, circleR);
        RectF oval = new RectF(-circleR, -circleR, circleR, circleR);
        path.arcTo(oval, 90, 90 - pointerTipAngle/2f);
        path.close();
        canvas.drawPath(path, paint);
        path.moveTo(0, -pointerL);
        path.lineTo(0, circleR);
        path.arcTo(oval, 90, pointerTipAngle/2f - 90f);
        path.close();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    private int measureHeight(int measureSpec) {

        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        // Default size if no limits are specified.
        int result = 500;
        if (specMode == MeasureSpec.AT_MOST){
            result = specSize;
        }
        else if (specMode == MeasureSpec.EXACTLY){
            result = specSize;
        }
        layoutHeight = result;

        return result;
    }

    private int measureWidth(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        // Default size if no limits are specified.
        int result = 500;
        if (specMode == MeasureSpec.AT_MOST){
            result = specSize;
        }

        else if (specMode == MeasureSpec.EXACTLY){
            result = specSize;
        }
        layoutWidth = result;
        return result;
    }

}