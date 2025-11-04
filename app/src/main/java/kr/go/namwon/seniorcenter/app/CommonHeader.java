package kr.go.namwon.seniorcenter.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import androidx.constraintlayout.widget.ConstraintLayout;

import kr.go.namwon.seniorcenter.app.databinding.CommonHeaderBinding;

public class CommonHeader extends ConstraintLayout {

    private String title;
    private CommonHeaderBinding binding;

    public CommonHeader(Context context) {
        super(context);
        init(context, null);
    }

    public CommonHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CommonHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        binding = CommonHeaderBinding.inflate(LayoutInflater.from(context), this, true);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CommonHeader, 0, 0);
            try {
                title = typedArray.getString(R.styleable.CommonHeader_title);
                if (title == null) {
                    title = "";
                }
                Log.d("commonHeader", "title ::: " + title);
            } finally {
                typedArray.recycle();
            }
        } else {
            title = "";
        }

        binding.tvTitle.setText(title);
    }
}