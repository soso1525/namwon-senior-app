package kr.go.namwon.seniorcenter.app.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import kr.go.namwon.seniorcenter.app.databinding.ActivityJoinBinding;
import kr.go.namwon.seniorcenter.app.model.Center;
import kr.go.namwon.seniorcenter.app.model.CenterResponse;
import kr.go.namwon.seniorcenter.app.model.SignUpRequest;
import kr.go.namwon.seniorcenter.app.retrofit.ApiClient;
import kr.go.namwon.seniorcenter.app.util.PhoneHyphenTextWatcher;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends BaseAppCompatActivity {
    public static final String TAG = "TAG_JoinActivity";
    private ActivityJoinBinding binding;
    private ArrayAdapter<Center> centerSpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJoinBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.cancelBtn.setOnClickListener(v -> finish());
        binding.signUpBtn.setOnClickListener(v -> signUp());

        centerSpinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        centerSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.centerSpinner.setAdapter(centerSpinnerAdapter);

        ApiClient.memberApi()
                .getCenterList()
                .enqueue(new Callback<CenterResponse>() {
                    @Override
                    public void onResponse(Call<CenterResponse> call, Response<CenterResponse> response) {
                        binding.loadingLayout.setVisibility(View.GONE);
                        List<Center> centerList = response.body().getResultVO();

                        centerSpinnerAdapter.clear();
                        centerSpinnerAdapter.add(new Center(0, "경로당 선택", null));
                        centerSpinnerAdapter.addAll(centerList);
                        centerSpinnerAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(Call<CenterResponse> call, Throwable t) {
                        Log.e(TAG, "Get center list failed: " + t.getMessage());
                    }
                });

        binding.phoneEt.addTextChangedListener(new PhoneHyphenTextWatcher(binding.phoneEt));
        binding.birthdateEt.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(
                            Locale.KOREA,
                            "%04d-%02d-%02d",
                            selectedYear,
                            selectedMonth + 1,
                            selectedDay
                    );
                    binding.birthdateEt.setText(formattedDate);
                },
                year,
                month,
                day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void signUp() {

        String name = binding.nameEt.getText().toString();
        String phone = binding.phoneEt.getText().toString();
        String password = "1";
        String birthdate = binding.birthdateEt.getText().toString();

        String gender = "";
        int checkedId = binding.sexRadioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            gender = "";
        } else {
            RadioButton selected = findViewById(checkedId);
            gender = selected.getText().toString().equals("남성") ? "M" : "F"; // "남성" or "여성"
        }

        Center selectedCenter = (Center) binding.centerSpinner.getSelectedItem();

        String specific = binding.specificationEt.getText().toString();
        String medication = binding.medicationEt.getText().toString();

        if (name.isEmpty()
                || phone.length() != 13
                || birthdate.isEmpty()
                || gender.isEmpty()
                || selectedCenter == null
                || selectedCenter.getId() == 0) {
            Toast.makeText(this, "회원 정보를 모두 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        SignUpRequest request = new SignUpRequest();
        request.setName(name);
        request.setPhone(phone);
        request.setPassword(password);
        request.setBirthdate(birthdate);
        request.setGender(gender);
        request.setCenterId(selectedCenter.getId());
        request.setSpecific(specific);
        request.setMedication(medication);

        binding.loadingLayout.setVisibility(View.VISIBLE);
        ApiClient.memberApi()
                .join(request)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.loadingLayout.setVisibility(View.GONE);

                        JsonObject resultVO = response.body();
                        int code = resultVO.get("code").getAsInt();

                        if (code == 0) {
                            Toast.makeText(getApplicationContext(), "회원가입이 완료되었습니다", Toast.LENGTH_SHORT).show();
                            finish();
                        } else if (code == 21013) {
                            Toast.makeText(getApplicationContext(), "이미 등록된 회원입니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.loadingLayout.setVisibility(View.GONE);
                        Log.e(TAG, "Join member failed: " + t.getMessage());
                        Toast.makeText(getApplicationContext(), "회원가입을 할 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}