package com.chill.with.examsaathi;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
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
import com.iambedant.text.*;
import com.makeramen.roundedimageview.*;
import com.makeramen.roundedimageview.RoundedImageView;
import com.sanojpunchihewa.glowbutton.*;
import eightbitlab.com.blurview.*;
import eightbitlab.com.blurview.BlurView;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import jp.wasabeef.glide.transformations.*;
import me.mattak.outlined_text.*;
import org.json.*;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.core.widget.NestedScrollView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import com.airbnb.lottie.LottieAnimationView;

public class GitaShlokaViewActivity extends AppCompatActivity {

	public static final String EXTRA_GITA_CHAPTER_MODE = "gita_chapter_mode";
	public static final String EXTRA_CHAPTER_SHLOKAS_JSON = "chapter_shlokas_json";
	public static final String EXTRA_CHAPTER_TITLE = "gita_chapter_title";
	public static final String EXTRA_CHAPTER_ID = "gita_chapter_id";

	public static Intent newGitaShlokaIntent(
			Context context,
			GitaChapterShlokas.Shloka shloka,
			List<GitaChapterShlokas.Shloka> chapterShlokas,
			String chapterTitle,
			int chapterId) {
		Intent intent = new Intent(context, GitaShlokaViewActivity.class);
		intent.putExtra(EXTRA_GITA_CHAPTER_MODE, true);
		intent.putExtra("posterUrl", shloka.image != null ? shloka.image : "");
		intent.putExtra("title", shloka.title != null ? shloka.title : "");
		intent.putExtra("description", GitaChapterShlokas.displayDescription(shloka));
		intent.putExtra("videoUrl", shloka.videoUrl);
		intent.putExtra("videoid", shloka.id != null ? shloka.id : "");
		intent.putExtra("type", "Gita");
		intent.putExtra(EXTRA_CHAPTER_TITLE, chapterTitle);
		intent.putExtra(EXTRA_CHAPTER_ID, chapterId);
		intent.putExtra(EXTRA_CHAPTER_SHLOKAS_JSON, new Gson().toJson(chapterShlokas));
		return intent;
	}

	public static Intent newExoPlayerIntent(Context context, GitaChapterShlokas.Shloka shloka) {
		Intent intent = new Intent(context, ExoPlayerActivity.class);
		intent.putExtra("link", shloka.videoUrl);
		intent.putExtra("user_agent", "a");
		intent.putExtra("refere", "a");
		intent.putExtra("title", shloka.title != null ? shloka.title : "");
		intent.putExtra("videoid", shloka.id != null ? shloka.id : "");
		intent.putExtra("img", shloka.image != null ? shloka.image : "");
		intent.putExtra("description", GitaChapterShlokas.displayDescription(shloka));
		intent.putExtra("type", "Gita");
		intent.putExtra("seriesKey", "null");
		intent.putExtra("category", "null");
		return intent;
	}
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private String img = "";
	private String title = "";
	private String description = "";
	private boolean favorite = false;
	private HashMap<String, Object> map = new HashMap<>();
	DatabaseReference dbRef;
	DatabaseReference all;
	
	private ArrayList<HashMap<String, Object>> listmap = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> sublist = new ArrayList<>();
	
	private LinearLayout linear5;
	private LinearLayout linear6;
	private NestedScrollView nested;
	private ShimmerFrameLayout shimmerMovieView;
	private ImageView back_btn;
	private View linear10;
	private ImageView img_addToWatchList;
	private ImageView btn_share;
	private LinearLayout linear12;
	private RoundedImageView imageview4;
	private TextView textview1;
	private LinearLayout btn_play;
	private View btnPlayWithAudio;
	private LinearLayout linear17;
	private TextView textview3;
	private View view1;
	private LinearLayout linear19;
	private TextView des;
	private View linear20;
	private LinearLayout linear15;
	private RecyclerView recyclerview1;
	private TextView textview5;
	private TextView textview2;
	private TextView textview4;

