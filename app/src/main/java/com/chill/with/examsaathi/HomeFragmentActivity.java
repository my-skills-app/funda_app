package com.chill.with.examsaathi;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragmentActivity extends Fragment {

    private static final int TOP_BAR_FADE_SCROLL_PX = 140;
    /**
     * Billboard closer to poster ratio (1080x1620 ≈ 0.67 W/H).
     * Slightly wider center card + taller frame so image relation fills better.
     */
    /** Side peek — kam padding = zyada width for main image */
    private static final int BANNER_PEEK_DP = 28;
    /** Negative margin → pages overlap (sides tuck behind center) */
    private static final int BANNER_PAGE_OVERLAP_DP = 44;
    private static final float BANNER_SIDE_MIN_SCALE = 0.84f;
    private static final float BANNER_SIDE_MIN_ALPHA = 0.72f;
    private static final int BANNER_DOT_SIZE_DP = 8;
    private static final int BANNER_DOT_GAP_DP = 6;
    /** Virtual pages for seamless left/right loop (realCount * this) */
    private static final int BANNER_LOOP_MULTIPLIER = 500;
    /** Pause between auto slides */
    private static final long BANNER_AUTO_SCROLL_MS = 3000L;
    private static final int BANNER_MIN_HEIGHT_DP = 230;
    private static final int BANNER_HEIGHT_BOOST_DP = 28;
    private static final float BANNER_MAX_SCREEN_FRACTION = 0.58f;
    /** Closer to poster W/H → taller carousel (was 0.85 shorter) */
    private static final float BANNER_NETFLIX_ASPECT = 0.72f;
    private boolean bannerHeightResolved;
    private static final int CATEGORY_ALL_INDEX = 0;
    private static final int ROW_SECTION = 0;
    private static final int ROW_BANNER = 1;
    private static final int ROW_GITA_AUDIO = 2;
    private static final int ROW_SUBJECT_GRID = 3;
    private static final int ROW_TRENDING = 4;
    private static final int GITA_AUDIO_ROW_AFTER_SECTIONS = 2;
    private static final int SUBJECT_GRID_VISIBLE = 7;

    private SwipeRefreshLayout swipeRefreshLayout;
    private NestedScrollView nestedScroll;
    private View toolbarSpacer;
    private LinearLayout topBarOverlay;
    private View btnProfile;
    private ImageView imgProfileIcon;
    private View btnCourseSelector;
    private ImageView imgCourseThumb;
    private TextView txtCourseName;
    private ShimmerFrameLayout shimmerUserName;
    private TextView txtUserName;
    private LinearLayout layoutCategoryChips;
    @Nullable
    private PrepExamRepository.SelectedCourse selectedCourse;
    private CardView cardShlokaDay;
    private ImageView imgShlokaDay;
    private TextView txtShlokaDayTitle;
    private TextView txtShlokaDayDesc;
    private View shimmerHome;
    private TextView txtError;
    private RecyclerView recyclerSections;
    private View layoutRecentViewsSection;
    private RecyclerView recyclerRecentViews;
    private View layoutFullGitaAudioSection;
    private View layoutFullGitaAudio;
    private View layoutFullAudioPlayer;
    private View fullAudioMediaBackdrop;
    private ImageView imgFullAudioBackground;
    private TextView txtFullAudioChapterBadge;
    private TextView txtFullAudioPositionBadge;
    private TextView txtFullAudioNowPlaying;
    private TextView txtFullAudioChapterLine;
    private TextView txtFullAudioElapsed;
    private TextView txtFullAudioDuration;
    private SeekBar seekFullAudio;
    private ImageButton btnFullAudioChapterPrev;
    private ImageButton btnFullAudioPlayPause;
    private ImageButton btnFullAudioChapterNext;
    private ProgressBar progressFullAudioBuffering;
    private GitaFullAudioPlaybackService audioService;
    private boolean audioServiceBound;
    private boolean pendingStartPlayback;
    private boolean pendingPlaybackAfterNotificationPermission;
    private boolean userSeekingAudio;

    private ActivityResultLauncher<String> postNotificationsPermissionLauncher;

    private DatabaseReference userProfileRef;
    private String loadedProfileUid;
    private Timer bannerTimer;
    private int bannerIndex;
    private ViewPager activeBannerPager;
    private List<GitaDashboard.Banner> bannerList = new ArrayList<>();
    private List<GitaDashboard.Section> allSections = new ArrayList<>();
    private int selectedCategoryIndex = 0;
    private boolean dashboardLoaded = false;
    private boolean fullGitaAudioLoading = false;
    private boolean playOverlayLoadingVisible = false;
    private static final String GITA_AUDIO_LISTENERS_TAG = "gita_audio_listeners";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ServiceConnection audioServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GitaFullAudioPlaybackService.LocalBinder binder = (GitaFullAudioPlaybackService.LocalBinder) service;
            audioService = binder.getService();
            audioServiceBound = true;
            audioService.setUiListener(playbackUiListener);
            if (pendingStartPlayback) {
                pendingStartPlayback = false;
                fullGitaAudioLoading = false;
                setFullGitaAudioLoadingUi(false);
                audioService.startFullGitaPlayback();
            } else {
                refreshGitaAudioUiFromService();
                requestGitaAudioPreload();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioService = null;
            audioServiceBound = false;
        }
    };

    private final GitaFullAudioPlaybackService.UiListener playbackUiListener = new GitaFullAudioPlaybackService.UiListener() {
        @Override
        public void onPlaybackUpdate(GitaFullAudioPlaybackService.PlaybackSnapshot snapshot) {
            if (!isAdded()) {
                return;
            }
            mainHandler.post(() -> applyPlaybackSnapshot(snapshot));
        }

        @Override
        public void onPlaybackStopped() {
            if (!isAdded()) {
                return;
            }
            mainHandler.post(() -> {
                boolean failedWhileStarting = fullGitaAudioLoading || pendingStartPlayback;
                showFullAudioPlayerShell();
                pendingStartPlayback = false;
                if (failedWhileStarting) {
                    AppToast.show(requireContext(), R.string.home_full_gita_audio_unavailable);
                }
            });
        }
    };

    private static final class HomeCategory {
        final String label;
        final boolean showAll;
        final String[] titleKeywords;

        HomeCategory(String label, boolean showAll, String... titleKeywords) {
            this.label = label;
            this.showAll = showAll;
            this.titleKeywords = titleKeywords;
        }

        boolean matches(GitaDashboard.Section section) {
            if (section == null) {
                return false;
            }
            String title = section.sectionTitle != null
                    ? section.sectionTitle.toLowerCase(Locale.US)
                    : "";
            String type = section.sectionType != null
                    ? section.sectionType.toLowerCase(Locale.US)
                    : "";
            for (String keyword : titleKeywords) {
                String key = keyword.toLowerCase(Locale.US);
                if (title.contains(key) || type.contains(key.replace(" ", "_"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private final HomeCategory[] HOME_CATEGORIES = {
            new HomeCategory("All", true),
            new HomeCategory("Education", false, "think like krishna", "popular shlokas"),
            new HomeCategory("Professionals", false, "before a big decision", "need strength"),
            new HomeCategory("Overthinking", false, "over-thinking", "cant_stop_overthinking"),
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postNotificationsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (pendingPlaybackAfterNotificationPermission) {
                        pendingPlaybackAfterNotificationPermission = false;
                        beginFullGitaAudioPlayback();
                    }
                });
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        bindViews(view);
        FirebaseApp.initializeApp(requireContext());

        View profileButton = view.findViewById(R.id.btnProfile);
        if (profileButton != null) {
            profileButton.setOnClickListener(v -> openProfile());
        }
        if (btnCourseSelector != null) {
            btnCourseSelector.setOnClickListener(v -> openCoursePicker());
        }

        swipeRefreshLayout.setColorSchemeColors(requireContext().getColor(R.color.app_color));
        swipeRefreshLayout.setOnRefreshListener(this::refreshHomeFromPull);

        attachUserProfileListener();
        loadSelectedCourseIntoTopBar();
        setupTopBarAndInsets(view);
        if (dashboardLoaded && !allSections.isEmpty()) {
            showHomeShimmer(false);
            swipeRefreshLayout.setRefreshing(false);
            txtError.setVisibility(View.GONE);
            recyclerSections.setVisibility(View.VISIBLE);
            applyCategoryFilter();
            updateFullGitaAudioCardVisibility();
        } else {
            loadDashboard();
        }
        return view;
    }

    private void bindViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        nestedScroll = view.findViewById(R.id.nestedScroll);
        toolbarSpacer = view.findViewById(R.id.toolbarSpacer);
        topBarOverlay = view.findViewById(R.id.topBarOverlay);
        btnProfile = view.findViewById(R.id.btnProfile);
        imgProfileIcon = view.findViewById(R.id.imgProfileIcon);
        btnCourseSelector = view.findViewById(R.id.btnCourseSelector);
        imgCourseThumb = view.findViewById(R.id.imgCourseThumb);
        txtCourseName = view.findViewById(R.id.txtCourseName);
        shimmerUserName = view.findViewById(R.id.shimmerUserName);
        txtUserName = view.findViewById(R.id.txtUserName);
        layoutCategoryChips = view.findViewById(R.id.layoutCategoryChips);
        cardShlokaDay = view.findViewById(R.id.cardShlokaDay);
        imgShlokaDay = view.findViewById(R.id.imgShlokaDay);
        txtShlokaDayTitle = view.findViewById(R.id.txtShlokaDayTitle);
        txtShlokaDayDesc = view.findViewById(R.id.txtShlokaDayDesc);
        shimmerHome = view.findViewById(R.id.shimmerHome);
        txtError = view.findViewById(R.id.txtError);
        recyclerSections = view.findViewById(R.id.recyclerSections);
        layoutRecentViewsSection = view.findViewById(R.id.layoutRecentViewsSection);
        recyclerRecentViews = view.findViewById(R.id.recyclerRecentViews);

        setupCategoryChips();
        recyclerSections.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSections.setNestedScrollingEnabled(false);
        recyclerRecentViews.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerRecentViews.setNestedScrollingEnabled(false);

        updateFullGitaAudioCardVisibility();
    }

    @Override
    public void onStart() {
        super.onStart();
        bindAudioServiceIfNeeded();
    }

    @Override
    public void onStop() {
        if (audioService != null) {
            audioService.setUiListener(null);
        }
        unbindAudioService();
        super.onStop();
    }

    private void openProfile() {
        startActivity(new Intent(requireContext(), ProfileActivity.class));
    }

    private void openCoursePicker() {
        if (!isAdded()) {
            return;
        }
        ExamOnboardingActivity.open(requireContext(), true);
    }

    private void loadSelectedCourseIntoTopBar() {
        PrepExamRepository.SelectedCourse local = PrepCourseStore.getActive(requireContext());
        if (local != null) {
            applySelectedCourseToTopBar(local);
            return;
        }
        new Thread(() -> {
            try {
                PrepExamRepository.SelectedCourse course = PrepExamRepository.loadSelectedCourse();
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (course != null) {
                        PrepCourseStore.addOrUpdate(requireContext(), course, true);
                    }
                    applySelectedCourseToTopBar(course);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (isAdded() && txtCourseName != null) {
                        txtCourseName.setText(R.string.prep_select_course);
                    }
                });
            }
        }).start();
    }

    private void applySelectedCourseToTopBar(@Nullable PrepExamRepository.SelectedCourse course) {
        selectedCourse = course;
        if (txtCourseName == null) {
            return;
        }
        if (course == null || course.examName == null || course.examName.trim().isEmpty()) {
            txtCourseName.setText(R.string.prep_select_course);
            if (imgCourseThumb != null) {
                imgCourseThumb.setImageResource(R.drawable.default_image);
            }
            return;
        }
        txtCourseName.setText(course.examName.trim());
        if (imgCourseThumb != null) {
            String thumb = course.thumbnailUrl != null ? course.thumbnailUrl.trim() : "";
            if (!thumb.isEmpty()) {
                Glide.with(this).load(thumb).circleCrop().into(imgCourseThumb);
            } else {
                imgCourseThumb.setImageResource(R.drawable.default_image);
            }
        }
    }

    private void setupCategoryChips() {
        if (layoutCategoryChips == null) {
            return;
        }
        layoutCategoryChips.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < HOME_CATEGORIES.length; i++) {
            final int index = i;
            View chipView = inflater.inflate(R.layout.item_home_category_chip, layoutCategoryChips, false);
            TextView chip = chipView.findViewById(R.id.txtCategoryChip);
            chip.setText(HOME_CATEGORIES[i].label);
            updateCategoryChipStyle(chip, index == selectedCategoryIndex);
            chip.setOnClickListener(v -> {
                if (selectedCategoryIndex != index) {
                    selectedCategoryIndex = index;
                    refreshCategoryChipStyles();
                    applyCategoryFilter();
                }
            });
            layoutCategoryChips.addView(chipView);
        }
    }

    private void refreshCategoryChipStyles() {
        for (int i = 0; i < layoutCategoryChips.getChildCount(); i++) {
            View child = layoutCategoryChips.getChildAt(i);
            TextView chip = child.findViewById(R.id.txtCategoryChip);
            updateCategoryChipStyle(chip, i == selectedCategoryIndex);
        }
    }

    private void updateCategoryChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected
                ? R.drawable.bg_home_category_chip_selected
                : R.drawable.bg_home_category_chip);
        chip.setTextColor(requireContext().getColor(
                selected ? R.color.white : R.color.text_color));
    }

    private void refreshHomeFromPull() {
        resetToAllCategory();
        fetchDashboardFromNetwork();
    }

    private void resetToAllCategory() {
        selectedCategoryIndex = CATEGORY_ALL_INDEX;
        refreshCategoryChipStyles();
        if (nestedScroll != null) {
            nestedScroll.scrollTo(0, 0);
        }
    }

    private boolean isHiddenHomeSection(@Nullable GitaDashboard.Section section) {
        if (section == null) {
            return true;
        }
        String type = section.sectionType != null ? section.sectionType.trim().toLowerCase(Locale.US) : "";
        String title = section.sectionTitle != null ? section.sectionTitle.trim().toLowerCase(Locale.US) : "";
        return "continue_watching".equals(type)
                || type.contains("continue_watching")
                || "continue watching".equals(title)
                || title.contains("continue watching");
    }

    private List<GitaDashboard.Section> filterSectionsForCategory(int categoryIndex) {
        List<GitaDashboard.Section> base = new ArrayList<>();
        for (GitaDashboard.Section section : allSections) {
            if (!isHiddenHomeSection(section)) {
                base.add(section);
            }
        }
        if (categoryIndex < 0 || categoryIndex >= HOME_CATEGORIES.length) {
            return base;
        }
        HomeCategory category = HOME_CATEGORIES[categoryIndex];
        if (category.showAll) {
            return base;
        }
        List<GitaDashboard.Section> filtered = new ArrayList<>();
        for (GitaDashboard.Section section : base) {
            if (category.matches(section)) {
                filtered.add(section);
            }
        }
        return filtered.isEmpty() ? base : filtered;
    }

    private void applyCategoryFilter() {
        if (!isAdded() || recyclerSections == null) {
            return;
        }
        List<GitaDashboard.Section> filtered = filterSectionsForCategory(selectedCategoryIndex);
        recyclerSections.setAdapter(new HomeFeedAdapter(buildFeedRows(filtered)));
        refreshRecentViewsSection();
    }

    private void setupTopBarAndInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int topPadding = statusTop + dp(6);
            topBarOverlay.setPadding(
                    topBarOverlay.getPaddingLeft(),
                    topPadding,
                    topBarOverlay.getPaddingRight(),
                    topBarOverlay.getPaddingBottom());
            topBarOverlay.post(this::updateToolbarSpacerHeight);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        nestedScroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY,
                oldScrollX, oldScrollY) -> updateTopBarScrim(scrollY));
        nestedScroll.getViewTreeObserver()
                .addOnScrollChangedListener(() -> updateTopBarScrim(nestedScroll.getScrollY()));
    }

    private void updateToolbarSpacerHeight() {
        if (toolbarSpacer == null || topBarOverlay == null)
            return;
        int h = topBarOverlay.getHeight();
        if (h > 0) {
            ViewGroup.LayoutParams lp = toolbarSpacer.getLayoutParams();
            lp.height = h;
            toolbarSpacer.setLayoutParams(lp);
        }
    }

    private void updateTopBarScrim(int scrollY) {
        if (topBarOverlay == null)
            return;
        float factor = Math.min(1f, scrollY / (float) TOP_BAR_FADE_SCROLL_PX);
        int alpha = (int) (factor * 200);
        topBarOverlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
    }

    private void configureBannerPager(ViewPager pager) {
        int peek = dp(BANNER_PEEK_DP);
        int overlap = dp(BANNER_PAGE_OVERLAP_DP);
        pager.setClipToPadding(false);
        pager.setClipChildren(false);
        // Side posters peek; bottom tight so dots sit ~10dp below
        pager.setPadding(peek, dp(4), peek, dp(4));
        // Overlap pages so sides tuck behind the focused card
        pager.setPageMargin(-overlap);
        pager.setOffscreenPageLimit(3);
        // reverseDrawingOrder → center draws above sides while overlapping
        pager.setPageTransformer(true, (page, position) -> {
            float abs = Math.abs(position);
            if (abs >= 1.2f) {
                page.setAlpha(0f);
                return;
            }
            float clamped = Math.min(1f, abs);
            // Center = 1.0; sides scale down (stacked preview)
            float scale = BANNER_SIDE_MIN_SCALE
                    + (1f - clamped) * (1f - BANNER_SIDE_MIN_SCALE);
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setAlpha(BANNER_SIDE_MIN_ALPHA
                    + (1f - clamped) * (1f - BANNER_SIDE_MIN_ALPHA));
            // Focused card pops above neighbors (keeps existing card elevation look)
            page.setTranslationZ((1f - clamped) * dp(14));
            // Pull sides inward so they sit tucked under / beside center
            int pageWidth = page.getWidth();
            if (pageWidth <= 0) {
                pageWidth = pager.getWidth() - pager.getPaddingLeft() - pager.getPaddingRight();
            }
            float tuck = pageWidth * 0.14f * clamped;
            page.setTranslationX(position < 0f ? tuck : (position > 0f ? -tuck : 0f));
        });
        applyBannerHeight(pager, BANNER_NETFLIX_ASPECT);
        bannerHeightResolved = true;
    }

    private void applyBannerHeight(@NonNull ViewPager pager, float widthOverHeight) {
        if (widthOverHeight <= 0.01f) {
            widthOverHeight = BANNER_NETFLIX_ASPECT;
        }
        int pagerWidth = pager.getWidth();
        if (pagerWidth <= 0 && pager.getParent() instanceof View) {
            pagerWidth = ((View) pager.getParent()).getWidth();
        }
        if (pagerWidth <= 0) {
            pagerWidth = getResources().getDisplayMetrics().widthPixels;
        }
        int contentWidth = Math.max(1, pagerWidth - pager.getPaddingLeft() - pager.getPaddingRight());
        int height = Math.round(contentWidth / widthOverHeight);
        int minH = dp(BANNER_MIN_HEIGHT_DP);
        int maxH = Math.round(getResources().getDisplayMetrics().heightPixels * BANNER_MAX_SCREEN_FRACTION);
        height = Math.max(minH, Math.min(maxH, height)) + dp(BANNER_HEIGHT_BOOST_DP);
        int totalHeight = height + pager.getPaddingTop() + pager.getPaddingBottom();
        ViewGroup.LayoutParams lp = pager.getLayoutParams();
        if (lp != null && lp.height != totalHeight) {
            lp.height = totalHeight;
            pager.setLayoutParams(lp);
        }
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private void attachUserProfileListener() {
        if (txtUserName == null) {
            return;
        }
        if (PrepLocalSession.isLoggedIn(requireContext())) {
            loadedProfileUid = "prep-local";
            String localName = PrepLocalSession.getDisplayName(requireContext());
            if (localName == null || localName.trim().isEmpty()) {
                localName = getString(R.string.seeker);
            }
            applyProfileToUi(localName, null);
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            loadedProfileUid = null;
            showUserNameLoading(false);
            txtUserName.setText(R.string.seeker);
            return;
        }
        String uid = user.getUid();
        boolean sameUser = uid.equals(loadedProfileUid);
        UserProfileCache.CachedProfile cached = UserProfileCache.load(requireContext(), uid);
        if (cached != null) {
            applyProfileToUi(cached.name, cached.profileUrl);
        } else if (!sameUser) {
            showUserNameLoading(true);
        }
        loadedProfileUid = uid;
        fetchUserProfileFromFirebase(uid, cached == null && !sameUser);
    }

    private void fetchUserProfileFromFirebase(String uid, boolean showShimmerOnFailure) {
        userProfileRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userProfileRef.get().addOnCompleteListener(task -> {
            if (!isAdded()) {
                return;
            }
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                updateProfileFromSnapshot(task.getResult());
                return;
            }
            if (showShimmerOnFailure) {
                showUserNameLoading(false);
                txtUserName.setText(R.string.seeker);
            }
        });
    }

    private void applyProfileToUi(String displayName, @Nullable String profileUrl) {
        if (txtUserName == null) {
            return;
        }
        txtUserName.setText(displayName);
        showUserNameLoading(false);
        if (imgProfileIcon != null) {
            imgProfileIcon.setImageResource(R.drawable.ic_profile);
        }
    }

    private void showUserNameLoading(boolean loading) {
        if (shimmerUserName == null || txtUserName == null) {
            return;
        }
        if (loading) {
            txtUserName.setVisibility(View.GONE);
            shimmerUserName.setVisibility(View.VISIBLE);
            shimmerUserName.startShimmer();
            return;
        }
        shimmerUserName.stopShimmer();
        shimmerUserName.setVisibility(View.GONE);
        txtUserName.setVisibility(View.VISIBLE);
    }

    private void updateProfileFromSnapshot(DataSnapshot snapshot) {
        if (!isAdded() || FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> value = (Map<String, Object>) snapshot.getValue();
        String displayName = null;
        String profileUrl = null;
        if (value != null && value.containsKey("name") && value.get("name") != null) {
            String name = value.get("name").toString().trim();
            if (!name.isEmpty()) {
                displayName = name;
            }
        }
        if (value != null && value.containsKey("profile") && value.get("profile") != null) {
            profileUrl = value.get("profile").toString().trim();
            if (profileUrl.isEmpty()) {
                profileUrl = null;
            }
        }
        if (displayName == null) {
            displayName = getString(R.string.seeker);
        } else if (user != null) {
            UserProfileCache.save(requireContext(), user.getUid(), displayName, profileUrl);
        }
        applyProfileToUi(displayName, profileUrl);
    }

    private void loadDashboard() {
        if (!isAdded()) {
            return;
        }
        fetchDashboardFromNetwork();
    }

    private void fetchDashboardFromNetwork() {
        if (!isAdded()) {
            return;
        }
        dashboardLoaded = false;
        clearGitaAudioViewRefs();
        playOverlayLoadingVisible = false;
        updateFullGitaAudioCardVisibility();
        showHomeShimmer(true);
        recyclerSections.setVisibility(View.GONE);
        if (cardShlokaDay != null) {
            cardShlokaDay.setVisibility(View.GONE);
        }
        if (layoutRecentViewsSection != null) {
            layoutRecentViewsSection.setVisibility(View.GONE);
        }
        txtError.setVisibility(View.GONE);
        clearBannerAutoScroll();

        final android.content.Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            try {
                PrepHomeRepository.Result result = PrepHomeRepository.loadHome(appContext);
                if (result.displayName != null && !result.displayName.isEmpty()) {
                    PrepLocalSession.setDisplayName(appContext, result.displayName);
                }
                mainHandler.post(() -> {
                    onDashboardLoaded(result.dashboard, true);
                    if (result.displayName != null && !result.displayName.isEmpty() && isAdded()) {
                        applyProfileToUi(result.displayName, null);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> onDashboardError(e.getMessage()));
            }
        }).start();
    }

    private void onDashboardLoaded(GitaDashboard.Data data, boolean fromNetwork) {
        if (!isAdded())
            return;
        dashboardLoaded = true;
        showHomeShimmer(false);
        swipeRefreshLayout.setRefreshing(false);
        txtError.setVisibility(View.GONE);
        recyclerSections.setVisibility(View.VISIBLE);

        bannerList = data.banners != null ? data.banners : new ArrayList<>();
        allSections = data.sections != null ? data.sections : new ArrayList<>();
        bannerHeightResolved = false;
        // Prep local mode: hide Gita-only daily shloka / audio blocks
        bindDailyShloka(null);
        applyCategoryFilter();
        updateFullGitaAudioCardVisibility();
    }

    private void onDashboardError(String message) {
        if (!isAdded())
            return;
        dashboardLoaded = false;
        showHomeShimmer(false);
        swipeRefreshLayout.setRefreshing(false);
        // Quiet retry UI — no "could not load" toast
        txtError.setVisibility(View.GONE);
        recyclerSections.setVisibility(View.GONE);
        if (layoutRecentViewsSection != null) {
            layoutRecentViewsSection.setVisibility(View.GONE);
        }
        if (cardShlokaDay != null) {
            cardShlokaDay.setVisibility(View.GONE);
        }
        updateFullGitaAudioCardVisibility();
    }

    private void updateFullGitaAudioCardVisibility() {
        // Prep local home: Gita full-audio card stays hidden
        if (layoutFullGitaAudioSection != null) {
            layoutFullGitaAudioSection.setVisibility(View.GONE);
        }
    }

    private void clearGitaAudioViewRefs() {
        layoutFullGitaAudioSection = null;
        layoutFullGitaAudio = null;
        layoutFullAudioPlayer = null;
        fullAudioMediaBackdrop = null;
        imgFullAudioBackground = null;
        txtFullAudioChapterBadge = null;
        txtFullAudioPositionBadge = null;
        txtFullAudioNowPlaying = null;
        txtFullAudioChapterLine = null;
        txtFullAudioElapsed = null;
        txtFullAudioDuration = null;
        seekFullAudio = null;
        btnFullAudioChapterPrev = null;
        btnFullAudioPlayPause = null;
        btnFullAudioChapterNext = null;
        progressFullAudioBuffering = null;
    }

    private void bindGitaAudioPlayerViews(View audioSectionRoot) {
        if (audioSectionRoot == null) {
            return;
        }
        layoutFullGitaAudioSection = audioSectionRoot;
        layoutFullGitaAudio = audioSectionRoot.findViewById(R.id.layoutFullGitaAudio);
        layoutFullAudioPlayer = audioSectionRoot.findViewById(R.id.layoutFullAudioPlayer);
        fullAudioMediaBackdrop = audioSectionRoot.findViewById(R.id.fullAudioMediaBackdrop);
        imgFullAudioBackground = audioSectionRoot.findViewById(R.id.imgFullAudioBackground);
        txtFullAudioChapterBadge = audioSectionRoot.findViewById(R.id.txtFullAudioChapterBadge);
        txtFullAudioPositionBadge = audioSectionRoot.findViewById(R.id.txtFullAudioPositionBadge);
        txtFullAudioNowPlaying = audioSectionRoot.findViewById(R.id.txtFullAudioNowPlaying);
        txtFullAudioChapterLine = audioSectionRoot.findViewById(R.id.txtFullAudioChapterLine);
        txtFullAudioElapsed = audioSectionRoot.findViewById(R.id.txtFullAudioElapsed);
        txtFullAudioDuration = audioSectionRoot.findViewById(R.id.txtFullAudioDuration);
        seekFullAudio = audioSectionRoot.findViewById(R.id.seekFullAudio);
        btnFullAudioChapterPrev = audioSectionRoot.findViewById(R.id.btnFullAudioChapterPrev);
        btnFullAudioPlayPause = audioSectionRoot.findViewById(R.id.btnFullAudioPlayPause);
        btnFullAudioChapterNext = audioSectionRoot.findViewById(R.id.btnFullAudioChapterNext);
        progressFullAudioBuffering = audioSectionRoot.findViewById(R.id.progressFullAudioBuffering);

        boolean listenersAttached = GITA_AUDIO_LISTENERS_TAG.equals(audioSectionRoot.getTag());
        if (!listenersAttached) {
            audioSectionRoot.setTag(GITA_AUDIO_LISTENERS_TAG);
            attachGitaAudioPlayerListeners();
        }
        refreshGitaAudioUiFromService();
    }

    private void attachGitaAudioPlayerListeners() {
        if (btnFullAudioPlayPause != null) {
            btnFullAudioPlayPause.setOnClickListener(v -> startFullGitaAudioPlayback());
        }
        if (btnFullAudioChapterPrev != null) {
            btnFullAudioChapterPrev.setOnClickListener(v -> {
                if (audioService != null && audioService.copySnapshot().active) {
                    audioService.playPreviousChapter(() -> {
                        if (isAdded()) {
                            AppToast.show(requireContext(), R.string.gita_audio_no_previous_chapter);
                        }
                    });
                }
            });
        }
        if (btnFullAudioChapterNext != null) {
            btnFullAudioChapterNext.setOnClickListener(v -> {
                if (audioService != null && audioService.copySnapshot().active) {
                    audioService.playNextChapter(() -> {
                        if (isAdded()) {
                            AppToast.show(requireContext(), R.string.gita_audio_no_next_chapter);
                        }
                    });
                }
            });
        }
        if (txtFullAudioChapterBadge != null) {
            txtFullAudioChapterBadge.setOnClickListener(v -> showGitaAudioChapterPicker());
        }
        if (txtFullAudioPositionBadge != null) {
            txtFullAudioPositionBadge.setOnClickListener(v -> showGitaAudioShlokaPicker());
        }
        if (txtFullAudioNowPlaying != null) {
            txtFullAudioNowPlaying.setOnClickListener(v -> showGitaAudioShlokaPicker());
        }
        if (seekFullAudio != null) {
            seekFullAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && audioService != null) {
                        GitaFullAudioPlaybackService.PlaybackSnapshot snap = audioService.copySnapshot();
                        if (snap.durationMs > 0L) {
                            long pos = (long) ((progress / 1000f) * snap.durationMs);
                            if (txtFullAudioElapsed != null) {
                                txtFullAudioElapsed.setText(
                                        GitaFullAudioPlaybackService.formatTime(pos));
                            }
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    userSeekingAudio = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    userSeekingAudio = false;
                    if (audioService != null) {
                        GitaFullAudioPlaybackService.PlaybackSnapshot snap = audioService.copySnapshot();
                        if (snap.durationMs > 0L) {
                            long pos = (long) ((seekBar.getProgress() / 1000f) * snap.durationMs);
                            audioService.seekTo(pos);
                        }
                    }
                }
            });
        }
    }

    private void refreshGitaAudioUiFromService() {
        if (!isAdded() || layoutFullAudioPlayer == null) {
            return;
        }
        bindAudioServiceIfNeeded();
        if (audioService != null) {
            GitaFullAudioPlaybackService.PlaybackSnapshot snap = audioService.copySnapshot();
            if (snap.active || snap.preloaded) {
                applyPlaybackSnapshot(snap);
                return;
            }
        }
        showFullAudioPlayerShell();
    }

    private void requestGitaAudioPreload() {
        if (!isAdded() || !dashboardLoaded) {
            return;
        }
        Context context = requireContext().getApplicationContext();
        Intent intent = new Intent(context, GitaFullAudioPlaybackService.class);
        if (!audioServiceBound) {
            context.bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE);
        } else if (audioService != null) {
            GitaFullAudioPlaybackService.PlaybackSnapshot snap = audioService.copySnapshot();
            if (!snap.active && !snap.preloaded) {
                audioService.preloadFirstChapter();
            }
        }
    }

    private void updatePlayOverlayLoading(boolean loading) {
        if (playOverlayLoadingVisible == loading) {
            return;
        }
        playOverlayLoadingVisible = loading;
        if (btnFullAudioPlayPause != null) {
            btnFullAudioPlayPause.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
            btnFullAudioPlayPause.setEnabled(!loading);
        }
        if (progressFullAudioBuffering != null) {
            progressFullAudioBuffering.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void setFullGitaAudioLoadingUi(boolean loading) {
        updatePlayOverlayLoading(loading);
        if (btnFullAudioChapterPrev != null) {
            btnFullAudioChapterPrev.setEnabled(!loading);
        }
        if (btnFullAudioChapterNext != null) {
            btnFullAudioChapterNext.setEnabled(!loading);
        }
    }

    private boolean isPlaybackBufferingUi(
            GitaFullAudioPlaybackService.PlaybackSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.loadingChapter || snapshot.loadingNextChapter) {
            return true;
        }
        return snapshot.active && snapshot.bufferingMedia;
    }

    private void startFullGitaAudioPlayback() {
        if (!isAdded() || fullGitaAudioLoading) {
            return;
        }
        if (audioService != null && audioService.copySnapshot().active) {
            audioService.togglePlayPause();
            return;
        }
        if (needsPostNotificationPermission()) {
            pendingPlaybackAfterNotificationPermission = true;
            postNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        beginFullGitaAudioPlayback();
    }

    private boolean needsPostNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    }

    private void beginFullGitaAudioPlayback() {
        if (!isAdded() || fullGitaAudioLoading) {
            return;
        }
        fullGitaAudioLoading = true;
        pendingStartPlayback = true;
        setFullGitaAudioLoadingUi(true);
        showFullAudioPlayerUiLoading();
        GitaFullAudioProgressStore.SavedProgress saved = GitaFullAudioProgressStore.load(requireContext());
        if (saved != null) {
            int chapterNum = saved.chapterNumber > 0 ? saved.chapterNumber : 1;
            String position = chapterNum + "." + saved.shlokaInChapter;
            AppToast.show(requireContext(), getString(R.string.home_full_gita_audio_resuming,
                    getString(R.string.chapter_number_label, chapterNum), position));
        } else {
            AppToast.show(requireContext(), R.string.home_full_gita_audio_loading);
        }
        ensureAudioServiceStartedAndBound();
    }

    private void ensureAudioServiceStartedAndBound() {
        Context context = requireContext().getApplicationContext();
        Intent intent = new Intent(context, GitaFullAudioPlaybackService.class);
        ContextCompat.startForegroundService(context, intent);
        if (!audioServiceBound) {
            context.bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE);
        } else if (audioService != null && pendingStartPlayback) {
            pendingStartPlayback = false;
            fullGitaAudioLoading = false;
            setFullGitaAudioLoadingUi(false);
            audioService.startFullGitaPlayback();
        }
    }

    private void bindAudioServiceIfNeeded() {
        if (!isAdded()) {
            return;
        }
        if (audioServiceBound) {
            if (audioService != null) {
                audioService.setUiListener(playbackUiListener);
            }
            return;
        }
        Context context = requireContext().getApplicationContext();
        Intent intent = new Intent(context, GitaFullAudioPlaybackService.class);
        context.bindService(intent, audioServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindAudioService() {
        if (!audioServiceBound || !isAdded()) {
            return;
        }
        try {
            requireContext().getApplicationContext()
                    .unbindService(audioServiceConnection);
        } catch (Exception ignored) {
        }
        audioServiceBound = false;
        audioService = null;
    }

    private void showGitaAudioChapterPicker() {
        if (!isAdded() || audioService == null) {
            return;
        }
        GitaFullAudioPlaybackService.PlaybackSnapshot snap = audioService.copySnapshot();
        if (!snap.active && !snap.preloaded) {
            return;
        }
        GitaFullAudioQueue.loadAllChapters(new GitaFullAudioQueue.ChapterListCallback() {
            @Override
            public void onLoaded(List<GitaChapters.Chapter> chapters) {
                if (!isAdded() || audioService == null) {
                    return;
                }
                AppCompatActivity activity = (AppCompatActivity) requireActivity();
                int selectedId = audioService.copySnapshot().chapterId;
                GitaAudioPickerSheets.showChapterPicker(activity, chapters, selectedId, null, chapter -> {
                    if (audioService != null) {
                        audioService.playChapterFromStart(chapter.chapterId, () -> {
                            if (isAdded()) {
                                AppToast.show(requireContext(), R.string.home_full_gita_audio_unavailable);
                            }
                        });
                    }
                });
            }

            @Override
            public void onError() {
                if (isAdded()) {
                    AppToast.show(requireContext(), R.string.gita_audio_chapters_load_error);
                }
            }
        });
    }

    private void showGitaAudioShlokaPicker() {
        if (!isAdded() || audioService == null) {
            return;
        }
        GitaFullAudioPlaybackService.PlaybackSnapshot snap = audioService.copySnapshot();
        if (!snap.active && !snap.preloaded) {
            return;
        }
        List<GitaChapterShlokas.Shloka> shlokas = audioService.getCurrentChapterShlokas();
        if (shlokas.isEmpty()) {
            AppToast.show(requireContext(), R.string.home_full_gita_audio_unavailable);
            return;
        }
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        GitaAudioPickerSheets.showShlokaPicker(
                activity,
                shlokas,
                snap.chapterLabel,
                Math.max(0, snap.shlokaInChapter - 1),
                index -> {
                    if (audioService != null) {
                        audioService.playShlokaInChapter(index + 1);
                    }
                });
    }

    private void showFullAudioPlayerUiLoading() {
        showFullAudioPlayerShell();
        updatePlayOverlayLoading(true);
        if (txtFullAudioChapterBadge != null) {
            txtFullAudioChapterBadge.setText(R.string.home_full_gita_audio_loading);
        }
        if (txtFullAudioPositionBadge != null) {
            txtFullAudioPositionBadge.setText("");
        }
    }

    private void showFullAudioPlayerShell() {
        if (!isAdded()) {
            return;
        }
        fullGitaAudioLoading = false;
        setFullGitaAudioLoadingUi(false);
        if (layoutFullAudioPlayer != null) {
            layoutFullAudioPlayer.setVisibility(View.VISIBLE);
        }
        if (fullAudioMediaBackdrop != null) {
            fullAudioMediaBackdrop.setVisibility(View.VISIBLE);
        }
        if (imgFullAudioBackground != null) {
            imgFullAudioBackground.setImageResource(R.color.view_bg_color);
        }
        if (txtFullAudioNowPlaying != null) {
            txtFullAudioNowPlaying.setText(R.string.play);
        }
        if (txtFullAudioChapterLine != null) {
            txtFullAudioChapterLine.setText(R.string.full_gita_audio_chapter_title);
        }
        if (txtFullAudioChapterBadge != null && !fullGitaAudioLoading) {
            GitaFullAudioProgressStore.SavedProgress saved = GitaFullAudioProgressStore.load(requireContext());
            if (saved != null && saved.chapterNumber > 0) {
                txtFullAudioChapterBadge.setText(
                        getString(R.string.chapter_number_label, saved.chapterNumber));
                if (txtFullAudioPositionBadge != null) {
                    txtFullAudioPositionBadge.setText(
                            saved.chapterNumber + "." + saved.shlokaInChapter);
                }
            } else {
                txtFullAudioChapterBadge.setText(
                        getString(R.string.chapter_number_label, 1));
                if (txtFullAudioPositionBadge != null) {
                    txtFullAudioPositionBadge.setText("1.1");
                }
            }
        }
        if (btnFullAudioPlayPause != null) {
            btnFullAudioPlayPause.setImageResource(R.drawable.ic_play_circle);
        }
        if (!fullGitaAudioLoading) {
            updatePlayOverlayLoading(false);
        }
    }

    private void applyPlaybackSnapshot(GitaFullAudioPlaybackService.PlaybackSnapshot snapshot) {
        if (snapshot == null || (!snapshot.active && !snapshot.preloaded)) {
            showFullAudioPlayerShell();
            return;
        }
        fullGitaAudioLoading = false;
        if (layoutFullAudioPlayer != null) {
            layoutFullAudioPlayer.setVisibility(View.VISIBLE);
        }
        if (fullAudioMediaBackdrop != null) {
            fullAudioMediaBackdrop.setVisibility(View.VISIBLE);
        }
        if (imgFullAudioBackground != null) {
            if (snapshot.imageUrl != null && !snapshot.imageUrl.trim().isEmpty()) {
                Glide.with(this)
                        .load(snapshot.imageUrl)
                        .centerCrop()
                        .placeholder(R.color.view_bg_color)
                        .error(R.color.view_bg_color)
                        .into(imgFullAudioBackground);
            } else {
                imgFullAudioBackground.setImageResource(R.color.view_bg_color);
            }
        }
        if (txtFullAudioChapterBadge != null) {
            txtFullAudioChapterBadge.setText(snapshot.chapterLabel);
        }
        if (txtFullAudioPositionBadge != null) {
            txtFullAudioPositionBadge.setText(snapshot.positionLabel);
        }
        if (txtFullAudioNowPlaying != null) {
            txtFullAudioNowPlaying.setText(snapshot.title);
        }
        if (txtFullAudioChapterLine != null) {
            String line = snapshot.chapterLabel;
            if (snapshot.loadingNextChapter) {
                line = getString(R.string.full_gita_audio_loading_next_chapter);
            }
            txtFullAudioChapterLine.setText(line);
        }
        if (txtFullAudioElapsed != null) {
            txtFullAudioElapsed.setText(GitaFullAudioPlaybackService.formatTime(snapshot.positionMs));
        }
        if (txtFullAudioDuration != null) {
            txtFullAudioDuration.setText(GitaFullAudioPlaybackService.formatTime(snapshot.durationMs));
        }
        if (seekFullAudio != null && !userSeekingAudio && snapshot.durationMs > 0L) {
            int progress = (int) ((snapshot.positionMs * 1000L) / snapshot.durationMs);
            seekFullAudio.setProgress(Math.min(1000, Math.max(0, progress)));
        }
        if (btnFullAudioPlayPause != null && btnFullAudioPlayPause.getVisibility() == View.VISIBLE) {
            boolean showPause = snapshot.active && snapshot.playing;
            btnFullAudioPlayPause.setImageResource(
                    showPause ? android.R.drawable.ic_media_pause : R.drawable.ic_play_circle);
            btnFullAudioPlayPause.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.white));
        }
        updatePlayOverlayLoading(isPlaybackBufferingUi(snapshot) || fullGitaAudioLoading);
        if (btnFullAudioChapterPrev != null) {
            boolean loading = isPlaybackBufferingUi(snapshot) || fullGitaAudioLoading;
            btnFullAudioChapterPrev.setEnabled(!loading);
        }
        if (btnFullAudioChapterNext != null) {
            boolean loading = isPlaybackBufferingUi(snapshot) || fullGitaAudioLoading;
            btnFullAudioChapterNext.setEnabled(!loading);
        }
    }

    /**
     * Home order:
     * 1) carousel/banner
     * 2) any one category row (category_card)
     * 3) All Subjects grid
     * 4) remaining sections
     * 5) trending band last
     */
    private List<HomeRow> buildFeedRows(List<GitaDashboard.Section> sections) {
        List<HomeRow> rows = new ArrayList<>();
        if (!bannerList.isEmpty()) {
            HomeRow bannerRow = new HomeRow();
            bannerRow.type = ROW_BANNER;
            rows.add(bannerRow);
        }
        if (sections == null) {
            return rows;
        }
        HomeRow subjectsGridRow = null;
        HomeRow firstCategoryRow = null;
        List<HomeRow> otherRows = new ArrayList<>();
        List<HomeRow> trendingRows = new ArrayList<>();
        for (GitaDashboard.Section section : sections) {
            HomeRow sectionRow = new HomeRow();
            sectionRow.type = ROW_SECTION;
            sectionRow.section = section;
            if (section != null && "trending".equalsIgnoreCase(section.layout)) {
                trendingRows.add(sectionRow);
            } else if (section != null && "grid".equalsIgnoreCase(section.layout)) {
                subjectsGridRow = sectionRow;
            } else if (firstCategoryRow == null) {
                firstCategoryRow = sectionRow;
            } else {
                otherRows.add(sectionRow);
            }
        }
        if (firstCategoryRow != null) {
            rows.add(firstCategoryRow);
        }
        if (subjectsGridRow != null) {
            rows.add(subjectsGridRow);
        }
        rows.addAll(otherRows);
        rows.addAll(trendingRows);
        return rows;
    }

    private List<GitaDashboard.ShlokaItem> buildSubjectGridItems(
            @NonNull GitaDashboard.Section section) {
        List<GitaDashboard.ShlokaItem> source =
                section.shlokas != null ? section.shlokas : new ArrayList<>();
        List<GitaDashboard.ShlokaItem> out = new ArrayList<>();
        int limit = Math.min(SUBJECT_GRID_VISIBLE, source.size());
        for (int i = 0; i < limit; i++) {
            out.add(source.get(i));
        }
        // Prepshots-style 8th "More" tile when subjects are present
        if (!out.isEmpty()) {
            GitaDashboard.ShlokaItem more = new GitaDashboard.ShlokaItem();
            more.title = getString(R.string.prep_more);
            more.contentType = "more";
            more.image = "";
            more.badge = "";
            out.add(more);
        }
        return out;
    }

    private boolean isThinkLikeKrishnaSection(GitaDashboard.Section section) {
        if (section.sectionTitle != null
                && section.sectionTitle.toLowerCase(Locale.US).contains("think like krishna")) {
            return true;
        }
        return section.sectionType != null
                && section.sectionType.toUpperCase(Locale.US).contains("THINK_LIKE");
    }

    private void bindDailyShloka(GitaDashboard.DailyShloka shloka) {
        if (cardShlokaDay == null) {
            return;
        }
        if (shloka == null || shloka.title == null || shloka.title.isEmpty()) {
            cardShlokaDay.setVisibility(View.GONE);
            return;
        }
        cardShlokaDay.setVisibility(View.VISIBLE);
        txtShlokaDayTitle.setText(shloka.title);
        if (!shloka.image.isEmpty()) {
            Glide.with(requireContext()).load(shloka.image).centerCrop().into(imgShlokaDay);
        }
        txtShlokaDayDesc.setText(formatDescription(shloka.description));
        cardShlokaDay.setOnClickListener(v -> openShlokaDetail(shloka.title));
    }

    private void openShlokaDetail(String title) {
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        ActivityNav.start(requireContext(), MovieviewActivity.newHomeShlokaIntent(requireContext(), title));
    }

    private void openSubjectSyllabus(@NonNull GitaDashboard.ShlokaItem item) {
        if (!isAdded()) {
            return;
        }
        long subjectId = item.contentId > 0 ? item.contentId : 0L;
        if (subjectId <= 0) {
            AppToast.show(requireContext(), R.string.prep_syllabus_load_error);
            return;
        }
        String examName = selectedCourse != null ? selectedCourse.examName : "";
        String examThumb = selectedCourse != null ? selectedCourse.thumbnailUrl : "";
        SubjectSyllabusActivity.open(
                requireContext(),
                subjectId,
                item.title,
                examName,
                examThumb,
                item.image);
    }

    private void openPrepItem(@NonNull GitaDashboard.ShlokaItem item) {
        if (!isAdded()) {
            return;
        }
        // Subject tiles stay on syllabus; series/topic tiles open chapter details.
        if ("category".equalsIgnoreCase(item.contentType)) {
            openSubjectSyllabus(item);
            return;
        }
        long seriesId = resolveSeriesId(item);
        if (seriesId > 0) {
            openSeriesDetails(item, seriesId);
            return;
        }
        AppToast.show(requireContext(), getString(R.string.prep_local_opening_video));
        new Thread(() -> {
            try {
                PrepHomeMapper.PlayableVideo video = PrepHomeRepository.loadPlayableOrDirect(item);
                mainHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (video == null || video.url == null || video.url.isEmpty()) {
                        AppToast.show(requireContext(), getString(R.string.prep_local_video_error));
                        return;
                    }
                    startPrepPlayer(video);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (isAdded()) {
                        AppToast.show(requireContext(), getString(R.string.prep_local_video_error));
                    }
                });
            }
        }).start();
    }

    private long resolveSeriesId(@NonNull GitaDashboard.ShlokaItem item) {
        if (item.seriesId > 0) {
            return item.seriesId;
        }
        if ("series".equalsIgnoreCase(item.contentType) && item.contentId > 0) {
            return item.contentId;
        }
        return 0L;
    }

    private void openSeriesDetails(@NonNull GitaDashboard.ShlokaItem item, long seriesId) {
        if (!isAdded() || seriesId <= 0) {
            return;
        }
        String examName = selectedCourse != null ? selectedCourse.examName : "";
        String examThumb = selectedCourse != null ? selectedCourse.thumbnailUrl : "";
        String subjectName = firstNonEmptyHome(
                item.chapterName,
                item.description,
                "");
        SeriesDetailsActivity.open(
                requireContext(),
                seriesId,
                item.title,
                subjectName,
                examName,
                examThumb,
                item.image);
    }

    @NonNull
    private static String firstNonEmptyHome(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()
                        && !"video".equalsIgnoreCase(trimmed)
                        && !"series".equalsIgnoreCase(trimmed)
                        && !"audio".equalsIgnoreCase(trimmed)
                        && !"category".equalsIgnoreCase(trimmed)) {
                    return trimmed;
                }
            }
        }
        return "";
    }

    private void openPrepBanner(@NonNull GitaDashboard.Banner banner) {
        GitaDashboard.ShlokaItem item = new GitaDashboard.ShlokaItem();
        item.seriesId = banner.seriesId;
        item.videoId = banner.videoId;
        item.title = banner.title != null && !banner.title.isEmpty() ? banner.title : "Banner";
        item.image = banner.url != null ? banner.url : "";
        item.playUrl = banner.playUrl != null ? banner.playUrl : "";
        item.contentType = "series";
        if (item.seriesId <= 0 && banner.imageKey != null && banner.imageKey.startsWith("prep:")) {
            String[] parts = banner.imageKey.split(":");
            if (parts.length >= 2) {
                try {
                    item.seriesId = Long.parseLong(parts[1]);
                    if (parts.length >= 3) {
                        item.videoId = Long.parseLong(parts[2]);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (item.seriesId > 0) {
            openSeriesDetails(item, item.seriesId);
            return;
        }
        if (item.playUrl == null || item.playUrl.isEmpty()) {
            AppToast.show(requireContext(), R.string.prep_local_video_error);
            return;
        }
        openPrepItem(item);
    }

    private void startPrepPlayer(@NonNull PrepHomeMapper.PlayableVideo video) {
        Intent intent = new Intent(requireContext(), ExoPlayerActivity.class);
        intent.putExtra("link", video.url);
        intent.putExtra("title", video.title != null ? video.title : "");
        intent.putExtra("img", video.image != null ? video.image : "");
        intent.putExtra("videoid", video.videoId != null ? video.videoId : "");
        intent.putExtra("type", "prep");
        intent.putExtra("description", "");
        ActivityNav.start(requireContext(), intent);
    }

    private void showHomeShimmer(boolean show) {
        if (!(shimmerHome instanceof ShimmerFrameLayout)) {
            return;
        }
        ShimmerFrameLayout shimmerLayout = (ShimmerFrameLayout) shimmerHome;
        if (show) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        } else {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
    }

    private void refreshRecentViewsSection() {
        // Offline recent/present views row removed from Prep home.
        if (layoutRecentViewsSection != null) {
            layoutRecentViewsSection.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        attachUserProfileListener();
        PrepExamRepository.SelectedCourse local = PrepCourseStore.getActive(requireContext());
        long prevId = selectedCourse != null ? selectedCourse.examId : 0L;
        long nextId = local != null ? local.examId : 0L;
        if (local != null) {
            applySelectedCourseToTopBar(local);
        }
        if (nextId > 0 && nextId != prevId) {
            loadDashboard();
        } else if (dashboardLoaded) {
            refreshRecentViewsSection();
            bindAudioServiceIfNeeded();
            refreshGitaAudioUiFromService();
        }
    }

    private static String formatDescription(String raw) {
        if (raw == null)
            return "";
        String text = raw.trim();
        int colon = text.indexOf(':');
        if (colon > 0 && colon < 24) {
            text = text.substring(colon + 1).trim();
        }
        return text;
    }

    private int bannerRealCount() {
        return bannerList != null ? bannerList.size() : 0;
    }

    private int toBannerRealIndex(int virtualPosition) {
        int count = bannerRealCount();
        if (count <= 0) {
            return 0;
        }
        int mod = virtualPosition % count;
        return mod < 0 ? mod + count : mod;
    }

    private int bannerLoopStartPosition(int realIndex) {
        int count = bannerRealCount();
        if (count <= 1) {
            return 0;
        }
        int safeReal = Math.min(Math.max(0, realIndex), count - 1);
        // Middle of virtual range so user can swipe both ways forever
        return (BANNER_LOOP_MULTIPLIER / 2) * count + safeReal;
    }

    private void startBannerAutoScroll() {
        stopBannerAutoScroll();
        if (activeBannerPager == null || bannerRealCount() <= 1) {
            return;
        }
        bannerIndex = activeBannerPager.getCurrentItem();
        bannerTimer = new Timer();
        bannerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isAdded() || activeBannerPager == null) {
                    return;
                }
                mainHandler.post(() -> {
                    if (!isAdded() || activeBannerPager == null || bannerRealCount() <= 1) {
                        return;
                    }
                    int next = activeBannerPager.getCurrentItem() + 1;
                    bannerIndex = next;
                    activeBannerPager.setCurrentItem(next, true);
                });
            }
        }, BANNER_AUTO_SCROLL_MS, BANNER_AUTO_SCROLL_MS);
    }

    private void stopBannerAutoScroll() {
        if (bannerTimer != null) {
            bannerTimer.cancel();
            bannerTimer = null;
        }
    }

    private void clearBannerAutoScroll() {
        stopBannerAutoScroll();
        activeBannerPager = null;
    }

    @Override
    public void onDestroyView() {
        clearBannerAutoScroll();
        clearGitaAudioViewRefs();
        playOverlayLoadingVisible = false;
        loadedProfileUid = null;
        userProfileRef = null;
        super.onDestroyView();
    }

    private static class HomeRow {
        int type;
        GitaDashboard.Section section;
    }

    // --- Feed adapter: sections + banner after Think Like Krishna ---

    private class HomeFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<HomeRow> rows;

        HomeFeedAdapter(List<HomeRow> rows) {
            this.rows = rows != null ? rows : new ArrayList<>();
        }

        @Override
        public int getItemViewType(int position) {
            HomeRow row = rows.get(position);
            if (row.type == ROW_BANNER || row.type == ROW_GITA_AUDIO) {
                return row.type;
            }
            if (row.section != null) {
                if ("grid".equalsIgnoreCase(row.section.layout)) {
                    return ROW_SUBJECT_GRID;
                }
                if ("trending".equalsIgnoreCase(row.section.layout)) {
                    return ROW_TRENDING;
                }
            }
            return ROW_SECTION;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == ROW_BANNER) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_gita_banner_carousel, parent, false);
                return new BannerRowHolder(v);
            }
            if (viewType == ROW_GITA_AUDIO) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_home_gita_audio_feed, parent, false);
                return new GitaAudioRowHolder(v);
            }
            if (viewType == ROW_SUBJECT_GRID) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_prep_subjects_grid, parent, false);
                return new SubjectGridHolder(v);
            }
            if (viewType == ROW_TRENDING) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_prep_trending_band, parent, false);
                return new TrendingBandHolder(v);
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gita_section, parent, false);
            return new SectionHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof BannerRowHolder) {
                ((BannerRowHolder) holder).bind();
            } else if (holder instanceof GitaAudioRowHolder) {
                bindGitaAudioPlayerViews(holder.itemView);
                holder.itemView.setVisibility(dashboardLoaded ? View.VISIBLE : View.GONE);
            } else if (holder instanceof SubjectGridHolder) {
                ((SubjectGridHolder) holder).bind(rows.get(position).section);
            } else if (holder instanceof TrendingBandHolder) {
                ((TrendingBandHolder) holder).bind(rows.get(position).section);
            } else if (holder instanceof SectionHolder) {
                GitaDashboard.Section section = rows.get(position).section;
                SectionHolder sh = (SectionHolder) holder;
                sh.txtSectionTitle.setText(section.sectionTitle);
                sh.recyclerShlokas.setLayoutManager(
                        new LinearLayoutManager(sh.itemView.getContext(),
                                LinearLayoutManager.HORIZONTAL, false));
                sh.recyclerShlokas.setAdapter(new ShlokaAdapter(section.shlokas));
            }
        }

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            if (holder instanceof BannerRowHolder) {
                if (activeBannerPager == ((BannerRowHolder) holder).viewpagerBanners) {
                    clearBannerAutoScroll();
                }
            }
            super.onViewRecycled(holder);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        class SectionHolder extends RecyclerView.ViewHolder {
            final TextView txtSectionTitle;
            final RecyclerView recyclerShlokas;

            SectionHolder(View itemView) {
                super(itemView);
                txtSectionTitle = itemView.findViewById(R.id.txtSectionTitle);
                recyclerShlokas = itemView.findViewById(R.id.recyclerShlokas);
            }
        }

        class SubjectGridHolder extends RecyclerView.ViewHolder {
            final TextView txtSubjectsTitle;
            final RecyclerView recyclerSubjectsGrid;

            SubjectGridHolder(View itemView) {
                super(itemView);
                txtSubjectsTitle = itemView.findViewById(R.id.txtSubjectsTitle);
                recyclerSubjectsGrid = itemView.findViewById(R.id.recyclerSubjectsGrid);
            }

            void bind(GitaDashboard.Section section) {
                if (section == null) {
                    itemView.setVisibility(View.GONE);
                    return;
                }
                itemView.setVisibility(View.VISIBLE);
                String examName = selectedCourse != null ? selectedCourse.examName : "";
                if (examName != null && !examName.trim().isEmpty()) {
                    txtSubjectsTitle.setText(getString(R.string.prep_prepare_for, examName.trim()));
                } else if (section.sectionTitle != null && !section.sectionTitle.isEmpty()) {
                    txtSubjectsTitle.setText(section.sectionTitle);
                } else {
                    txtSubjectsTitle.setText(R.string.prep_all_subjects);
                }
                List<GitaDashboard.ShlokaItem> tiles = buildSubjectGridItems(section);
                recyclerSubjectsGrid.setLayoutManager(
                        new GridLayoutManager(itemView.getContext(), 4));
                recyclerSubjectsGrid.setAdapter(new SubjectGridAdapter(tiles));
            }
        }

        class TrendingBandHolder extends RecyclerView.ViewHolder {
            final ImageView imgTrendingBg;
            final TextView txtTrendingTag;
            final TextView txtTrendingTitle;
            final TextView txtTrendingSubtitle;
            final RecyclerView recyclerTrending;

            TrendingBandHolder(View itemView) {
                super(itemView);
                imgTrendingBg = itemView.findViewById(R.id.imgTrendingBg);
                txtTrendingTag = itemView.findViewById(R.id.txtTrendingTag);
                txtTrendingTitle = itemView.findViewById(R.id.txtTrendingTitle);
                txtTrendingSubtitle = itemView.findViewById(R.id.txtTrendingSubtitle);
                recyclerTrending = itemView.findViewById(R.id.recyclerTrending);
            }

            void bind(GitaDashboard.Section section) {
                if (section == null) {
                    itemView.setVisibility(View.GONE);
                    return;
                }
                itemView.setVisibility(View.VISIBLE);
                if (section.bgImage != null && !section.bgImage.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(section.bgImage)
                            .centerCrop()
                            .into(imgTrendingBg);
                } else {
                    imgTrendingBg.setImageDrawable(null);
                }
                if (section.tag != null && !section.tag.isEmpty()) {
                    txtTrendingTag.setVisibility(View.VISIBLE);
                    txtTrendingTag.setText(section.tag);
                } else {
                    txtTrendingTag.setVisibility(View.GONE);
                }
                txtTrendingTitle.setText(
                        section.sectionTitle != null ? section.sectionTitle : "");
                if (section.subTitle != null && !section.subTitle.isEmpty()) {
                    txtTrendingSubtitle.setVisibility(View.VISIBLE);
                    txtTrendingSubtitle.setText(section.subTitle);
                } else {
                    txtTrendingSubtitle.setVisibility(View.GONE);
                }
                recyclerTrending.setLayoutManager(
                        new LinearLayoutManager(itemView.getContext(),
                                LinearLayoutManager.HORIZONTAL, false));
                recyclerTrending.setAdapter(new TrendingCardAdapter(
                        section.shlokas != null ? section.shlokas : new ArrayList<>()));
            }
        }

        class GitaAudioRowHolder extends RecyclerView.ViewHolder {
            GitaAudioRowHolder(View itemView) {
                super(itemView);
            }
        }

        class BannerRowHolder extends RecyclerView.ViewHolder {
            final ViewPager viewpagerBanners;
            final LinearLayout layoutBannerDots;

            BannerRowHolder(View itemView) {
                super(itemView);
                viewpagerBanners = itemView.findViewById(R.id.viewpagerBanners);
                layoutBannerDots = itemView.findViewById(R.id.layoutBannerDots);
                configureBannerPager(viewpagerBanners);
                viewpagerBanners.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        bannerIndex = position;
                        updateBannerDots(layoutBannerDots, bannerRealCount(),
                                toBannerRealIndex(position));
                    }
                });
            }

            void bind() {
                if (bannerList.isEmpty()) {
                    itemView.setVisibility(View.GONE);
                    return;
                }
                itemView.setVisibility(View.VISIBLE);
                configureBannerPager(viewpagerBanners);
                viewpagerBanners.setAdapter(new BannerPagerAdapter(bannerList));
                int realStart = toBannerRealIndex(bannerIndex);
                int start = bannerLoopStartPosition(realStart);
                viewpagerBanners.setCurrentItem(start, false);
                bannerIndex = start;
                setupBannerDots(layoutBannerDots, bannerRealCount(), realStart);
                activeBannerPager = viewpagerBanners;
                viewpagerBanners.post(() -> applyBannerHeight(viewpagerBanners, BANNER_NETFLIX_ASPECT));
                startBannerAutoScroll();
            }
        }
    }

    private void setupBannerDots(@Nullable LinearLayout dotsLayout, int count, int selected) {
        if (dotsLayout == null) {
            return;
        }
        dotsLayout.removeAllViews();
        if (count <= 1) {
            dotsLayout.setVisibility(View.GONE);
            return;
        }
        dotsLayout.setVisibility(View.VISIBLE);
        int size = dp(BANNER_DOT_SIZE_DP);
        int gap = dp(BANNER_DOT_GAP_DP);
        for (int i = 0; i < count; i++) {
            View dot = new View(dotsLayout.getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            if (i > 0) {
                lp.setMarginStart(gap);
            }
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.bg_banner_dot);
            dotsLayout.addView(dot);
        }
        updateBannerDots(dotsLayout, count, selected);
    }

    private void updateBannerDots(@Nullable LinearLayout dotsLayout, int count, int selected) {
        if (dotsLayout == null || count <= 0) {
            return;
        }
        int childCount = dotsLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View dot = dotsLayout.getChildAt(i);
            if (dot != null) {
                dot.setSelected(i == selected);
            }
        }
    }

    private class ShlokaEntryHomeAdapter extends RecyclerView.Adapter<ShlokaEntryHomeAdapter.EntryHolder> {

        private final List<LocalRecentViews.Entry> entries;

        ShlokaEntryHomeAdapter(List<LocalRecentViews.Entry> entries) {
            this.entries = entries != null ? entries : new ArrayList<>();
        }

        @NonNull
        @Override
        public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gita_shloka, parent, false);
            return new EntryHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull EntryHolder holder, int position) {
            LocalRecentViews.Entry entry = entries.get(position);
            String titleText = entry.title != null ? entry.title : "";
            holder.txtTitle.setText(titleText);
            holder.txtDescription.setText(formatDescription(entry.description));
            if (entry.posterUrl != null && !entry.posterUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(entry.posterUrl)
                        .centerCrop()
                        .into(holder.imgShloka);
            }
            holder.itemView.setOnClickListener(v -> openShlokaDetail(titleText));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        class EntryHolder extends RecyclerView.ViewHolder {
            final ImageView imgShloka;
            final TextView txtTitle;
            final TextView txtDescription;

            EntryHolder(View itemView) {
                super(itemView);
                imgShloka = itemView.findViewById(R.id.imgShloka);
                txtTitle = itemView.findViewById(R.id.txtTitle);
                txtDescription = itemView.findViewById(R.id.txtDescription);
            }
        }
    }

    private class SubjectGridAdapter
            extends RecyclerView.Adapter<SubjectGridAdapter.SubjectTileHolder> {

        private final List<GitaDashboard.ShlokaItem> items;

        SubjectGridAdapter(List<GitaDashboard.ShlokaItem> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public SubjectTileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_prep_subject_tile, parent, false);
            return new SubjectTileHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SubjectTileHolder holder, int position) {
            GitaDashboard.ShlokaItem item = items.get(position);
            boolean isMore = "more".equalsIgnoreCase(item.contentType);
            holder.txtSubjectName.setText(item.title != null ? item.title : "");
            if (isMore) {
                holder.layoutSubjectTile.setCardBackgroundColor(0xB322222A);
                holder.imgSubject.setVisibility(View.GONE);
                holder.imgMoreIcon.setVisibility(View.VISIBLE);
                holder.txtSubjectBadge.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v -> AllSubjectsActivity.open(requireContext()));
                return;
            }
            holder.layoutSubjectTile.setCardBackgroundColor(0xFF1C1C24);
            holder.imgSubject.setVisibility(View.VISIBLE);
            holder.imgMoreIcon.setVisibility(View.GONE);
            if (item.badge != null && !item.badge.isEmpty()) {
                holder.txtSubjectBadge.setVisibility(View.VISIBLE);
                holder.txtSubjectBadge.setText(item.badge);
            } else {
                holder.txtSubjectBadge.setVisibility(View.GONE);
            }
            if (item.image != null && !item.image.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.image)
                        .centerCrop()
                        .into(holder.imgSubject);
            } else {
                holder.imgSubject.setImageResource(R.drawable.default_image);
            }
            holder.itemView.setOnClickListener(v -> openSubjectSyllabus(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class SubjectTileHolder extends RecyclerView.ViewHolder {
            final CardView layoutSubjectTile;
            final ImageView imgSubject;
            final ImageView imgMoreIcon;
            final TextView txtSubjectBadge;
            final TextView txtSubjectName;

            SubjectTileHolder(View itemView) {
                super(itemView);
                layoutSubjectTile = itemView.findViewById(R.id.layoutSubjectTile);
                imgSubject = itemView.findViewById(R.id.imgSubject);
                imgMoreIcon = itemView.findViewById(R.id.imgMoreIcon);
                txtSubjectBadge = itemView.findViewById(R.id.txtSubjectBadge);
                txtSubjectName = itemView.findViewById(R.id.txtSubjectName);
            }
        }
    }

    private class TrendingCardAdapter
            extends RecyclerView.Adapter<TrendingCardAdapter.TrendingCardHolder> {

        private final List<GitaDashboard.ShlokaItem> items;

        TrendingCardAdapter(List<GitaDashboard.ShlokaItem> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public TrendingCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_prep_trending_card, parent, false);
            return new TrendingCardHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TrendingCardHolder holder, int position) {
            GitaDashboard.ShlokaItem item = items.get(position);
            holder.txtTrendingCardTitle.setText(item.title != null ? item.title : "");
            if (item.description != null && !item.description.isEmpty()) {
                holder.txtTrendingCategory.setVisibility(View.VISIBLE);
                holder.txtTrendingCategory.setText(item.description);
            } else {
                holder.txtTrendingCategory.setVisibility(View.GONE);
            }
            if (item.badge != null && !item.badge.isEmpty()) {
                holder.txtTrendingBadge.setVisibility(View.VISIBLE);
                holder.txtTrendingBadge.setText(item.badge);
            } else {
                holder.txtTrendingBadge.setVisibility(View.GONE);
            }
            if (item.image != null && !item.image.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.image)
                        .centerCrop()
                        .into(holder.imgTrendingCard);
            } else {
                holder.imgTrendingCard.setImageResource(R.drawable.default_image);
            }
            holder.itemView.setOnClickListener(v -> openPrepItem(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class TrendingCardHolder extends RecyclerView.ViewHolder {
            final ImageView imgTrendingCard;
            final TextView txtTrendingBadge;
            final TextView txtTrendingCategory;
            final TextView txtTrendingCardTitle;

            TrendingCardHolder(View itemView) {
                super(itemView);
                imgTrendingCard = itemView.findViewById(R.id.imgTrendingCard);
                txtTrendingBadge = itemView.findViewById(R.id.txtTrendingBadge);
                txtTrendingCategory = itemView.findViewById(R.id.txtTrendingCategory);
                txtTrendingCardTitle = itemView.findViewById(R.id.txtTrendingCardTitle);
            }
        }
    }

    private class ShlokaAdapter extends RecyclerView.Adapter<ShlokaAdapter.ShlokaHolder> {

        private final List<GitaDashboard.ShlokaItem> items;

        ShlokaAdapter(List<GitaDashboard.ShlokaItem> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @NonNull
        @Override
        public ShlokaHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gita_shloka, parent, false);
            return new ShlokaHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ShlokaHolder holder, int position) {
            GitaDashboard.ShlokaItem item = items.get(position);
            holder.txtTitle.setText(item.title);
            holder.txtDescription.setText(formatDescription(item.description));
            if (!item.image.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.image)
                        .centerCrop()
                        .into(holder.imgShloka);
            }
            holder.itemView.setOnClickListener(v -> openPrepItem(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ShlokaHolder extends RecyclerView.ViewHolder {
            final ImageView imgShloka;
            final TextView txtTitle;
            final TextView txtDescription;

            ShlokaHolder(View itemView) {
                super(itemView);
                imgShloka = itemView.findViewById(R.id.imgShloka);
                txtTitle = itemView.findViewById(R.id.txtTitle);
                txtDescription = itemView.findViewById(R.id.txtDescription);
            }
        }
    }

    private class BannerPagerAdapter extends PagerAdapter {

        private final List<GitaDashboard.Banner> banners;

        BannerPagerAdapter(List<GitaDashboard.Banner> banners) {
            this.banners = banners;
        }

        private int realIndex(int position) {
            int count = banners.size();
            if (count <= 0) {
                return 0;
            }
            int mod = position % count;
            return mod < 0 ? mod + count : mod;
        }

        @Override
        public int getCount() {
            int count = banners.size();
            if (count <= 1) {
                return count;
            }
            // Large virtual range → infinite left/right loop with side peeks
            return count * BANNER_LOOP_MULTIPLIER;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext())
                    .inflate(R.layout.item_movie_banner, container, false);
            ImageView img = view.findViewById(R.id.imageview1);
            TextView titleView = view.findViewById(R.id.txtBannerTitle);
            View playBtn = view.findViewById(R.id.playbtn);
            View myListBtn = view.findViewById(R.id.mylistbnt);

            final GitaDashboard.Banner banner = banners.get(realIndex(position));
            if (titleView != null) {
                String title = banner.title != null ? banner.title : "";
                titleView.setText(title);
                titleView.setVisibility(title.isEmpty() ? View.GONE : View.VISIBLE);
            }
            Glide.with(container.getContext())
                    .load(banner.url)
                    .centerCrop()
                    .into(img);

            View.OnClickListener playClick = v -> openPrepBanner(banner);
            view.setOnClickListener(playClick);
            if (playBtn != null) {
                playBtn.setOnClickListener(playClick);
            }
            if (myListBtn != null) {
                myListBtn.setOnClickListener(v -> {
                    LocalWatchlist.Entry entry = new LocalWatchlist.Entry();
                    entry.title = banner.title != null && !banner.title.isEmpty()
                            ? banner.title
                            : "Banner";
                    entry.posterUrl = banner.url != null ? banner.url : "";
                    entry.description = "";
                    entry.videoUrl = banner.playUrl != null ? banner.playUrl : "";
                    entry.videoid = String.valueOf(
                            banner.videoId > 0 ? banner.videoId : banner.seriesId);
                    entry.mediaType = "video";
                    boolean added = LocalWatchlist.toggle(container.getContext(), entry);
                    AppToast.show(
                            container.getContext(),
                            added ? R.string.prep_banner_added_list : R.string.prep_banner_my_list);
                });
            }

            container.addView(view, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }
}
