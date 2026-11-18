package com.example.elearningportal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.Intent;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoursesFragment extends Fragment {

    EditText etSearchCourse;
    ListView listCourses;
    Button btnRefreshCourses;

    ArrayList<Course> allCourses;
    ArrayList<Course> filteredCourses;

    CourseAdapter adapter;

    ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final int VIEW_COURSE = 1;
    private static final int SHARE_COURSE = 2;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_courses,
                container,
                false);

        // Connect XML components
        etSearchCourse = view.findViewById(
                R.id.etSearchCourse);

        listCourses = view.findViewById(
                R.id.listCourses);

        btnRefreshCourses = view.findViewById(
                R.id.btnRefreshCourses);

        // Initialize course lists
        allCourses = new ArrayList<>();
        filteredCourses = new ArrayList<>();

        // Load sample courses
        loadSampleCourses();

        // Create adapter
        adapter = new CourseAdapter();
        listCourses.setAdapter(adapter);

        // Enable context menu
        registerForContextMenu(listCourses);

        // Search courses
        etSearchCourse.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start,
                            int count, int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s, int start,
                            int before, int count) {

                        filterCourses(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

        // Click course to view details
        listCourses.setOnItemClickListener(
                (parent, itemView, position, id) -> {

                    Course course = filteredCourses.get(position);

                    showCourseDetails(course);
                });

        // Refresh courses
        btnRefreshCourses.setOnClickListener(v -> {
            refreshCourses();
        });

        return view;
    }

    // Sample course data
    private void loadSampleCourses() {

        allCourses.clear();

        allCourses.add(new Course(
                "Computer Science",
                "Study of programming, algorithms and computing.",
                R.drawable.data_science));

        allCourses.add(new Course(
                "Software Engineering",
                "Learn software development and system design.",
                R.drawable.network_engineering));

        allCourses.add(new Course(
                "Information Technology",
                "Study computer systems and IT support.",
                R.drawable.it));

        allCourses.add(new Course(
                "Business Information Systems",
                "Learn how technology supports business operations.",
                R.drawable.bbit));

        allCourses.add(new Course(
                "Computer Networking",
                "Study network infrastructure and communication.",
                R.drawable.network_configuration));

        filteredCourses.clear();
        filteredCourses.addAll(allCourses);
    }

    // Search/filter courses
    private void filterCourses(String keyword) {

        filteredCourses.clear();

        for (Course course : allCourses) {

            if (course.name.toLowerCase()
                    .contains(keyword.toLowerCase())) {

                filteredCourses.add(course);
            }
        }

        adapter.notifyDataSetChanged();
    }

    // Display course details
    private void showCourseDetails(Course course) {

        new AlertDialog.Builder(requireContext())
                .setTitle(course.name)
                .setMessage(course.description)
                .setPositiveButton("OK", null)
                .show();
    }

    // Create Context Menu
    @Override
    public void onCreateContextMenu(
            ContextMenu menu,
            View v,
            ContextMenu.ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("Course Options");

        menu.add(0, VIEW_COURSE, 0, "View Course");

        menu.add(0, SHARE_COURSE, 1, "Share Course");
    }

    // Handle Context Menu selections
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (item.getMenuInfo() instanceof
                android.widget.AdapterView.AdapterContextMenuInfo) {

            android.widget.AdapterView.AdapterContextMenuInfo info =
                    (android.widget.AdapterView.AdapterContextMenuInfo)
                            item.getMenuInfo();

            int position = info.position;

            if (position < 0 ||
                    position >= filteredCourses.size()) {
                return super.onContextItemSelected(item);
            }

            Course course = filteredCourses.get(position);

            if (item.getItemId() == VIEW_COURSE) {

                showCourseDetails(course);
                return true;
            }

            if (item.getItemId() == SHARE_COURSE) {

                shareCourse(course);
                return true;
            }
        }

        return super.onContextItemSelected(item);
    }

    // Share course using implicit Intent
    private void shareCourse(Course course) {

        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");

        intent.putExtra(
                Intent.EXTRA_TEXT,
                course.name + "\n" + course.description);

        startActivity(Intent.createChooser(
                intent, "Share Course"));
    }

    // Refresh using background thread
    private void refreshCourses() {

        btnRefreshCourses.setEnabled(false);

        executor.execute(() -> {

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {

                loadSampleCourses();

                adapter.notifyDataSetChanged();

                etSearchCourse.setText("");

                btnRefreshCourses.setEnabled(true);

                Toast.makeText(requireContext(),
                        "Courses refreshed successfully",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    // Custom Adapter
    class CourseAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredCourses.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredCourses.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(
                int position,
                View convertView,
                ViewGroup parent) {

            View view = convertView;

            if (view == null) {

                view = LayoutInflater.from(requireContext())
                        .inflate(
                                R.layout.row_course,
                                parent,
                                false);
            }

            ImageView image = view.findViewById(
                    R.id.imgCourse);

            TextView name = view.findViewById(
                    R.id.tvCourseName);

            TextView description = view.findViewById(
                    R.id.tvCourseDescription);

            Course course = filteredCourses.get(position);

            image.setImageResource(course.image);

            name.setText(course.name);

            description.setText(course.description);

            return view;
        }
    }

    // Course model
    class Course {

        String name;
        String description;
        int image;

        Course(String name, String description, int image) {

            this.name = name;
            this.description = description;
            this.image = image;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
