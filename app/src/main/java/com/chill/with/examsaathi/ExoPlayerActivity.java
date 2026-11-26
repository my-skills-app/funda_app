package com.chill.with.examsaathi;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import java.lang.ref.WeakReference;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.media3.ui.*;
import androidx.media3.ui.PlayerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.bumptech.glide.Glide;
import com.iambedant.text.*;
import com.makeramen.roundedimageview.*;
import com.sanojpunchihewa.glowbutton.*;
import eightbitlab.com.blurview.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.regex.*;
import java.util.Random;
import jp.wasabeef.glide.transformations.*;
import me.mattak.outlined_text.*;
import org.json.*;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.core.view.GestureDetectorCompat;
import android.provider.Settings;

import androidx.media3.common.C;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.TrackSelectionView;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import androidx.media3.common.Format;
import androidx.appcompat.app.AppCompatDialog;

import androidx.appcompat.app.AppCompatDelegate;

import androidx.media3.ui.AspectRatioFrameLayout;
import com.google.firebase.database.ServerValue;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.app.PictureInPictureParams;
import android.util.Rational;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.PowerManager;

public class ExoPlayerActivity extends AppCompatActivity {

	@Nullable
	private static WeakReference<ExoPlayerActivity> sActivePlayer;

	public static final String EXTRA_CHAPTER_SHLOKAS_JSON = "player_chapter_shlokas_json";
	public static final String EXTRA_CHAPTER_TITLE = "player_chapter_title";
	public static final String EXTRA_CURRENT_SHLOKA_ID = "player_current_shloka_id";
	public static final String EXTRA_FULL_GITA_AUDIO = "player_full_gita_audio";
	public static final String EXTRA_FULL_GITA_CHAPTER_ID = "player_full_gita_chapter_id";

	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

	private ExoPlayer exoPlayer;
	private String subtitleURL = "";
	private String videoURL = "";
	private TrackSelectionParameters trackSelectionParameters;
	private Bundle myBundle;
	private boolean isShowingTrackSelectionDialog = false;
	int currentBrightness;
	int currentVolume;
	GestureDetectorCompat gestureDetectorCompat;
	AudioManager audioManager;
	private boolean shouldShowController = false;
	int touchPositionX;
	private boolean loading = false;
	DefaultTrackSelector defaultTrackSelector;
	long resumePosition = 0;
	private String link = "";
	private String title = "";
	private String img = "";
	private String videoid = "";
	private String description = "";
	Runnable retryRunnable;
	Handler retryHandler = new Handler();;
	int REQUEST_CODE_PERMISSIONS = 1001;
	float brightnessStart = 0f;
	float volumeStart = 0f;
	Handler gestureUiHandler = new Handler();;
	Runnable hideGestureUiRunnable;
	private String type = "";
	private final List<GitaChapterShlokas.Shloka> playlist = new ArrayList<>();
	private int currentPlaylistIndex = -1;
	private String chapterTitle = "";
	private boolean fullGitaAudioMode;
	private int currentGitaChapterId;
	private boolean loadingNextGitaChapter;
	private TextView fileNameTv;
	private ImageView moreVideosBtn;
	private PlayerMoreVideosSheet moreVideosSheet;
	private boolean playWhenReadyBeforePause = true;
	private static final int PIP_EXIT_CHECK_DELAY_MS = 200;
	private static final int PIP_EXIT_CHECK_MAX_ATTEMPTS = 6;
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private final Runnable pipExitCheckRunnable = this::handleExitedPictureInPictureMode;
	private final Runnable pipDismissOnStopRunnable = this::handlePipDismissedWhileStopped;
	private int pipExitCheckAttempts;
	private boolean wasInPipMode;
	private boolean pipBringToFrontAttempted;

	private RelativeLayout rl1;
	private PlayerView playerView;
	private ImageView audioArtwork;
	private LinearLayout linear1;
	private LinearLayout brightness_volume_container;
	private ImageView volume_icon;
	private ImageView brightness_icon;
	private TextView brightness_volume_tv;
	private ProgressBar progressbar1;

