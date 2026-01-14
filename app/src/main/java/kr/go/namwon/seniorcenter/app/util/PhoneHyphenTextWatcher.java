package kr.go.namwon.seniorcenter.app.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class PhoneHyphenTextWatcher implements TextWatcher {

    private final EditText editText;
    private boolean isFormatting;

    public PhoneHyphenTextWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (isFormatting) return;

        isFormatting = true;

        String digits = s.toString().replaceAll("[^0-9]", "");

        String formatted;
        if (digits.length() <= 3) {
            formatted = digits;
        } else if (digits.length() <= 7) {
            formatted = digits.substring(0, 3) + "-" + digits.substring(3);
        } else {
            formatted = digits.substring(0, 3) + "-"
                    + digits.substring(3, 7) + "-"
                    + digits.substring(7);
        }

        editText.setText(formatted);
        editText.setSelection(formatted.length());

        isFormatting = false;
    }
}
