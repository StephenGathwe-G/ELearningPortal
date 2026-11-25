package com.example.elearningportal;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.elearningportal.database.AppDatabase;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private TextView tvLastLogin;
    private TextView imgUserAvatar;
    private EditText etHomeSearch;

    private TextView chipAll, chipCS, chipEng, chipBusiness;
    private CardView cardFeaturedCourse1;
    private TextView tvFeaturedTitle, tvFeaturedSubtitle;
    private ImageView imgCourseBanner;

    private AppDatabase db;
    private final List<CoursesFragment.CourseData> loadedCourses = new ArrayList<>();
    private CoursesFragment.CourseData activeFeaturedCourse;

    private String selectedCategoryFilter = "All";
    private String currentSearchQuery = "";

    private static final String TAG = "HOME_LIFECYCLE";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext());

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvLastLogin = view.findViewById(R.id.tvLastLogin);
        imgUserAvatar = view.findViewById(R.id.imgUserAvatar);
        etHomeSearch = view.findViewById(R.id.etHomeSearch);

        chipAll = view.findViewById(R.id.chipAll);
        chipCS = view.findViewById(R.id.chipCS);
        chipEng = view.findViewById(R.id.chipEng);
        chipBusiness = view.findViewById(R.id.chipBusiness);

        cardFeaturedCourse1 = view.findViewById(R.id.cardFeaturedCourse1);
        imgCourseBanner = view.findViewById(R.id.imgCourseBanner);

        // Subtitle & Title inside featured course card
        if (cardFeaturedCourse1 != null) {
            ViewGroup innerLayout = (ViewGroup) cardFeaturedCourse1.getChildAt(0);
            if (innerLayout != null && innerLayout.getChildCount() > 1) {
                ViewGroup textContainer = (ViewGroup) innerLayout.getChildAt(1);
                if (textContainer != null && textContainer.getChildCount() >= 2) {
                    tvFeaturedTitle = (TextView) textContainer.getChildAt(0);
                    tvFeaturedSubtitle = (TextView) textContainer.getChildAt(1);
                }
            }
            cardFeaturedCourse1.setOnClickListener(v -> {
                if (activeFeaturedCourse != null) {
                    openCourseDetail(activeFeaturedCourse.id);
                }
            });
        }

        setupSearchAndChips();
        loadStudentName();
        loadLastLogin();
        loadCoursesFromDatabase();
    }

    private void setupSearchAndChips() {
        if (etHomeSearch != null) {
            etHomeSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString();
                    applyCourseFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        View.OnClickListener chipClickListener = v -> {
            resetChips();
            TextView selectedChip = (TextView) v;
            selectedChip.setBackgroundResource(R.drawable.bg_chip_selected);
            selectedChip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

            if (v == chipAll) selectedCategoryFilter = "All";
            else if (v == chipCS) selectedCategoryFilter = "CS";
            else if (v == chipEng) selectedCategoryFilter = "Eng";
            else if (v == chipBusiness) selectedCategoryFilter = "Business";

            applyCourseFilters();
        };

        if (chipAll != null) chipAll.setOnClickListener(chipClickListener);
        if (chipCS != null) chipCS.setOnClickListener(chipClickListener);
        if (chipEng != null) chipEng.setOnClickListener(chipClickListener);
        if (chipBusiness != null) chipBusiness.setOnClickListener(chipClickListener);
    }

    private void resetChips() {
        TextView[] chips = {chipAll, chipCS, chipEng, chipBusiness};
        int textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.text_primary);
        for (TextView chip : chips) {
            if (chip != null) {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                chip.setTextColor(textPrimaryColor);
            }
        }
    }

    private void loadCoursesFromDatabase() {
        executor.execute(() -> {
            List<CoursesFragment.CourseData> list = new ArrayList<>();
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
                    if (imageRes == 0) imageRes = R.drawable.campus_building;

                    list.add(new CoursesFragment.CourseData(id, code, title, desc, dept, hours, modules, imageRes));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    loadedCourses.clear();
                    loadedCourses.addAll(list);
                    applyCourseFilters();
                });
            }
        });
    }

    private void applyCourseFilters() {
        if (loadedCourses.isEmpty()) return;

        CoursesFragment.CourseData matchedCourse = null;
        String q = currentSearchQuery.toLowerCase().trim();

        for (CoursesFragment.CourseData c : loadedCourses) {
            boolean matchesCategory = true;
            if ("CS".equals(selectedCategoryFilter)) {
                matchesCategory = c.code.equalsIgnoreCase("BCS") || c.title.toLowerCase().contains("computer science");
            } else if ("Eng".equals(selectedCategoryFilter)) {
                matchesCategory = c.code.equalsIgnoreCase("BSE") || c.code.equalsIgnoreCase("BED") || c.department.toLowerCase().contains("engineering");
            } else if ("Business".equals(selectedCategoryFilter)) {
                matchesCategory = c.code.equalsIgnoreCase("BCF") || c.department.toLowerCase().contains("business");
            }

            boolean matchesSearch = q.isEmpty() ||
                    c.title.toLowerCase().contains(q) ||
                    c.code.toLowerCase().contains(q) ||
                    c.department.toLowerCase().contains(q);

            if (matchesCategory && matchesSearch) {
                matchedCourse = c;
                break;
            }
        }

        if (matchedCourse == null && !loadedCourses.isEmpty()) {
            matchedCourse = loadedCourses.get(0);
        }

        activeFeaturedCourse = matchedCourse;

        if (activeFeaturedCourse != null && tvFeaturedTitle != null) {
            tvFeaturedTitle.setText(activeFeaturedCourse.title);
            if (tvFeaturedSubtitle != null) {
                tvFeaturedSubtitle.setText(activeFeaturedCourse.modulesCount + " Modules • " + activeFeaturedCourse.durationHours + " Hours • " + activeFeaturedCourse.code);
            }
            if (imgCourseBanner != null) {
                imgCourseBanner.setImageResource(activeFeaturedCourse.imageRes != 0 ? activeFeaturedCourse.imageRes : R.drawable.campus_building);
            }
        }
    }

    private void openCourseDetail(int courseId) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, CourseDetailFragment.newInstance(courseId))
                .addToBackStack(null)
                .commit();
    }

    private void loadStudentName() {
        String studentName = null;

        if (getActivity() != null) {
            studentName = getActivity().getIntent().getStringExtra("studentName");
        }

        if (studentName == null || studentName.trim().isEmpty()) {
            SharedPreferences preferences = requireActivity().getSharedPreferences(
                    "StudentPortalPrefs", Context.MODE_PRIVATE);
            studentName = preferences.getString("studentName", "Student");
        }

        if (tvWelcome != null) {
            tvWelcome.setText("Welcome back, " + studentName + "! 👋");
        }

        if (imgUserAvatar != null && !studentName.isEmpty()) {
            imgUserAvatar.setText(String.valueOf(studentName.charAt(0)).toUpperCase());
        }
    }

    private void loadLastLogin() {
        String timestamp = null;

        try {
            FileInputStream fis = requireActivity().openFileInput("last_login.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            timestamp = reader.readLine();
            reader.close();
        } catch (Exception e) {
            Log.d(TAG, "No file timestamp found");
        }

        if (timestamp == null || timestamp.trim().isEmpty()) {
            SharedPreferences preferences = requireActivity().getSharedPreferences(
                    "StudentPortalPrefs", Context.MODE_PRIVATE);
            timestamp = preferences.getString("last_login", null);
        }

        if (tvLastLogin != null) {
            if (timestamp != null && !timestamp.trim().isEmpty()) {
                tvLastLogin.setText("Last login: " + timestamp);
            } else {
                tvLastLogin.setText("Last login: Not available");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showMainNavigation();
        }
        loadLastLogin();
        loadCoursesFromDatabase();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}