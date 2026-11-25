package com.example.elearningportal;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elearningportal.database.AppDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoursesFragment extends Fragment {

    private EditText etSearchCourse;
    private RecyclerView recyclerCourses;
    private Button btnAddCourse;
    private Button btnRefreshCourses;

    private AppDatabase db;
    private CourseAdapter adapter;

    private final List<CourseData> allCourses = new ArrayList<>();
    private final List<CourseData> filteredCourses = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class CourseData {
        public int id;
        public String code;
        public String title;
        public String description;
        public String department;
        public int durationHours;
        public int modulesCount;
        public int imageRes;

        public CourseData(int id, String code, String title, String description, String department, int durationHours, int modulesCount, int imageRes) {
            this.id = id;
            this.code = code;
            this.title = title;
            this.description = description;
            this.department = department;
            this.durationHours = durationHours;
            this.modulesCount = modulesCount;
            this.imageRes = imageRes;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);

        db = AppDatabase.getInstance(requireContext());

        etSearchCourse = view.findViewById(R.id.etSearchCourse);
        recyclerCourses = view.findViewById(R.id.recyclerCourses);
        btnAddCourse = view.findViewById(R.id.btnAddCourse);
        btnRefreshCourses = view.findViewById(R.id.btnRefreshCourses);

        recyclerCourses.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CourseAdapter();
        recyclerCourses.setAdapter(adapter);

        etSearchCourse.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCourses(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnAddCourse != null) {
            btnAddCourse.setOnClickListener(v -> openAddCourseFragment());
        }

        if (btnRefreshCourses != null) {
            btnRefreshCourses.setOnClickListener(v -> loadCoursesFromDb());
        }

        loadCoursesFromDb();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showMainNavigation();
        }
        loadCoursesFromDb();
    }

    private void loadCoursesFromDb() {
        executor.execute(() -> {
            List<CourseData> list = new ArrayList<>();
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
                    int id = cursor.getInt(idIdx);
                    String code = cursor.getString(codeIdx);
                    String title = cursor.getString(titleIdx);
                    String desc = cursor.getString(descIdx);
                    String dept = cursor.getString(deptIdx);
                    int hours = cursor.getInt(hoursIdx);
                    int modules = cursor.getInt(modIdx);
                    int imageRes = cursor.getInt(imgIdx);
                    if (imageRes == 0) imageRes = R.drawable.brain;

                    list.add(new CourseData(id, code, title, desc, dept, hours, modules, imageRes));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    allCourses.clear();
                    allCourses.addAll(list);
                    filterCourses(etSearchCourse.getText().toString());
                });
            }
        });
    }

    private void filterCourses(String query) {
        filteredCourses.clear();
        String lowerQuery = query.toLowerCase().trim();
        for (CourseData c : allCourses) {
            if (c.title.toLowerCase().contains(lowerQuery) ||
                c.code.toLowerCase().contains(lowerQuery) ||
                c.department.toLowerCase().contains(lowerQuery)) {
                filteredCourses.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void openCourseDetailFragment(CourseData course) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, CourseDetailFragment.newInstance(course.id))
                .addToBackStack(null)
                .commit();
    }

    private void openAddCourseFragment() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, CourseAddEditFragment.newInstance(-1))
                .addToBackStack(null)
                .commit();
    }

    class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CourseData course = filteredCourses.get(position);

            holder.courseName.setText(course.title + " (" + course.code + ")");
            holder.courseDescription.setText(course.description);
            holder.courseImage.setImageResource(course.imageRes);

            holder.itemView.setOnClickListener(v -> openCourseDetailFragment(course));
        }

        @Override
        public int getItemCount() {
            return filteredCourses.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView courseImage;
            TextView courseName;
            TextView courseDescription;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                courseImage = itemView.findViewById(R.id.courseImage);
                courseName = itemView.findViewById(R.id.courseName);
                courseDescription = itemView.findViewById(R.id.courseDescription);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}