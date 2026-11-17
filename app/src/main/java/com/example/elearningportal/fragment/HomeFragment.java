package com.example.elearningportal.fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.elearningportal.R;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class HomeFragment extends Fragment{

    private TextView tvWelcome;
    private TextView tvLastLogin;

    private static final String TAG = "HOME_LIFECYCLE";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        return inflater.inflate(
                R.layout.fragment_home,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvLastLogin = view.findViewById(R.id.tvLastLogin);

        loadStudentName();
        loadLastLogin();
    }

    private void loadStudentName() {

        String studentName = null;

        if (getActivity() != null) {

            studentName = getActivity()
                    .getIntent()
                    .getStringExtra("studentName");
        }

        if (studentName == null || studentName.trim().isEmpty()) {

            SharedPreferences preferences =
                    requireActivity().getSharedPreferences(
                            "StudentPortalPrefs",
                            Context.MODE_PRIVATE
                    );

            studentName = preferences.getString(
                    "studentName",
                    "Student"
            );
        }

        tvWelcome.setText("Welcome, " + studentName + "!");
    }

    private void loadLastLogin() {

        try {

            FileInputStream fis =
                    requireActivity().openFileInput("last_login.txt");

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(fis));

            String timestamp = reader.readLine();

            reader.close();

            if (timestamp != null) {

                tvLastLogin.setText(
                        "Last login: " + timestamp
                );
            }

        } catch (Exception e) {

            tvLastLogin.setText(
                    "Last login: Not available"
            );

            Log.d(TAG, "No previous login file found");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

}
