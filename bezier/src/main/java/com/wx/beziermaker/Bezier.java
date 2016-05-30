package com.wx.beziermaker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class Bezier extends View {

    public static final int COUNT = 9;  // 贝塞尔曲线阶数
    public static final int BEZIER_WIDTH = 5;   // 贝塞尔曲线线宽
    public static final int TANGENT_WIDTH = 5;  // 切线线宽
    public static final int FIXED_WIDTH = 6;    // 固定线线宽

    private Path mBezierPath = null;    // 贝塞尔曲线路径

    private Paint mBezierPaint = null;  // 贝塞尔曲线画笔
    private Paint mMovingPaint = null;  // 移动点画笔
    private Paint mControlPaint = null;  // 控制点画笔
    private Paint mTangentPaint = null;  // 切线画笔
    private Paint mLinePaint = null;    // 固定线画笔
    private Paint mTextPaint = null;    // 文字画笔

    private ArrayList<PointF> mBezierPoints = null; // 贝塞尔曲线点集
    private PointF mBezierPoint = null; // 贝塞尔曲线移动点

    private PointF mPoint1, mPoint2;

    private PointF p1, p2, p3;  // 固定点

    private ArrayList<PointF> mPoints1 = new ArrayList<>();
    private ArrayList<PointF> mPoints2 = new ArrayList<>();

    public Bezier(Context context) {
        super(context);
        init();
    }

    public Bezier(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Bezier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 贝塞尔曲线画笔
        mBezierPaint = new Paint();
        mBezierPaint.setColor(Color.RED);
        mBezierPaint.setStrokeWidth(BEZIER_WIDTH);
        mBezierPaint.setStyle(Paint.Style.STROKE);
        mBezierPaint.setAntiAlias(true);

        // 移动点画笔
        mMovingPaint = new Paint();
        mMovingPaint.setColor(Color.BLACK);
        mMovingPaint.setAntiAlias(true);
        mMovingPaint.setStyle(Paint.Style.FILL);

        // 控制点画笔
        mControlPaint = new Paint();
        mControlPaint.setColor(Color.BLACK);
        mControlPaint.setAntiAlias(true);
        mControlPaint.setStyle(Paint.Style.STROKE);

        // 切线画笔
        mTangentPaint = new Paint();
        mTangentPaint.setColor(Color.GREEN);
        mTangentPaint.setAntiAlias(true);
        mTangentPaint.setStrokeWidth(TANGENT_WIDTH);
        mTangentPaint.setStyle(Paint.Style.FILL);

        // 固定线画笔
        mLinePaint = new Paint();
        mLinePaint.setColor(Color.LTGRAY);
        mLinePaint.setStrokeWidth(FIXED_WIDTH);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.FILL);

        // 文字画笔
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setAntiAlias(true);

        p1 = new PointF(100, 100);
        p2 = new PointF(50, 300);
        p3 = new PointF(500, 200);
        mPoint1 = p1;
        mPoint2 = p2;

        mBezierPoints = createQuad(p1, p2, p3);

        mBezierPath = new Path();
    }

    private ArrayList<PointF> createQuad(PointF start, PointF con, PointF end) {
        ArrayList<PointF> points = new ArrayList<>();
        float x, y;
        for (float t = 0; t <= 1; t += 0.001f) {
            x = (1 - t) * (1 - t) * start.x + 2 * t * (1 - t) * con.x + t * t * end.x;
            y = (1 - t) * (1 - t) * start.y + 2 * t * (1 - t) * con.y + t * t * end.y;
            points.add(new PointF(x, y));
        }
        for (float i = 0; i <= 1; i += 0.001f) {
            float xx = start.x + (con.x - start.x) * i;
            float yy = start.y + (con.y - start.y) * i;
            float xxx = con.x + (end.x - con.x) * i;
            float yyy = con.y + (end.y - con.y) * i;
            mPoints1.add(new PointF(xx, yy));
            mPoints2.add(new PointF(xxx, yyy));
        }
        return points;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBezierPoint == null) {
            mBezierPoint = mBezierPoints.get(0);
            mBezierPath.moveTo(mBezierPoint.x, mBezierPoint.y);
        }
        // 固定点连线
        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mLinePaint);
        canvas.drawLine(p2.x, p2.y, p3.x, p3.y, mLinePaint);

        // 固定点
        canvas.drawCircle(p1.x, p1.y, 6, mControlPaint);
        canvas.drawCircle(p2.x, p2.y, 6, mControlPaint);
        canvas.drawCircle(p3.x, p3.y, 6, mControlPaint);

        // 连线
        canvas.drawLine(mPoint1.x, mPoint1.y, mPoint2.x, mPoint2.y, mTangentPaint);

        // Bezier曲线
        mBezierPath.lineTo(mBezierPoint.x, mBezierPoint.y);
        canvas.drawPath(mBezierPath, mBezierPaint);
        // Bezier曲线起始移动点
        canvas.drawCircle(mBezierPoint.x, mBezierPoint.y, 6, mMovingPaint);

        mHandler.removeMessages(100);
        mHandler.sendEmptyMessage(100);
    }

    private int i = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                i += 3;
                if (i >= mBezierPoints.size() || i >= mPoints1.size() || i >= mPoints2.size()) {
                    removeMessages(100);
                    return;
                }
                mBezierPoint = new PointF(mBezierPoints.get(i).x, mBezierPoints.get(i).y);
                mPoint1 = new PointF(mPoints1.get(i).x, mPoints1.get(i).y);
                mPoint2 = new PointF(mPoints2.get(i).x, mPoints2.get(i).y);
                invalidate();
            }
        }
    };

}
