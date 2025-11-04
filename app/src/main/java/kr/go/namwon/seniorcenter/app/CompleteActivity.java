package kr.go.namwon.seniorcenter.app;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import kr.go.namwon.seniorcenter.app.databinding.ActivityCompleteBinding;
import kr.go.namwon.seniorcenter.app.util.BaseAppCompatActivity;


public class CompleteActivity extends BaseAppCompatActivity {

    private ActivityCompleteBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCompleteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }
}