package com.example.elearningportal;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.elearningportal.database.AppDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseAddEditFragment extends Fragment {

    private static final String ARG_COURSE_ID = "course_id";

    private int courseId = -1;
    private AppDatabase db;

    private TextView tvAddEditHeader;
    private EditText etCourseCode, etCourseTitle, etCourseDept, etCourseHours, etCourseModules, etCourseDesc;
    private Button btnSaveCourse, btnCancelCourse;
    private View btnCourseAddEditBack;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static CourseAddEditFragment newInstance(int courseId) {
        CourseAddEditFragment fragment = new CourseAddEditFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COURSE_ID, courseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            courseId = getArguments().getInt(ARG_COURSE_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_add_edit, container, false);

        db = AppDatabase.getInstance(requireContext());

        btnCourseAddEditBack = view.findViewById(R.id.btnCourseAddEditBack);
        tvAddEditHeader = view.findViewById(R.id.tvAddEditHeader);
        etCourseCode = view.findViewById(R.id.etCourseCode);
        etCourseTitle = view.findViewById(R.id.etCourseTitle);
        etCourseDept = view.findViewById(R.id.etCourseDept);
        etCourseHours = view.findViewById(R.id.etCourseHours);
        etCourseModules = view.findViewById(R.id.etCourseModules);
        etCourseDesc = view.findViewById(R.id.etCourseDesc);
        btnSaveCourse = view.findViewById(R.id.btnSaveCourse);
        btnCancelCourse = view.findViewById(R.id.btnCancelCourse);

        if (btnCourseAddEditBack != null) {
            btnCourseAddEditBack.setOnClickListener(v -> navigateBack());
        }

        if (btnCancelCourse != null) {
            btnCancelCourse.setOnClickListener(v -> navigateBack());
        }

        if (btnSaveCourse != null) {
            btnSaveCourse.setOnClickListener(v -> saveCourse());
        }

        if (courseId != -1) {
            tvAddEditHeader.setText("Edit Course Program");
            loadExistingCourseData();
        } else {
            tvAddEditHeader.setText("Add New Course Program");
        }

        return view;
    }

    private void loadExistingCourseData() {
        executor.execute(() -> {
            try (Cursor cursor = db.queryAllCourses()) {
                int idIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_ID);
                int codeIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_CODE);
                int titleIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_TITLE);
                int descIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_DESC);
                int deptIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_DEPT);
                int hoursIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_HOURS);
                int modIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_MODULES);

                while (cursor.moveToNext()) {
                    if (cursor.getInt(idIdx) == courseId) {
                        String code = cursor.getString(codeIdx);
                        String title = cursor.getString(titleIdx);
                        String desc = cursor.getString(descIdx);
                        String dept = cursor.getString(deptIdx);
                        int hours = cursor.getInt(hoursIdx);
                        int modules = cursor.getInt(modIdx);

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                etCourseCode.setText(code);
                                etCourseTitle.setText(title);
                                etCourseDept.setText(dept);
                                etCourseHours.setText(String.valueOf(hours));
                                etCourseModules.setText(String.valueOf(modules));
                                etCourseDesc.setText(desc);
                            });
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void saveCourse() {
        String code = etCourseCode.getText().toString().trim();
        String title = etCourseTitle.getText().toString().trim();
        String dept = etCourseDept.getText().toString().trim();
        String desc = etCourseDesc.getText().toString().trim();
        int hours = tryParseInt(etCourseHours.getText().toString().trim(), 40);
        int modules = tryParseInt(etCourseModules.getText().toString().trim(), 10);

        if (code.isEmpty()) {
            etCourseCode.setError("Enter course code");
            etCourseCode.requestFocus();
            return;
        }

        if (title.isEmpty()) {
            etCourseTitle.setError("Enter course title");
            etCourseTitle.requestFocus();
            return;
        }

        executor.execute(() -> {
            long result;
            if (courseId == -1) {
                result = db.insertCourse(code, title, desc, dept, hours, modules, R.drawable.brain);
            } else {
                result = db.updateCourse(courseId, code, title, desc, dept, hours, modules);
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (result > 0) {
                        Toast.makeText(requireContext(), "Course saved successfully", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    } else {
                        Toast.makeText(requireContext(), "Failed to save course", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private int tryParseInt(String val, int defaultVal) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private void navigateBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}