package com.example.elearningportal;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.elearningportal.database.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseDetailFragment extends Fragment {

    private static final String ARG_COURSE_ID = "course_id";

    private int courseId;
    private AppDatabase db;

    private TextView tvCourseTitle, tvCourseCodeDept, tvModulesBadge, tvHoursBadge, tvDeptBadge;
    private TextView tvCourseDescription, tvEnrolledStudents, tvEnrolledHeader;
    private ImageView imgCourseBanner;
    private View btnCourseDetailBack;

    private CoursesFragment.CourseData currentCourse;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static CourseDetailFragment newInstance(int courseId) {
        CourseDetailFragment fragment = new CourseDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COURSE_ID, courseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            courseId = getArguments().getInt(ARG_COURSE_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_detail, container, false);

        db = AppDatabase.getInstance(requireContext());

        btnCourseDetailBack = view.findViewById(R.id.btnCourseDetailBack);
        imgCourseBanner = view.findViewById(R.id.imgCourseBanner);
        tvCourseTitle = view.findViewById(R.id.tvCourseTitle);
        tvCourseCodeDept = view.findViewById(R.id.tvCourseCodeDept);
        tvModulesBadge = view.findViewById(R.id.tvModulesBadge);
        tvHoursBadge = view.findViewById(R.id.tvHoursBadge);
        tvDeptBadge = view.findViewById(R.id.tvDeptBadge);
        tvCourseDescription = view.findViewById(R.id.tvCourseDescription);
        tvEnrolledHeader = view.findViewById(R.id.tvEnrolledHeader);
        tvEnrolledStudents = view.findViewById(R.id.tvEnrolledStudents);

        if (btnCourseDetailBack != null) {
            btnCourseDetailBack.setOnClickListener(v -> navigateBack());
        }

        loadCourseData();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notifyMainActivityOfDetailMode();
    }

    private void notifyMainActivityOfDetailMode() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showDetailNavigation(
                    () -> showEditFragment(),
                    () -> confirmDeleteCourse()
            );
        }
    }

    private void loadCourseData() {
        executor.execute(() -> {
            CoursesFragment.CourseData course = null;
            try (Cursor cursor = db.queryAllCourses()) {
                int idIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_ID);
                int codeIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_CODE);
                int titleIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_TITLE);
                int descIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_DESC);
                int deptIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_DEPT);
                int hoursIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_HOURS);
                int modIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_MODULES);
                int imgIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_C_IMAGE);

                while (cursor.moveToNext()) {
                    if (cursor.getInt(idIdx) == courseId) {
                        int id = cursor.getInt(idIdx);
                        String code = cursor.getString(codeIdx);
                        String title = cursor.getString(titleIdx);
                        String desc = cursor.getString(descIdx);
                        String dept = cursor.getString(deptIdx);
                        int hours = cursor.getInt(hoursIdx);
                        int modules = cursor.getInt(modIdx);
                        int imageRes = cursor.getInt(imgIdx);
                        if (imageRes == 0) imageRes = R.drawable.campus_building;

                        course = new CoursesFragment.CourseData(id, code, title, desc, dept, hours, modules, imageRes);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final CoursesFragment.CourseData finalCourse = course;
            if (finalCourse != null) {
                List<String> studentNames = getEnrolledStudentsList(finalCourse.code);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        currentCourse = finalCourse;
                        bindCourseToUI(finalCourse, studentNames);
                    });
                }
            }
        });
    }

    private List<String> getEnrolledStudentsList(String courseCode) {
        List<String> names = new ArrayList<>();
        try (Cursor cursor = db.queryAll()) {
            int nameIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_NAME);
            int courseIdx = cursor.getColumnIndexOrThrow(AppDatabase.COLUMN_COURSE);

            while (cursor.moveToNext()) {
                String cCode = cursor.getString(courseIdx);
                if (cCode != null && cCode.equalsIgnoreCase(courseCode)) {
                    String sName = cursor.getString(nameIdx);
                    if (sName != null) names.add(sName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return names;
    }

    private void bindCourseToUI(CoursesFragment.CourseData course, List<String> studentNames) {
        tvCourseTitle.setText(course.title);
        tvCourseCodeDept.setText(course.code + " • " + course.department);
        tvModulesBadge.setText(course.modulesCount + " Modules");
        tvHoursBadge.setText(course.durationHours + " Hours");
        tvDeptBadge.setText(course.department);
        tvCourseDescription.setText(course.description);
        imgCourseBanner.setImageResource(course.imageRes != 0 ? course.imageRes : R.drawable.campus_building);

        tvEnrolledHeader.setText("Enrolled Students (" + studentNames.size() + ")");
        if (studentNames.isEmpty()) {
            tvEnrolledStudents.setText("No students currently registered for this course program.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < studentNames.size(); i++) {
                sb.append("• ").append(studentNames.get(i));
                if (i < studentNames.size() - 1) sb.append("\n");
            }
            tvEnrolledStudents.setText(sb.toString());
        }
    }

    private void showEditFragment() {
        if (currentCourse == null) return;
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, CourseAddEditFragment.newInstance(currentCourse.id))
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeleteCourse() {
        if (currentCourse == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete " + currentCourse.title + " (" + currentCourse.code + ")?")
                .setPositiveButton("Delete", (d, w) -> {
                    executor.execute(() -> {
                        db.deleteCourse(currentCourse.id);
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Course deleted", Toast.LENGTH_SHORT).show();
                                navigateBack();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showMainNavigation();
        }
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}