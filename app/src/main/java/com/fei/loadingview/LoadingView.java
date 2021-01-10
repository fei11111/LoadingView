package com.fei.loadingview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * @ClassName: LoadingView
 * @Description: 仿58同城loadingView
 * @Author: Fei
 * @CreateDate: 2021-01-09 21:29
 * @UpdateUser: 更新者
 * @UpdateDate: 2021-01-09 21:29
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class LoadingView extends View {

    //图形大小
    private float mShapeSize = 25;//默认25dp
    //图形弹跳高度
    private float mShapeScrollHeight = 80;//默认80dp
    //阴影宽度
    private float mShadowWidth = 50;//默认25dp
    //阴影高度
    private float mShadowHeight = 3;//默认3dp
    //字体大小
    private int mTextSize = 23;//默认23sp
    //字体颜色
    private int mTextColor = Color.BLACK;//默认黑色
    //文本画笔
    private Paint mTextPaint;
    //文本
    private String mText;

    //形状颜色
    private int mCircleColor = Color.GREEN;//圆形颜色
    private int mSquareColor = Color.BLUE;//正方形颜色
    private int mTriangleColor = Color.YELLOW;//三角形颜色
    private Shape mCurrentShape = Shape.SHAPE_RECT;//默认先显示正方形
    //形状画笔
    private Paint mShapePaint;
    //三角形
    private Path mPath;
    //形状中心点
    private float mCenter;
    //形状一半
    private float mHalfShape;
    //阴影一半
    private float mHalfShadow;
    //文本一半
    private float mHalfText;
    //文本高度
    private float mTextHeight;
    //文本离阴影高度
    private float mDistance = 10;
    //百分比 0-1
    private float mPercent;
    //动画时长
    private int mDuration = 500;
    //阴影椭圆
    RectF mRectF;

    public enum Shape {
        SHAPE_RECT,
        SHAPE_CIRCLE,
        SHAPE_TRIANGLE
    }

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingView);

        mShapeSize = typedArray.getDimension(R.styleable.LoadingView_shapeSize, dp2px(mShapeSize));//默认25dp
        mShapeScrollHeight = typedArray.getDimension(R.styleable.LoadingView_shapeScrollHeight, dp2px(mShapeScrollHeight));//默认80dp
        mCircleColor = typedArray.getColor(R.styleable.LoadingView_circleColor, mCircleColor);
        mSquareColor = typedArray.getColor(R.styleable.LoadingView_squareColor, mSquareColor);
        mTriangleColor = typedArray.getColor(R.styleable.LoadingView_triangleColor, mTriangleColor);
        mShadowWidth = typedArray.getDimension(R.styleable.LoadingView_shadowWidth, dp2px(mShadowWidth));//默认25dp
        mShadowHeight = typedArray.getDimension(R.styleable.LoadingView_shadowHeight, dp2px(mShadowHeight));//默认3dp
        mTextSize = typedArray.getDimensionPixelSize(R.styleable.LoadingView_textSize, sp2px(mTextSize));//默认23sp
        mTextColor = typedArray.getColor(R.styleable.LoadingView_textColor, mTextColor);//默认黑色
        mText = typedArray.getString(R.styleable.LoadingView_text);
        typedArray.recycle();

        mDistance = dp2px(mDistance);
        mPath = new Path();
        mRectF = new RectF();
        initPaint();

        post(new Runnable() {
            @Override
            public void run() {
                down();
            }
        });

    }

    /**
     * 初始画笔
     */
    private void initPaint() {
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);

        mShapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShapePaint.setStyle(Paint.Style.FILL);

    }

    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int sp2px(int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //获取文本宽高度
        int textHeight = 0;
        int textWidth = 0;
        if (!TextUtils.isEmpty(mText)) {
            Rect bounds = new Rect();
            mTextPaint.getTextBounds(mText, 0, mText.length(), bounds);
            textWidth = bounds.width();
            textHeight = bounds.height();
        }
        //总宽高
        int height = (int) (mShapeSize + mShapeScrollHeight + mShadowHeight + textHeight + getPaddingTop() + getPaddingBottom() + mDistance);
        //宽度取三者最大
        int width = (int) Math.max(Math.max(mShapeSize, mShadowWidth), textWidth);
        setMeasuredDimension(width, height);

        mCenter = width / 2f;
        mHalfShape = mShapeSize / 2f;
        mHalfShadow = mShadowWidth / 2f;
        mHalfText = textWidth / 2f;
        mTextHeight = textHeight;
    }

    //开始画
    @Override
    protected void onDraw(Canvas canvas) {
        //画形状
        drawShape(canvas);
        //画阴影
        drawShadow(canvas);
        //画文本
        drawText(canvas);
    }

    /**
     * 画文本
     *
     * @param canvas
     */
    private void drawText(Canvas canvas) {
        //剩下文本高度
        float top = getPaddingTop() + mShapeSize + mShapeScrollHeight + mShadowHeight + mDistance;
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float dy = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
        canvas.drawText(mText, mCenter - mHalfText, top + (mTextHeight / 2) + dy, mTextPaint);
    }

    /**
     * 画形状
     *
     * @param canvas
     */
    private void drawShape(Canvas canvas) {
        //保存画布,需要做动画
        canvas.save();
        float dy = mPercent * mShapeScrollHeight;
//        float degrees = mPercent * 180f;
//        canvas.rotate(degrees,mCenter,dy);
        canvas.translate(0, dy);
        switch (mCurrentShape) {
            case SHAPE_RECT:
                //画正方形
                mShapePaint.setColor(mSquareColor);
                canvas.drawRect(mCenter - mHalfShape, getPaddingTop(),
                        mCenter + mHalfShape, getPaddingTop() + mShapeSize, mShapePaint);
                break;
            case SHAPE_CIRCLE:
                //画圆形
                mShapePaint.setColor(mCircleColor);
                canvas.drawCircle(mCenter, getPaddingTop() + mHalfShape, mHalfShape, mShapePaint);
                break;
            case SHAPE_TRIANGLE:
                //画三角形
                float y = (float) (getPaddingTop() + (mShapeSize / 2 * Math.sqrt(3)));
                mShapePaint.setColor(mTriangleColor);
                mPath.moveTo(mCenter, getPaddingTop());
                mPath.lineTo(mCenter - mHalfShape, y);
                mPath.lineTo(mCenter + mHalfShape, y);
                mPath.close();
                canvas.drawPath(mPath, mShapePaint);
                break;
        }
        canvas.restore();
    }

    /**
     * 画阴影
     *
     * @param canvas
     */
    private void drawShadow(Canvas canvas) {
        canvas.save();
        //x轴缩放
        canvas.scale(1 - mPercent * 0.5f, 1, mCenter, 0);
        float top = getPaddingTop() + mShapeSize + mShapeScrollHeight;
        mRectF.set(mCenter - mHalfShadow, top,
                mCenter + mHalfShadow, top + mShadowHeight);
        canvas.drawOval(mRectF, mTextPaint);
        canvas.restore();
    }

    public void exchangeShape() {
        switch (mCurrentShape) {
            case SHAPE_RECT:
                mCurrentShape = Shape.SHAPE_CIRCLE;
                break;
            case SHAPE_CIRCLE:
                mCurrentShape = Shape.SHAPE_TRIANGLE;
                break;
            case SHAPE_TRIANGLE:
                mCurrentShape = Shape.SHAPE_RECT;
                break;
        }
        invalidate();
    }

    /**
     * 下落
     */

    private void down() {
        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(mDuration);
        //加速插值器
        valueAnimator.setInterpolator(new AccelerateInterpolator());
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                exchangeShape();
                up();
            }
        });
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPercent = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        valueAnimator.start();
    }

    /**
     * 上弹
     */
    private void up() {
        ValueAnimator valueAnimator = ObjectAnimator.ofFloat(1, 0);
        valueAnimator.setDuration(mDuration);
        //减速插值器
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                down();

            }
        });
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPercent = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        valueAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearAnimation();
    }
}
