package com.metsakuur.lemondemo;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatRadioButton;

public class CustomRadioButton extends AppCompatRadioButton {

    private int startPadding = 0;

    public CustomRadioButton(Context context) {
        super(context);
    }
    public CustomRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CustomRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setStartPaddingDp(float paddingDp) {
        startPadding = (int) (paddingDp * getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(startPadding, 0f);
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public int getStartPadding() {
        return startPadding;
    }

    public void setStartPadding(int startPadding) {
        this.startPadding = startPadding;
        requestLayout();
    }
}