	private Intent i = new Intent();
	private FirebaseAuth auth;
	private OnCompleteListener<AuthResult> _auth_create_user_listener;
	private OnCompleteListener<AuthResult> _auth_sign_in_listener;
	private OnCompleteListener<Void> _auth_reset_password_listener;
	private OnCompleteListener<Void> auth_updateEmailListener;
	private OnCompleteListener<Void> auth_updatePasswordListener;
	private OnCompleteListener<Void> auth_emailVerificationSentListener;
	private OnCompleteListener<Void> auth_deleteUserListener;
	private OnCompleteListener<Void> auth_updateProfileListener;
	private OnCompleteListener<AuthResult> auth_phoneAuthListener;
	private OnCompleteListener<AuthResult> auth_googleSignInListener;
	private DatabaseReference recent = _firebase.getReference("recent_watch");
	private ChildEventListener _recent_child_listener;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		BackgroundPlaybackStopper.stopAllBackgroundAudio(this);
		retireOtherPlayerInstances(this);
		setContentView(R.layout.exo_player);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}

	private void initialize(Bundle _savedInstanceState) {
		myBundle = _savedInstanceState;
		rl1 = findViewById(R.id.rl1);
		playerView = findViewById(R.id.playerView);
		audioArtwork = findViewById(R.id.audio_artwork);
		linear1 = findViewById(R.id.linear1);
		brightness_volume_container = findViewById(R.id.brightness_volume_container);
		volume_icon = findViewById(R.id.volume_icon);
		brightness_icon = findViewById(R.id.brightness_icon);
		brightness_volume_tv = findViewById(R.id.brightness_volume_tv);
		progressbar1 = findViewById(R.id.progressbar1);
		auth = FirebaseAuth.getInstance();

		brightness_volume_container.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {

				return true;
			}
		});

		_recent_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = FirebaseSnapshotHelper.asMap(_param1);

			}

			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = FirebaseSnapshotHelper.asMap(_param1);

			}

			@Override
			public void onChildMoved(DataSnapshot _param1, String _param2) {

			}

			@Override
			public void onChildRemoved(DataSnapshot _param1) {
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = FirebaseSnapshotHelper.asMap(_param1);

			}

			@Override
			public void onCancelled(DatabaseError _param1) {
				final int _errorCode = _param1.getCode();
				final String _errorMessage = _param1.getMessage();

			}
		};
		recent.addChildEventListener(_recent_child_listener);

		auth_updateEmailListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_updatePasswordListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_emailVerificationSentListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_deleteUserListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_phoneAuthListener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> task) {
				final boolean _success = task.isSuccessful();
				final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";

			}
		};

		auth_updateProfileListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_googleSignInListener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> task) {
				final boolean _success = task.isSuccessful();
				final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";

			}
		};

		_auth_create_user_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		_auth_sign_in_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		_auth_reset_password_listener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();

			}
		};
	}

	private void initializeLogic() {
		type = getIntent().getStringExtra("type");
		final boolean gitaPlaybackMode = isGitaPlaybackMode();
		getWindow().setStatusBarColor(Color.TRANSPARENT);
		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
				WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			getWindow()
					.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}
		hideSystemUI();

		getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
			if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
				hideSystemUI();
			}
		});

		if (gitaPlaybackMode) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
			rl1.setBackgroundResource(R.drawable.bg_home_glass);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
		if (isGitaAudioMode()) {
			setupAudioOnlyUi();
		}

		playerView.setEnabled(false);
		try {
			trackSelectionParameters = TrackSelectionParameters.fromBundle(
					myBundle.getBundle("track_selection_parameters"));
		} catch (Exception e) {
			trackSelectionParameters = new TrackSelectionParameters.Builder(this).build();
		}

		link = getIntent().getStringExtra("link");
		title = getIntent().getStringExtra("title");
		img = getIntent().getStringExtra("img");
		videoid = getIntent().getStringExtra("videoid");
		description = getIntent().getStringExtra("description");
		if (type == null) {
			type = "";
		}
		try {
			String positionStr = getIntent().getStringExtra("position");
			if (positionStr != null && !positionStr.isEmpty()) {
				resumePosition = Long.parseLong(positionStr);
			}
		} catch (Exception e) {
			resumePosition = 0;
		}
		if (myBundle != null && myBundle.containsKey("playback_position")) {
			resumePosition = myBundle.getLong("playback_position", resumePosition);
		}
		if (myBundle != null && myBundle.containsKey("play_when_ready")) {
			playWhenReadyBeforePause = myBundle.getBoolean("play_when_ready", true);
		}

		loadPlaylistFromIntent();
		if (exoPlayer != null) {
			releasePlayerOnly();
		}
		exoPlayer = FastPlaybackExoPlayer.create(this);
		exoPlayer.addListener(new Player.Listener() {
			@Override
			public void onPlayerError(@NonNull PlaybackException error) {
				Log.e("ExoPlayerActivity", "Playback failed for " + videoURL, error);
				runOnUiThread(() -> showMessage(getString(R.string.playback_failed)));
			}

			@Override
			public void onPlaybackStateChanged(int playbackState) {
				if (playbackState != Player.STATE_ENDED) {
					return;
				}
				if (hasChapterPlaylist()) {
					playNextInPlaylist();
				} else if (fullGitaAudioMode && !loadingNextGitaChapter) {
					loadNextGitaChapter();
				}
			}
		});
		startPlayback();
		_initVars();
		volume_icon.setImageResource(R.drawable.volume_up_24);
		brightness_icon.setImageResource(R.drawable.brightness_6_24);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		try {
			ContentResolver resolver = getContentResolver();
			currentBrightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS);
		} catch (Settings.SettingNotFoundException e) {
			currentBrightness = 50;
		}

		currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

		playerView.setOnTouchListener((view, motionEvent) -> {
			gestureDetectorCompat.onTouchEvent(motionEvent);

			if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				brightness_volume_container.setVisibility(View.GONE);

				if (!shouldShowController) {
					playerView.setUseController(false);
					new Handler().postDelayed(() -> {
						shouldShowController = true;
						playerView.setUseController(true);
					}, 500);
				}
			}

			if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
				touchPositionX = (int) motionEvent.getX();
			}

			return false;
		});

		_setupGestureDetector();
		_setInitialBrightnessAndVolume();
	}

	private void startPlayback() {
		startPlaybackInternal(false);
	}

	private void startPlaybackInternal(boolean switchingVideo) {
		if (link == null || link.trim().isEmpty()) {
			showMessage(getString(R.string.video_unavailable));
			return;
		}
		videoURL = link.trim();
		try {
			playerView.setPlayer(exoPlayer);
			playerView.setShowNextButton(false);
			playerView.setShowPreviousButton(false);
			playerView.setControllerShowTimeoutMs(2500);

			MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoURL));
			exoPlayer.setTrackSelectionParameters(trackSelectionParameters);
			exoPlayer.setMediaItem(mediaItem);
			exoPlayer.prepare();
			exoPlayer.seekTo(switchingVideo ? 0L : resumePosition);
			exoPlayer.setPlayWhenReady(true);
			playerView.setEnabled(true);
			if (switchingVideo) {
				recordCurrentShlokaAsRecent();
			}
		} catch (Exception e) {
			Log.e("ExoPlayerActivity", "Failed to start playback for " + videoURL, e);
			showMessage(getString(R.string.playback_failed));
		}
	}

	private void loadPlaylistFromIntent() {
		playlist.clear();
		fullGitaAudioMode = getIntent().getBooleanExtra(EXTRA_FULL_GITA_AUDIO, false);
		currentGitaChapterId = getIntent().getIntExtra(EXTRA_FULL_GITA_CHAPTER_ID, 0);
		String json = getIntent().getStringExtra(EXTRA_CHAPTER_SHLOKAS_JSON);
		playlist.addAll(GitaPlayerPlaylist.parsePlayable(json));
		String chapter = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);
		chapterTitle = chapter != null ? chapter : "";
		if (playlist.isEmpty()) {
			currentPlaylistIndex = -1;
			return;
		}
		String currentId = getIntent().getStringExtra(EXTRA_CURRENT_SHLOKA_ID);
		currentPlaylistIndex = GitaPlayerPlaylist.findCurrentIndex(playlist, currentId, title);
		if (currentPlaylistIndex < 0) {
			currentPlaylistIndex = 0;
		}
		applyCurrentShlokaMetadata(false);
	}

	private void applyCurrentShlokaMetadata(boolean recordRecent) {
		if (currentPlaylistIndex < 0 || currentPlaylistIndex >= playlist.size()) {
			return;
		}
		GitaChapterShlokas.Shloka shloka = playlist.get(currentPlaylistIndex);
		if (shloka.videoUrl != null && !shloka.videoUrl.isEmpty()) {
			link = shloka.videoUrl;
		}
		title = shloka.title != null ? shloka.title : title;
		img = shloka.image != null ? shloka.image : img;
		videoid = shloka.id != null ? shloka.id : videoid;
		description = GitaChapterShlokas.displayDescription(shloka);
		if (fileNameTv != null) {
			fileNameTv.setText(title);
		}
		if (recordRecent) {
			recordCurrentShlokaAsRecent();
		}
		if (moreVideosSheet != null) {
			moreVideosSheet.updateCurrentIndex(currentPlaylistIndex);
		}
		updateAudioArtwork();
	}

	private void setupAudioOnlyUi() {
		if (audioArtwork != null) {
			audioArtwork.setVisibility(View.VISIBLE);
		}
		updateAudioArtwork();
	}

	private void updateAudioArtwork() {
		if (audioArtwork == null || !isGitaAudioMode() || isDestroyed() || isFinishing()) {
			return;
		}
		if (img != null && !img.trim().isEmpty()) {
			Glide.with(this)
					.load(img)
					.placeholder(R.color.view_bg_color)
					.error(R.color.view_bg_color)
					.centerCrop()
					.into(audioArtwork);
		} else {
			audioArtwork.setImageResource(R.color.view_bg_color);
		}
	}

	private void recordCurrentShlokaAsRecent() {
		if (currentPlaylistIndex >= 0 && currentPlaylistIndex < playlist.size()) {
			LocalRecentViews.add(this, LocalRecentViews.fromShloka(playlist.get(currentPlaylistIndex)));
			return;
		}
		LocalRecentViews.Entry entry = new LocalRecentViews.Entry();
		entry.title = title != null ? title : "";
		entry.posterUrl = img != null ? img : "";
		entry.description = description != null ? description : "";
		entry.videoUrl = link != null ? link : "";
		entry.videoid = videoid != null ? videoid : "";
		LocalRecentViews.add(this, entry);
	}

	private boolean hasChapterPlaylist() {
		return currentPlaylistIndex >= 0 && currentPlaylistIndex + 1 < playlist.size();
	}

	private void playNextInPlaylist() {
		if (currentPlaylistIndex + 1 >= playlist.size()) {
			return;
		}
		playAtIndex(currentPlaylistIndex + 1);
	}

	private void playPreviousInPlaylist() {
		if (currentPlaylistIndex <= 0) {
			return;
		}
		playAtIndex(currentPlaylistIndex - 1);
	}

	/** Vertical swipe playlist (Gita / Prep series topics). */
	private boolean isVerticalSwipePlaylistEnabled() {
		return isGitaVerticalMode() && playlist.size() > 1 && !fullGitaAudioMode;
	}

	private void loadNextGitaChapter() {
		if (!fullGitaAudioMode || loadingNextGitaChapter || currentGitaChapterId <= 0) {
			return;
		}
		loadingNextGitaChapter = true;
		Toast.makeText(this, R.string.full_gita_audio_loading_next_chapter, Toast.LENGTH_SHORT).show();
		GitaFullAudioQueue.loadNextChapterAfter(currentGitaChapterId,
				new GitaFullAudioQueue.NextChapterCallback() {
					@Override
					public void onLoaded(GitaFullAudioQueue.ChapterPlaylist chapterPlaylist) {
						if (isFinishing()) {
							return;
						}
						loadingNextGitaChapter = false;
						if (chapterPlaylist == null
								|| chapterPlaylist.shlokas == null
								|| chapterPlaylist.shlokas.isEmpty()) {
							Toast.makeText(ExoPlayerActivity.this,
									R.string.full_gita_audio_complete, Toast.LENGTH_LONG).show();
							return;
						}
						int nextIndex = currentPlaylistIndex + 1;
						playlist.addAll(chapterPlaylist.shlokas);
						currentGitaChapterId = chapterPlaylist.chapterId;
						chapterTitle = chapterPlaylist.chapterTitle != null
								? chapterPlaylist.chapterTitle
								: chapterTitle;
						if (moreVideosSheet != null) {
							moreVideosSheet.updateCurrentIndex(currentPlaylistIndex);
						}
						updateMoreVideosButtonVisibility();
						playAtIndex(nextIndex);
					}

					@Override
					public void onNoMoreChapters() {
						if (isFinishing()) {
							return;
						}
						loadingNextGitaChapter = false;
						Toast.makeText(ExoPlayerActivity.this,
								R.string.full_gita_audio_complete, Toast.LENGTH_LONG).show();
					}
				});
	}

	private void playAtIndex(int index) {
		if (index < 0 || index >= playlist.size()) {
			return;
		}
		currentPlaylistIndex = index;
		resumePosition = 0;
		applyCurrentShlokaMetadata(true);
		if (exoPlayer != null) {
			exoPlayer.stop();
			exoPlayer.clearMediaItems();
		}
		startPlaybackInternal(true);
	}

	private void openMoreVideosSheet() {
		if (playlist.isEmpty()) {
			return;
		}
		moreVideosSheet = PlayerMoreVideosSheet.show(
				this,
				playlist,
				chapterTitle,
				currentPlaylistIndex,
				this::playAtIndex);
	}

	private void updateMoreVideosButtonVisibility() {
		if (moreVideosBtn == null) {
			return;
		}
		boolean show = isGitaPlaybackMode() && playlist.size() > 1 && !fullGitaAudioMode;
		moreVideosBtn.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (exoPlayer != null) {
			outState.putLong("playback_position", exoPlayer.getCurrentPosition());
			outState.putBoolean("play_when_ready", exoPlayer.getPlayWhenReady());
		}
		if (trackSelectionParameters != null) {
			outState.putBundle("track_selection_parameters", trackSelectionParameters.toBundle());
		}
	}

	private static void retireOtherPlayerInstances(@NonNull ExoPlayerActivity current) {
		ExoPlayerActivity previous = sActivePlayer != null ? sActivePlayer.get() : null;
		sActivePlayer = new WeakReference<>(current);
		if (previous != null && previous != current && !previous.isFinishing()) {
			previous.forceStopVideoPlayback();
		}
	}

	/** Stops this player (including PiP) when another video session starts. */
	public void forceStopVideoPlayback() {
		runOnUiThread(() -> {
			if (isFinishing()) {
				return;
			}
			releasePlayerOnly();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				ExoPlayerActivity.this.finishAndRemoveTask();
			} else {
				finish();
			}
		});
	}

	private void releasePlayerOnly() {
		if (exoPlayer != null) {
			exoPlayer.setPlayWhenReady(false);
			exoPlayer.stop();
			if (playerView != null) {
				playerView.setPlayer(null);
			}
			exoPlayer.release();
			exoPlayer = null;
		}
	}

	@Override
	protected void onDestroy() {
		mainHandler.removeCallbacks(pipExitCheckRunnable);
		mainHandler.removeCallbacks(pipDismissOnStopRunnable);
		if (sActivePlayer != null && sActivePlayer.get() == this) {
			sActivePlayer = null;
		}
		if (audioArtwork != null) {
			// Activity is already destroyed here; Glide.with(this) throws
			// IllegalArgumentException.
			Glide.with(getApplicationContext()).clear(audioArtwork);
		}
		if (exoPlayer != null) {
			exoPlayer.setPlayWhenReady(false);
			exoPlayer.stop();
			if (playerView != null) {
				playerView.setPlayer(null);
			}
			exoPlayer.release();
			exoPlayer = null;
		}
		super.onDestroy();

		if (_recent_child_listener != null) {
			recent.removeEventListener(_recent_child_listener);
		}
	}

	@Override
	public void onBackPressed() {
		if (exoPlayer != null) {
			long position = exoPlayer.getCurrentPosition();
			long duration = exoPlayer.getDuration();

			FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
			String videoKey = getIntent().getStringExtra("videoid");
			if (user != null && videoKey != null && !videoKey.isEmpty()) {
				DatabaseReference recentRef = FirebaseDatabase.getInstance()
						.getReference("recent_watch")
						.child(user.getUid());

				String seriesKey = getIntent().getStringExtra("seriesKey");
				String category = getIntent().getStringExtra("category");

				HashMap<String, Object> map = new HashMap<>();
				map.put("seriesKey", seriesKey);
				map.put("category", category);
				map.put("type", type);
				map.put("description", description);
				map.put("videoid", videoid);
				map.put("link", link);
				map.put("title", title);
				map.put("img", img);
				map.put("position", position);
				map.put("duration", duration);
				map.put("timestamp", ServerValue.TIMESTAMP);

				recentRef.child(videoKey).setValue(map);
			}
		}

		finish();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onResume() {
		super.onResume();
		mainHandler.removeCallbacks(pipExitCheckRunnable);
		mainHandler.removeCallbacks(pipDismissOnStopRunnable);
		wasInPipMode = false;
		if (exoPlayer != null && !isInPictureInPictureMode()) {
			if (playerView != null && playerView.getPlayer() != exoPlayer) {
				playerView.setPlayer(exoPlayer);
			}
			if (isActivityResumed()) {
				exoPlayer.setPlayWhenReady(playWhenReadyBeforePause);
				if (playerView != null) {
					playerView.showController();
				}
			}
		}
	}

	@Override
	public void onPause() {
		if (exoPlayer != null) {
			playWhenReadyBeforePause = exoPlayer.getPlayWhenReady();
			// Only pause if NOT in PiP mode. If in PiP, let it play.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				if (!isInPictureInPictureMode()) {
					exoPlayer.setPlayWhenReady(false);
				}
			} else {
				exoPlayer.setPlayWhenReady(false);
			}
		}
		super.onPause();
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		if (supportsPictureInPicture() && exoPlayer != null && exoPlayer.isPlaying()) {
			enterPictureInPictureModeSafely();
		}
	}

	@Override
	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
		super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
		if (isInPictureInPictureMode) {
			wasInPipMode = true;
			pipBringToFrontAttempted = false;
			if (brightness_volume_container != null) {
				brightness_volume_container.setVisibility(View.GONE);
			}
			if (playerView != null) {
				playerView.hideController();
			}
			hideSystemUI();
		} else {
			schedulePipExitCheck();
		}
	}

	private void schedulePipExitCheck() {
		pipExitCheckAttempts = 0;
		mainHandler.removeCallbacks(pipExitCheckRunnable);
		mainHandler.postDelayed(pipExitCheckRunnable, PIP_EXIT_CHECK_DELAY_MS);
	}

	/**
	 * Restores fullscreen playback after PiP expand; never finishes here (that
	 * races onResume).
	 */
	private void handleExitedPictureInPictureMode() {
		if (isFinishing() || isDestroyed()) {
			return;
		}
		if (isInPictureInPictureMode()) {
			return;
		}
		if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
			restorePlaybackAfterLeavingPip();
			return;
		}
		pipExitCheckAttempts++;
		if (pipExitCheckAttempts < PIP_EXIT_CHECK_MAX_ATTEMPTS) {
			mainHandler.postDelayed(pipExitCheckRunnable, PIP_EXIT_CHECK_DELAY_MS);
		}
	}

	private void restorePlaybackAfterLeavingPip() {
		if (playerView != null && exoPlayer != null && playerView.getPlayer() != exoPlayer) {
			playerView.setPlayer(exoPlayer);
		}
		if (exoPlayer != null && isActivityResumed()) {
			exoPlayer.setPlayWhenReady(playWhenReadyBeforePause);
			if (playerView != null) {
				playerView.showController();
			}
		}
	}

	private void bringPlayerActivityToFront() {
		if (isFinishing() || isDestroyed() || isInPictureInPictureMode()) {
			return;
		}
		Intent intent = new Intent(this, ExoPlayerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
	}

	/**
	 * PiP window closed with X — activity stops without returning to fullscreen.
	 */
	private void handlePipDismissedWhileStopped() {
		if (isFinishing() || isDestroyed()) {
			return;
		}
		// If we are now resumed, it means we returned from PiP to fullscreen
		if (isActivityResumed()) {
			wasInPipMode = false;
			return;
		}
		// If we were in PiP and now we are NOT in PiP and NOT resumed, it means PiP was closed
		if (wasInPipMode && (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode())) {
			stopPlaybackAndFinish();
		}
	}

	private void stopPlaybackAndFinish() {
		if (exoPlayer != null) {
			exoPlayer.setPlayWhenReady(false);
			exoPlayer.stop();
			if (playerView != null) {
				playerView.setPlayer(null);
			}
			exoPlayer.release();
			exoPlayer = null;
		}
		if (!isFinishing()) {
			finish();
		}
	}

	@Override
	protected void onStop() {
		mainHandler.removeCallbacks(pipExitCheckRunnable);
		if (exoPlayer != null) {
			boolean inPip = false;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				inPip = isInPictureInPictureMode();
			}
			
			if (!inPip && !isChangingConfigurations() && !isActivityResumed()) {
				exoPlayer.setPlayWhenReady(false);
			}
		}
		
		// If we were in PiP mode and now we are stopping, check if it was a dismissal
		if (wasInPipMode) {
			mainHandler.removeCallbacks(pipDismissOnStopRunnable);
			mainHandler.postDelayed(pipDismissOnStopRunnable, 500); // Increased delay to be safer
		}
		super.onStop();
	}



	private boolean isActivityResumed() {
		return getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
	}

	@Override
	public void onStart() {
		super.onStart();
		brightness_volume_container.setVisibility(View.GONE);
	}

	public void _initVars() {
		final ImageView fit_screen_btn = (ImageView) playerView.findViewById(R.id.fit_screen_btn);
		final ImageView backward_10 = (ImageView) playerView.findViewById(R.id.backward_10);
		final ImageView forward_10 = (ImageView) playerView.findViewById(R.id.forward_10);
		final ImageView quality_selection_btn = (ImageView) playerView.findViewById(R.id.quality_selection_btn);
		final View backButton = playerView.findViewById(R.id.back_button);
		fileNameTv = playerView.findViewById(R.id.file_name_tv);
		moreVideosBtn = playerView.findViewById(R.id.more_videos_btn);
		final View playerTopBar = playerView.findViewById(R.id.player_top_bar);
		final ImageView screen_rotate_btn = (ImageView) playerView.findViewById(R.id.screen_rotate_btn);
		final ImageView pip_btn = playerView.findViewById(R.id.pip_btn);
		final View exoSettings = playerView.findViewById(androidx.media3.ui.R.id.exo_settings);
		final View exoSubtitle = playerView.findViewById(androidx.media3.ui.R.id.exo_subtitle);

		setupPlayerTopBarInsets(playerTopBar);

		if (backButton != null) {
			backButton.setVisibility(View.VISIBLE);
			backButton.setOnClickListener(v -> {
				if (exoPlayer != null) {
					exoPlayer.stop();
					exoPlayer.release();
					exoPlayer = null;
				}
				onBackPressed();
			});
		}

		if (isGitaPlaybackMode()) {
			screen_rotate_btn.setVisibility(View.GONE);
			playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
		}
		if (isGitaAudioMode() && fit_screen_btn != null) {
			fit_screen_btn.setVisibility(View.GONE);
		}
		if (exoSettings != null) {
			exoSettings.setVisibility(View.GONE);
		}
		if (exoSubtitle != null) {
			exoSubtitle.setVisibility(View.GONE);
		}

		updateMoreVideosButtonVisibility();
		if (moreVideosBtn != null) {
			moreVideosBtn.setContentDescription(getString(R.string.player_more_videos));
			moreVideosBtn.setOnClickListener(v -> openMoreVideosSheet());
		}

		fit_screen_btn.setOnClickListener(v -> {
			if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
				playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
				fit_screen_btn.setImageResource(R.drawable.crop_5_4);
				Toast.makeText(this, R.string.player_zoom, Toast.LENGTH_SHORT).show();
			} else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
				playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
				fit_screen_btn.setImageResource(R.drawable.fit_screen);
				Toast.makeText(this, R.string.player_fill, Toast.LENGTH_SHORT).show();
			} else {
				playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
				fit_screen_btn.setImageResource(R.drawable.crop_free);
				Toast.makeText(this, R.string.player_fit, Toast.LENGTH_SHORT).show();
			}
		});
		backward_10.setOnClickListener(view -> exoPlayer.seekTo(exoPlayer.getCurrentPosition() - 10000));
		forward_10.setOnClickListener(view -> exoPlayer.seekTo(exoPlayer.getCurrentPosition() + 10000));
		quality_selection_btn.setOnClickListener(view -> {
			isShowingTrackSelectionDialog = true;
			VideoQualityDialog.show(this, exoPlayer, () -> isShowingTrackSelectionDialog = false);
		});
		ProgressBar bufferingSpinner = playerView.findViewById(androidx.media3.ui.R.id.exo_buffering);
		if (bufferingSpinner != null) {
			// Set size (180x180 pixels)
			ViewGroup.LayoutParams params = bufferingSpinner.getLayoutParams();
			params.width = 155;
			params.height = 155;
			bufferingSpinner.setLayoutParams(params);

			bufferingSpinner.getIndeterminateDrawable().setColorFilter(
					0xFFFF0000,
					android.graphics.PorterDuff.Mode.SRC_IN);
		}
		screen_rotate_btn.setOnClickListener(view -> {
			if (isGitaPlaybackMode()) {
				return;
			}
			int currentOrientation = getResources().getConfiguration().orientation;

			if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
			} else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			}
		});
		if (pip_btn != null) {
			if (supportsPictureInPicture()) {
				pip_btn.setVisibility(View.VISIBLE);
				pip_btn.setOnClickListener(v -> enterPictureInPictureModeSafely());
			} else {
				pip_btn.setVisibility(View.GONE);
			}
		}
		if (fileNameTv != null) {
			fileNameTv.setText(title);
		}
	}

	private void setupPlayerTopBarInsets(View playerTopBar) {
		if (playerTopBar == null) {
			return;
		}
		ViewCompat.setOnApplyWindowInsetsListener(playerTopBar, (view, windowInsets) -> {
			androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
			view.setPadding(
					view.getPaddingLeft(),
					insets.top + dp(8),
					view.getPaddingRight(),
					view.getPaddingBottom());
			return windowInsets;
		});
		ViewCompat.requestApplyInsets(playerTopBar);
	}

	private int dp(int value) {
		return Math.round(value * getResources().getDisplayMetrics().density);
	}

	public void _setupGestureDetector() {
		gestureDetectorCompat = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {

			@Override
			public boolean onDown(MotionEvent e) {
				int screenWidth = getResources().getDisplayMetrics().widthPixels;

				if (e.getX() < screenWidth / 2) {

					WindowManager.LayoutParams lp = getWindow().getAttributes();
					brightnessStart = (lp.screenBrightness == -1f) ? 0.5f : lp.screenBrightness;
					brightnessStart *= 100f;

					progressbar1.setMax(100);
					progressbar1.setProgress((int) brightnessStart);
					brightness_volume_tv.setText(String.format(Locale.US, "%.0f%%", brightnessStart));

					brightness_icon.setVisibility(View.VISIBLE);
					volume_icon.setVisibility(View.GONE);
				} else {

					volumeStart = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
					int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
					float volumePercent = (volumeStart / (float) maxVolume) * 100f;

					progressbar1.setMax(100);
					progressbar1.setProgress((int) volumePercent);
					brightness_volume_tv.setText(String.format(Locale.US, "%.0f%%", volumePercent));

					brightness_icon.setVisibility(View.GONE);
					volume_icon.setVisibility(View.VISIBLE);

					if (volumeStart == 0) {
						volume_icon.setImageResource(R.drawable.mute);
					} else {
						volume_icon.setImageResource(R.drawable.volume_up_24);
					}
				}
				return true;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (!isVerticalSwipePlaylistEnabled() || e1 == null || e2 == null) {
					return false;
				}
				if (Math.abs(velocityY) < 900f || Math.abs(velocityY) < Math.abs(velocityX) * 1.2f) {
					return false;
				}
				// Swipe up → next topic video; swipe down → previous
				if (velocityY < 0) {
					if (hasChapterPlaylist()) {
						playNextInPlaylist();
						return true;
					}
					return false;
				}
				if (currentPlaylistIndex > 0) {
					playPreviousInPlaylist();
					return true;
				}
				return false;
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				int screenWidth = getResources().getDisplayMetrics().widthPixels;
				int screenHeight = getResources().getDisplayMetrics().heightPixels;

				if (Math.abs(distanceY) > Math.abs(distanceX)) {
					// In vertical playlist mode, leave vertical drag for fling next/prev.
					if (isVerticalSwipePlaylistEnabled()) {
						return false;
					}
					_showGestureUI();

					float deltaY = e1.getY() - e2.getY();

					if (e1.getX() < screenWidth / 2) {

						brightness_icon.setVisibility(View.VISIBLE);
						volume_icon.setVisibility(View.GONE);

						float percentChange = (deltaY / screenHeight) * 100f;
						float newBrightness = brightnessStart + percentChange;
						newBrightness = Math.max(0f, Math.min(newBrightness, 100f));
						currentBrightness = (int) newBrightness;

						WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
						layoutParams.screenBrightness = newBrightness / 100f;
						getWindow().setAttributes(layoutParams);

						brightness_volume_tv.setText(String.format(Locale.US, "%.0f%%", newBrightness));
						progressbar1.setMax(100);
						progressbar1.setProgress((int) newBrightness);
					} else {

						brightness_icon.setVisibility(View.GONE);
						volume_icon.setVisibility(View.VISIBLE);

						int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
						float percentChange = (deltaY / screenHeight) * maxVolume;
						float newVolume = volumeStart + percentChange;
						newVolume = Math.max(0f, Math.min(newVolume, (float) maxVolume));
						currentVolume = (int) newVolume;

						audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);

						float volumePercent = (newVolume / maxVolume) * 100f;
						brightness_volume_tv.setText(String.format(Locale.US, "%.0f%%", volumePercent));
						progressbar1.setMax(100);
						progressbar1.setProgress((int) volumePercent);

						if (currentVolume == 0) {
							volume_icon.setImageResource(R.drawable.mute);
						} else {
							volume_icon.setImageResource(R.drawable.volume_up_24);
						}
					}
				}

				return true;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				int deviceWidth = getResources().getDisplayMetrics().widthPixels;
				float x = e.getX();

				if (x < deviceWidth / 2) {

					long newPos = Math.max(0, exoPlayer.getCurrentPosition() - 10000);
					exoPlayer.seekTo(newPos);

				} else {

					long newPos = Math.min(exoPlayer.getDuration(), exoPlayer.getCurrentPosition() + 10000);
					exoPlayer.seekTo(newPos);

				}

				return true;
			}
		});
	}

	public void _extra() {
	}

	private static final int UI_OPTIONS = View.SYSTEM_UI_FLAG_LOW_PROFILE
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			|
			View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			|
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

	private void hideSystemUI() {
		ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.hide();
		getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);
	}

	private boolean isGitaVerticalMode() {
		return type != null && "Gita".equalsIgnoreCase(type.trim());
	}

	private boolean isGitaAudioMode() {
		return type != null && "GitaAudio".equalsIgnoreCase(type.trim());
	}

	private boolean isGitaPlaybackMode() {
		return isGitaVerticalMode() || isGitaAudioMode();
	}

	private boolean supportsPictureInPicture() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
				&& getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
	}

	private void enterPictureInPictureModeSafely() {
		if (!supportsPictureInPicture()) {
			showMessage(getString(R.string.player_pip_unavailable));
			return;
		}
		if (exoPlayer == null || link == null || link.trim().isEmpty()) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Rational aspectRatio = isGitaPlaybackMode()
					? new Rational(9, 16)
					: new Rational(16, 9);
			PictureInPictureParams params = new PictureInPictureParams.Builder()
					.setAspectRatio(aspectRatio)
					.build();
			enterPictureInPictureMode(params);
		}
	}

	{
	}

	public void _setRipple1(final View _view) {
		ViewRippleHelper.applySelectableBackground(this, _view);
	}

	public void _showGestureUI() {
		brightness_volume_container.setVisibility(View.VISIBLE);
		progressbar1.setVisibility(View.VISIBLE);

		if (hideGestureUiRunnable != null) {
			gestureUiHandler.removeCallbacks(hideGestureUiRunnable);
		}

		hideGestureUiRunnable = new Runnable() {
			@Override
			public void run() {
				brightness_volume_container.animate().alpha(0f).setDuration(300).withEndAction(() -> {
					brightness_volume_container.setVisibility(View.GONE);
					brightness_volume_container.setAlpha(1f);
				}).start();

				progressbar1.animate().alpha(0f).setDuration(300).withEndAction(() -> {
					progressbar1.setVisibility(View.GONE);
					progressbar1.setAlpha(1f);
				}).start();
			}
		};
		gestureUiHandler.postDelayed(hideGestureUiRunnable, 1500);
	}

	public void _setInitialBrightnessAndVolume() {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		float brightness = (lp.screenBrightness == -1f) ? 0.5f : lp.screenBrightness;
		int brightnessPercent = (int) (brightness * 100);

		// Volume
		int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int volumePercent = (int) ((currentVol / (float) maxVol) * 100);

		brightness_icon.setVisibility(View.VISIBLE);
		volume_icon.setVisibility(View.GONE);
		brightness_volume_tv.setText(brightnessPercent + "%");
		progressbar1.setMax(100);
		progressbar1.setProgress(brightnessPercent);
	}

	@Deprecated
	public void showMessage(String _s) {
		Toast.makeText(getApplicationContext(), _s, Toast.LENGTH_SHORT).show();
	}

	@Deprecated
	public int getLocationX(View _v) {
		int _location[] = new int[2];
		_v.getLocationInWindow(_location);
		return _location[0];
	}

	@Deprecated
	public int getLocationY(View _v) {
		int _location[] = new int[2];
		_v.getLocationInWindow(_location);
		return _location[1];
	}

	@Deprecated
	public int getRandom(int _min, int _max) {
		Random random = new Random();
		return random.nextInt(_max - _min + 1) + _min;
	}

	@Deprecated
	public ArrayList<Double> getCheckedItemPositionsToArray(ListView _list) {
		ArrayList<Double> _result = new ArrayList<Double>();
		SparseBooleanArray _arr = _list.getCheckedItemPositions();
		for (int _iIdx = 0; _iIdx < _arr.size(); _iIdx++) {
			if (_arr.valueAt(_iIdx))
				_result.add((double) _arr.keyAt(_iIdx));
		}
		return _result;
	}

	@Deprecated
	public float getDip(int _input) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _input, getResources().getDisplayMetrics());
	}

	@Deprecated
	public int getDisplayWidthPixels() {
		return getResources().getDisplayMetrics().widthPixels;
	}

	@Deprecated
	public int getDisplayHeightPixels() {
		return getResources().getDisplayMetrics().heightPixels;
	}
}
