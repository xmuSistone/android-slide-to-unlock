package com.stone.textview.slide;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

@SuppressLint("DrawAllocation")
public class SlideTextView extends View {
	private String showText = "向右滑动来解锁";

	private Paint paint;
	private int cycleNum = 0; // 线程循环，sleep的次数

	// 在onDraw函数中调用canvas.drawText需要传入x/y坐标，那个坐标是文字左下角的坐标。
	private int firstLineOffset = 0;

	// drawText的y初始值非常诡谲，根据stackoverflow的反馈，那个y值要通过计算单个文字占据的高度，才比较科学、合理，但是存在一定的偏差
	// 这个extraPaddingTop就是用来弥补偏差的，不代表绝对正确
	private int extraPaddingTop = PixValue.dip.valueOf(1f);

	// 以下属性需要通过xml文件来配置
	private int textSize = PixValue.sp.valueOf(18); // 字号
	private int textColor = Color.WHITE; // 文字颜色

	private int textWidth, textHeight;

	// 在文字的左边和右边，都添加了一些空格以占位
	// rangeArea的意思是闪光的文字部分占所有文字部分(不包括空格)的三分之一
	private String extraText, actualText;
	private int extraWidth;
	private float rangeArea;
	private float maxXDistance;
	private int darkColor;
	private Matrix trans;

	public SlideTextView(Context context) {
		this(context, null);
	}

	public SlideTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SlideTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		// 1. 初始化paint画笔
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(textColor);
		paint.setTextSize(textSize);
		paint.setTextAlign(Align.LEFT);

		// 2. 初始化那个诡谲的y坐标值
		Rect rect = new Rect();
		paint.getTextBounds("豆", 0, 1, rect); // 以一个典型的汉字为模板，计算高度
		firstLineOffset = (int) (rect.height() - rect.bottom) + extraPaddingTop; // stackoverflow上面某人给的建议

		textWidth = (int) paint.measureText(showText);
		textHeight = textSize;
		extraText = "            ";
		extraWidth = (int) paint.measureText(extraText);
		rangeArea = (float) textWidth / (textWidth + extraWidth * 2) / 3;
		actualText = extraText + showText + extraText; // drawText时，在文字的左边和右边，都添加了一些空格以占位
		maxXDistance = 1 + 2 * rangeArea;
		darkColor = Color.parseColor("#747474");
		trans = new Matrix();
		trans.setRotate(-90);
	}

	@Override
	protected void onFinishInflate() {
		new UIThread().start();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int thisNum = 1 + cycleNum % circulationNum;

		// 跑马灯的区域是[-rangeArea, 1 + rangeArea]
		float centerX = -rangeArea + maxXDistance * thisNum / circulationNum;
		Shader shader = new LinearGradient(0, 0, 0, textWidth, new int[] {
				darkColor, darkColor, Color.WHITE, darkColor, darkColor },
				new float[] { -1f, centerX - rangeArea, centerX,
						centerX + rangeArea, 2f }, TileMode.CLAMP);
		shader.setLocalMatrix(trans);
		paint.setShader(shader);
		canvas.drawText(actualText, -extraWidth + getPaddingLeft(),
				getPaddingTop() + firstLineOffset, paint);
	}

	private int circulationNum = 30; // 从左至右一次循环的次数

	class UIThread extends Thread {

		public UIThread() {
			cycleNum = 0;
		}

		@Override
		public void run() {
			try {
				while (true) {
					sleep(40);
					// handler通知ui更新文字透明度
					if (cycleNum > 0 && cycleNum % circulationNum == 0) {
						sleep(1200);
					}

					Message msg = uiHandler.obtainMessage();
					cycleNum++;
					msg.sendToTarget();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		};
	};

	private Handler uiHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// 刷新View
			invalidate();
		};
	};

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		// 复写onMeasure方法，没有子View，手动计算高度
		setMeasuredDimension(textWidth + getPaddingLeft() + getPaddingRight(),
				textHeight + getPaddingTop() + getPaddingBottom());
	}
}