	private List<GitaChapterShlokas.Shloka> gitaChapterShlokas = new ArrayList<>();
	private GitaShlokasAdapter gitaShlokasAdapter;
	private String selectedShlokaId = "";
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private static final long MOVIE_VIEW_SHIMMER_MS = 450L;
	private Runnable pendingShimmerReveal;
	private RequestNetwork list;
	private RequestNetwork.RequestListener _list_request_listener;
	private SharedPreferences sp;
	private DatabaseReference watchlist = _firebase.getReference("watchlist");
	private ChildEventListener _watchlist_child_listener;
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
	private Intent intent = new Intent();
	private Intent play = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_gita_shloka_view);
		bindViews();
		FirebaseApp.initializeApp(this);
		setupStatusBar();
		initializeLogic();
	}

	private void bindViews() {
		linear5 = findViewById(R.id.linear5);
		linear6 = findViewById(R.id.linear6);
		nested = findViewById(R.id.nested);
		shimmerMovieView = findViewById(R.id.shimmerMovieView);
		back_btn = findViewById(R.id.back_btn);
		linear10 = findViewById(R.id.linear10);
		img_addToWatchList = findViewById(R.id.img_addToWatchList);
		btn_share = findViewById(R.id.btn_share);
		linear12 = findViewById(R.id.linear12);
		imageview4 = findViewById(R.id.imageview4);
		textview1 = findViewById(R.id.textview1);
		btn_play = findViewById(R.id.btn_play);
		btnPlayWithAudio = findViewById(R.id.btnPlayWithAudio);
		linear17 = findViewById(R.id.linear17);
		textview3 = findViewById(R.id.textview3);
		view1 = findViewById(R.id.view1);
		linear19 = findViewById(R.id.linear19);
		des = findViewById(R.id.des);
		linear20 = findViewById(R.id.linear20);
		linear15 = findViewById(R.id.linear15);
		recyclerview1 = findViewById(R.id.recyclerview1);
		textview5 = findViewById(R.id.textview5);
		textview2 = findViewById(R.id.textview2);
		textview4 = findViewById(R.id.textview4);
		list = new RequestNetwork(this);
		sp = getSharedPreferences("sp", Activity.MODE_PRIVATE);
		auth = FirebaseAuth.getInstance();
		
		back_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				ActivityNav.finish(GitaShlokaViewActivity.this);
			}
		});
		
		img_addToWatchList.setOnClickListener(v -> toggleLocalWatchlist());

		btn_share.setOnClickListener(v -> AppShare.shareApp(GitaShlokaViewActivity.this));
		
		btn_play.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				openExoPlayerDirectly();
			}
		});

		if (btnPlayWithAudio != null) {
			btnPlayWithAudio.setOnClickListener(v -> openGitaAudioPlayer());
		}
		
		_list_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				
			}
			
			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;
				
			}
		};
		
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
	
	private void setupStatusBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.setStatusBarColor(Color.TRANSPARENT);
			window.setNavigationBarColor(getColor(R.color.colorPrimary));
			window.getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		}
		final int horizontalPad = (int) getDip(16);
		ViewCompat.setOnApplyWindowInsetsListener(linear6, (v, insets) -> {
			int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
			int bottom = v.getPaddingBottom();
			v.setPadding(horizontalPad, statusTop + (int) getDip(10), horizontalPad, bottom);
			return insets;
		});
		ViewCompat.requestApplyInsets(linear6);
	}
	
	private void initializeLogic() {
		back_btn.setImageResource(R.drawable.arrow_back_24);
		btn_share.setImageResource(R.drawable.ic_share);
		revealContentAfterShimmer(() -> {
			bindContentFromIntent();
			if (getIntent().getBooleanExtra(EXTRA_GITA_CHAPTER_MODE, false)) {
				recyclerview1.setLayoutManager(new GridLayoutManager(this, 2));
				loadGitaChapterShlokasRow();
			} else {
				recyclerview1.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
				loadFirebaseMoreLikeRow();
			}
			loadWatchlistState();
		});
	}

	private void revealContentAfterShimmer(Runnable work) {
		if (pendingShimmerReveal != null) {
			mainHandler.removeCallbacks(pendingShimmerReveal);
		}
		showMovieViewShimmer(true);
		pendingShimmerReveal = () -> {
			work.run();
			showMovieViewShimmer(false);
			pendingShimmerReveal = null;
		};
		mainHandler.postDelayed(pendingShimmerReveal, MOVIE_VIEW_SHIMMER_MS);
	}

	private void showMovieViewShimmer(boolean show) {
		if (shimmerMovieView == null) {
			return;
		}
		if (show) {
			shimmerMovieView.setVisibility(View.VISIBLE);
			shimmerMovieView.startShimmer();
			nested.setVisibility(View.INVISIBLE);
		} else {
			shimmerMovieView.stopShimmer();
			shimmerMovieView.setVisibility(View.GONE);
			nested.setVisibility(View.VISIBLE);
		}
	}

	private void openGitaAudioPlayer() {
		String videoUrl = getIntent().getStringExtra("videoUrl");
		if (videoUrl == null || videoUrl.trim().isEmpty()) {
			Toast.makeText(this, R.string.video_unavailable, Toast.LENGTH_SHORT).show();
			return;
		}
		BackgroundPlaybackStopper.stopAllBackgroundAudio(this);
		ActivityNav.start(this, GitaAudioPlayerActivity.newIntent(this, getIntent()));
	}

	private void updatePlayWithAudioVisibility() {
		if (btnPlayWithAudio == null) {
			return;
		}
		String videoUrl = getIntent().getStringExtra("videoUrl");
		boolean available = videoUrl != null && !videoUrl.trim().isEmpty();
		btnPlayWithAudio.setVisibility(available ? View.VISIBLE : View.GONE);
	}

	private void openExoPlayerDirectly() {
		String videoUrl = getIntent().getStringExtra("videoUrl");
		if (videoUrl == null || videoUrl.isEmpty()) {
			Toast.makeText(this, R.string.video_unavailable, Toast.LENGTH_SHORT).show();
			return;
		}
		Intent playerIntent = new Intent(this, ExoPlayerActivity.class);
		playerIntent.putExtra("link", videoUrl);
		playerIntent.putExtra("user_agent", "a");
		playerIntent.putExtra("refere", "a");
		playerIntent.putExtra("title", getIntent().getStringExtra("title"));
		playerIntent.putExtra("videoid", getIntent().getStringExtra("videoid"));
		playerIntent.putExtra("img", getIntent().getStringExtra("posterUrl"));
		playerIntent.putExtra("description", getIntent().getStringExtra("description"));
		String type = getIntent().getStringExtra("type");
		playerIntent.putExtra("type", type != null ? type : "Movie");
		playerIntent.putExtra("seriesKey", "null");
		playerIntent.putExtra("category", "null");
		GitaPlayerPlaylist.attachToPlayerIntent(getIntent(), playerIntent);
		ActivityNav.start(this, playerIntent);
	}

	private void bindContentFromIntent() {
		String posterUrl = getIntent().getStringExtra("posterUrl");
		if (posterUrl != null && !posterUrl.isEmpty()) {
			Glide.with(getApplicationContext()).load(Uri.parse(posterUrl)).into(imageview4);
		} else {
			imageview4.setImageResource(R.color.view_bg_color);
		}
		textview1.setText(getIntent().getStringExtra("title"));
		des.setText(getIntent().getStringExtra("description"));
		String videoid = getIntent().getStringExtra("videoid");
		selectedShlokaId = videoid != null ? videoid : "";
		LocalRecentViews.add(this, LocalRecentViews.fromIntent(getIntent()));
		updatePlayWithAudioVisibility();
	}

	private void applyGitaShloka(GitaChapterShlokas.Shloka shloka) {
		if (shloka == null) {
			return;
		}
		String shlokaId = shloka.id != null ? shloka.id : "";
		if (shlokaId.equals(selectedShlokaId)) {
			return;
		}
		if (shloka.videoUrl == null || shloka.videoUrl.isEmpty()) {
			Toast.makeText(this, R.string.video_unavailable, Toast.LENGTH_SHORT).show();
			return;
		}
		revealContentAfterShimmer(() -> {
			getIntent().putExtras(newGitaShlokaIntent(
					this,
					shloka,
					gitaChapterShlokas,
					getIntent().getStringExtra(EXTRA_CHAPTER_TITLE),
					getIntent().getIntExtra(EXTRA_CHAPTER_ID, 0)).getExtras());
			bindContentFromIntent();
			nested.scrollTo(0, 0);
			if (gitaShlokasAdapter != null) {
				gitaShlokasAdapter.notifyDataSetChanged();
			}
			loadWatchlistState();
		});
	}

	private void toggleLocalWatchlist() {
		String title = getIntent().getStringExtra("title");
		if (title == null || title.trim().isEmpty()) {
			Toast.makeText(this, R.string.video_unavailable, Toast.LENGTH_SHORT).show();
			return;
		}
		boolean added = LocalWatchlist.toggle(this, LocalWatchlist.fromIntent(getIntent()));
		updateWatchlistIcon();
		Toast.makeText(
				this,
				added ? R.string.watchlist_saved : R.string.watchlist_removed,
				Toast.LENGTH_SHORT).show();
	}

	private void updateWatchlistIcon() {
		img_addToWatchList.setVisibility(View.VISIBLE);
		String title = getIntent().getStringExtra("title");
		if (LocalWatchlist.isSaved(this, title)) {
			img_addToWatchList.setImageResource(R.drawable.ic_bookmark);
		} else {
			img_addToWatchList.setImageResource(R.drawable.ic_bookmark_not);
		}
	}

	private void loadWatchlistState() {
		updateWatchlistIcon();
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateWatchlistIcon();
	}

	private void loadFirebaseMoreLikeRow() {
		textview2.setText("More Like");
		textview4.setVisibility(View.VISIBLE);
		DatabaseReference all = FirebaseDatabase.getInstance()
				.getReference("Categories")
				.child("all");
		all.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot _dataSnapshot) {
				listmap = new ArrayList<>();
				try {
					for (DataSnapshot _data : _dataSnapshot.getChildren()) {
						HashMap<String, Object> _map = FirebaseSnapshotHelper.asMap(_data);
						listmap.add(_map);
					}
					Collections.shuffle(listmap);
					ArrayList<HashMap<String, Object>> sublist = new ArrayList<>(
							listmap.subList(0, Math.min(9, listmap.size())));
					recyclerview1.setAdapter(new Recyclerview1Adapter(sublist));
				} catch (Exception _e) {
					_e.printStackTrace();
				}
			}

			@Override
			public void onCancelled(DatabaseError _databaseError) {
			}
		});
	}

	private void loadGitaChapterShlokasRow() {
		String chapterTitle = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);
		if (chapterTitle == null || chapterTitle.isEmpty()) {
			textview2.setText(R.string.shloks_screen_title);
		} else {
			textview2.setText(getString(R.string.chapter_shlokas_more, chapterTitle));
		}
		textview4.setVisibility(View.GONE);
		recyclerview1.setClipToPadding(false);
		recyclerview1.setPadding(0, 0, 0, (int) getDip(8));

		if (gitaChapterShlokas == null || gitaChapterShlokas.isEmpty()) {
			String json = getIntent().getStringExtra(EXTRA_CHAPTER_SHLOKAS_JSON);
			if (json != null && !json.isEmpty()) {
				try {
					Type listType = new TypeToken<List<GitaChapterShlokas.Shloka>>() {}.getType();
					List<GitaChapterShlokas.Shloka> shlokas = new Gson().fromJson(json, listType);
					gitaChapterShlokas = shlokas != null ? shlokas : new ArrayList<>();
				} catch (Exception e) {
					gitaChapterShlokas = new ArrayList<>();
				}
			} else {
				gitaChapterShlokas = new ArrayList<>();
			}
		}
		gitaShlokasAdapter = new GitaShlokasAdapter(gitaChapterShlokas);
		recyclerview1.setAdapter(gitaShlokasAdapter);
	}
	
	
	@Override
	protected void onDestroy() {
		if (pendingShimmerReveal != null) {
			mainHandler.removeCallbacks(pendingShimmerReveal);
		}
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		ActivityNav.finish(this);
	}
	public void _Custom_Loading(final boolean _ifShow) {
		if (_ifShow) {
			if (coreprog == null){
				coreprog = new ProgressDialog(this);
				coreprog.setCancelable(false);
				coreprog.setCanceledOnTouchOutside(false);
				
				coreprog.requestWindowFeature(Window.FEATURE_NO_TITLE);  coreprog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
				
			}
			coreprog.setMessage(null);
			coreprog.show();
			View _view = getLayoutInflater().inflate(R.layout.custom_dialog, null);
			LinearLayout linear_base = (LinearLayout) _view.findViewById(R.id.linear_base);
			
			LottieAnimationView lottieAnimationView = _view.findViewById(R.id.lottieAnimationView);
			android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
			gd.setColor(Color.parseColor("#00000000"));
			gd.setCornerRadius(0);
			linear_base.setBackground(gd);
			coreprog.setContentView(_view);
			lottieAnimationView.setAnimation("anim2.json");
		} else {
			if (coreprog != null){
				coreprog.dismiss();
			}
		}
	}
	private ProgressDialog coreprog;
	{
	}
	
	public class GitaShlokasAdapter extends RecyclerView.Adapter<GitaShlokasAdapter.ViewHolder> {

		private final List<GitaChapterShlokas.Shloka> shlokas;

		public GitaShlokasAdapter(List<GitaChapterShlokas.Shloka> shlokas) {
			this.shlokas = shlokas != null ? shlokas : new ArrayList<>();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = getLayoutInflater().inflate(R.layout.item_gita_shloka_grid, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, final int position) {
			GitaChapterShlokas.Shloka shloka = shlokas.get(position);
			holder.txtTitle.setText(shloka.title);
			holder.txtDescription.setText(GitaChapterShlokas.shortDescription(shloka));
			if (shloka.image != null && !shloka.image.isEmpty()) {
				Glide.with(getApplicationContext())
						.load(shloka.image)
						.centerCrop()
						.placeholder(R.color.view_bg_color)
						.into(holder.imgShloka);
			} else {
				holder.imgShloka.setImageResource(R.color.view_bg_color);
			}
			String shlokaId = shloka.id != null ? shloka.id : "";
			float alpha = shlokaId.equals(selectedShlokaId) ? 1f : 0.92f;
			holder.itemView.setAlpha(alpha);
			holder.itemView.setOnClickListener(v -> applyGitaShloka(shloka));
		}

		@Override
		public int getItemCount() {
			return shlokas.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			final ImageView imgShloka;
			final TextView txtTitle;
			final TextView txtDescription;

			public ViewHolder(View v) {
				super(v);
				imgShloka = v.findViewById(R.id.imgShloka);
				txtTitle = v.findViewById(R.id.txtTitle);
				txtDescription = v.findViewById(R.id.txtDescription);
			}
		}
	}

	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.items, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final RelativeLayout RL1 = _view.findViewById(R.id.RL1);
			final RelativeLayout RL2 = _view.findViewById(R.id.RL2);
			final androidx.constraintlayout.utils.widget.ImageFilterView img = _view.findViewById(R.id.img);
			final View view1 = _view.findViewById(R.id.view1);
			final RelativeLayout RL3 = _view.findViewById(R.id.RL3);
			final TextView type = _view.findViewById(R.id.type);
			final View view2 = _view.findViewById(R.id.view2);
			
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
			_view.setLayoutParams(_lp);
			Glide.with(getApplicationContext()).load(Uri.parse(_data.get((int)_position).get("posterUrl").toString())).into(img);
			type.setText(_data.get((int)_position).get("type").toString());
			linear1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (_data.get((int)_position).get("type").toString().equals("Movie")) {
						intent.setClass(getApplicationContext(), ExoPlayerActivity.class);
						intent.putExtra("img", _data.get((int)_position).get("posterUrl").toString());
						intent.putExtra("title", _data.get((int)_position).get("title").toString());
						intent.putExtra("description", _data.get((int)_position).get("description").toString());
						intent.putExtra("link", _data.get((int)_position).get("videoUrl").toString());
						intent.putExtra("videoid", _data.get((int)_position).get("videoid").toString());
						intent.putExtra("type", _data.get((int)_position).get("type").toString());
						intent.putExtra("seriesKey", "null");
						startActivity(intent);
					} else {
						intent.setClass(getApplicationContext(), SeriesviewActivity.class);
						intent.putExtra("posterUrl", _data.get((int)_position).get("posterUrl").toString());
						intent.putExtra("title", _data.get((int)_position).get("title").toString());
						intent.putExtra("description", _data.get((int)_position).get("description").toString());
						intent.putExtra("videoid", _data.get((int)_position).get("videoid").toString());
						intent.putExtra("type", _data.get((int)_position).get("type").toString());
						intent.putExtra("seriesKey", _data.get((int)_position).get("videoid").toString());
						intent.putExtra("category", _data.get((int)_position).get("category").toString());
						startActivity(intent);
					}
				}
			});
		}
		
		@Override
		public int getItemCount() {
			return _data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
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
			_result.add((double)_arr.keyAt(_iIdx));
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
