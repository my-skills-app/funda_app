package com.chill.with.examsaathi;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
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
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.media3.ui.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
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
import android.view.View;
import com.google.android.material.snackbar.Snackbar;


public class SeriesviewActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	DatabaseReference dbRef;
	private String seriesKey = "";
	private String category = "";
	DatabaseReference userWatchlist;
	private String description = "";
	
	private ArrayList<HashMap<String, Object>> maplist = new ArrayList<>();
	
	private RelativeLayout root_lout;
	private RelativeLayout rl2;
	private LinearLayout linear5;
	private ImageView imageview1;
	private BlurView blurView;
	private RelativeLayout linear4;
	private ImageView shadow_top;
	private ImageView shadow_bottom;
	private LinearLayout linear6;
	private NestedScrollView nested;
	private ImageFilterView back_btn;
	private LinearLayout linear10;
	private ImageFilterView img_addToWatchList;
	private ImageFilterView btn_share;
	private LinearLayout linear12;
	private RoundedImageView imageview4;
	private TextView textview1;
	private LinearLayout linear17;
	private View view1;
	private LinearLayout linear19;
	private TextView des;
	private View linear20;
	private LinearLayout linear15;
	private RecyclerView recyclerview1;
	private TextView textview5;
	private TextView textview2;
	private TextView textview4;
	
	private DatabaseReference db = _firebase.getReference("db");
	private ChildEventListener _db_child_listener;
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
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.seriesview);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		root_lout = findViewById(R.id.root_lout);
		rl2 = findViewById(R.id.rl2);
		linear5 = findViewById(R.id.linear5);
		imageview1 = findViewById(R.id.imageview1);
		blurView = findViewById(R.id.blurView);
		linear4 = findViewById(R.id.linear4);
		shadow_top = findViewById(R.id.shadow_top);
		shadow_bottom = findViewById(R.id.shadow_bottom);
		linear6 = findViewById(R.id.linear6);
		nested = findViewById(R.id.nested);
		back_btn = findViewById(R.id.back_btn);
		linear10 = findViewById(R.id.linear10);
		img_addToWatchList = findViewById(R.id.img_addToWatchList);
		btn_share = findViewById(R.id.btn_share);
		linear12 = findViewById(R.id.linear12);
		imageview4 = findViewById(R.id.imageview4);
		textview1 = findViewById(R.id.textview1);
		linear17 = findViewById(R.id.linear17);
		view1 = findViewById(R.id.view1);
		linear19 = findViewById(R.id.linear19);
		des = findViewById(R.id.des);
		linear20 = findViewById(R.id.linear20);
		linear15 = findViewById(R.id.linear15);
		recyclerview1 = findViewById(R.id.recyclerview1);
		textview5 = findViewById(R.id.textview5);
		textview2 = findViewById(R.id.textview2);
		textview4 = findViewById(R.id.textview4);
		auth = FirebaseAuth.getInstance();
		
		img_addToWatchList.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_Custom_Loading(true); 
				
				String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
				String posterUrl = getIntent().getStringExtra("posterUrl");
				String title = getIntent().getStringExtra("title");
				String description = getIntent().getStringExtra("description");
				String videoUrl = getIntent().getStringExtra("videoUrl");
				String videoid = getIntent().getStringExtra("videoid");
				
				
				DatabaseReference userWatchlist = FirebaseDatabase.getInstance().getReference("watchlist").child(uid);
				
				userWatchlist.addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(DataSnapshot snapshot) {
						boolean found = false;
						String keyToRemove = null;
						
						for (DataSnapshot child : snapshot.getChildren()) {
							Object imgValue = child.child("posterUrl").getValue();
							if (imgValue != null && imgValue.toString().equals(posterUrl)) {
								found = true;
								keyToRemove = child.getKey();
								break;
							}
						}
						
						if (found) {
							
							userWatchlist.child(keyToRemove).removeValue().addOnCompleteListener(task -> {
								_Custom_Loading(false); 
								img_addToWatchList.setImageResource(R.drawable.ic_bookmark_not);
								Toast.makeText(getApplicationContext(), "Removed from watchlist", Toast.LENGTH_SHORT).show();
							});
						} else {
							
							Map<String, Object> newItem = new HashMap<>();
							newItem.put("posterUrl", posterUrl);
							newItem.put("title", title);
							newItem.put("description", description);
							newItem.put("videoUrl", videoUrl);
							newItem.put("videoid", videoid);
							newItem.put("type", "Series");
							newItem.put("key", seriesKey);
							newItem.put("category", category);
							userWatchlist.push().setValue(newItem).addOnCompleteListener(task -> {
								_Custom_Loading(false); 
								img_addToWatchList.setImageResource(R.drawable.ic_bookmark);
								Toast.makeText(getApplicationContext(), "Added to watchlist", Toast.LENGTH_SHORT).show();
							});
						}
					}
					
					@Override
					public void onCancelled(DatabaseError error) {
						_Custom_Loading(false); 
						Toast.makeText(getApplicationContext(), "Error occurred", Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		
		_db_child_listener = new ChildEventListener() {
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
		db.addChildEventListener(_db_child_listener);
		
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
		Glide.with(getApplicationContext()).load(Uri.parse(getIntent().getStringExtra("posterUrl"))).into(imageview1);
		Glide.with(getApplicationContext()).load(Uri.parse(getIntent().getStringExtra("posterUrl"))).into(imageview4);
		textview1.setText(getIntent().getStringExtra("title"));
		des.setText(getIntent().getStringExtra("description"));
		seriesKey = getIntent().getStringExtra("seriesKey");
		category = getIntent().getStringExtra("category");
		DatabaseReference dbRef = FirebaseDatabase.getInstance()
		.getReference("Categories")
		.child(getIntent().getStringExtra("category"))
		.child(getIntent().getStringExtra("seriesKey"))
		.child("sessions")
		.child("Session1");
		dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot _dataSnapshot) {
				maplist = new ArrayList<>();
				try {
					for (DataSnapshot _data : _dataSnapshot.getChildren()) {
						HashMap<String, Object> _map = FirebaseSnapshotHelper.asMap(_data);
						maplist.add(_map);
					}
				} catch (Exception _e) {
					_e.printStackTrace();
				}
				recyclerview1.setAdapter(new Recyclerview1Adapter(maplist));
			}
			@Override
			public void onCancelled(DatabaseError _databaseError) {
			}
		});
		recyclerview1.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL, false));
		float radius = 20f;
		
		View decorView = getWindow().getDecorView();
		// ViewGroup you want to start blur from. Choose root as close to BlurView in hierarchy as possible.
		ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
		
		// Optional:
		// Set drawable to draw in the beginning of each blurred frame.
		// Can be used in case your layout has a lot of transparent space and your content
		// gets a too low alpha value after blur is applied.
		Drawable windowBackground = decorView.getBackground();
		
		blurView.setupWith(rootView) // Optionally pass RenderEffectBlur or RenderScriptBlur as the second parameter
		.setFrameClearDrawable(windowBackground) // Optional. Useful when your root has a lot of transparent background, which results in semi-transparent blurred content. This will make the background opaque
		.setBlurRadius(radius);
		shadow_top.setImageResource(R.drawable.bg_gradient_linear_black);
		shadow_bottom.setImageResource(R.drawable.bg_gradient_linear_black);
		btn_share.setImageResource(R.drawable.ic_share);
		back_btn.setImageResource(R.drawable.ic_back);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			
			window.setNavigationBarColor(Color.parseColor("#100a0e"));
			window.getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
		
		String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
		String posterUrl = getIntent().getStringExtra("posterUrl");
		
		
		DatabaseReference userWatchlist = FirebaseDatabase.getInstance().getReference("watchlist").child(uid);
		
		
		userWatchlist.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot snapshot) {
				boolean isAdded = false;
				
				for (DataSnapshot child : snapshot.getChildren()) {
					String imgValue = String.valueOf(child.child("posterUrl").getValue());
					if (imgValue.equals(posterUrl)) {
						isAdded = true;
						break;
					}
				}
				
				
				if (isAdded) {
					img_addToWatchList.setImageResource(R.drawable.ic_bookmark);
				} else {
					img_addToWatchList.setImageResource(R.drawable.ic_bookmark_not);
				}
			}
			
			@Override
			public void onCancelled(DatabaseError error) {
				Toast.makeText(getApplicationContext(), "Error loading watchlist", Toast.LENGTH_SHORT).show();
			}
		});
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
	
	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.recent_watch, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			
			final androidx.appcompat.widget.LinearLayoutCompat linear1 = _view.findViewById(R.id.linear1);
			final androidx.cardview.widget.CardView cardview1 = _view.findViewById(R.id.cardview1);
			final androidx.appcompat.widget.LinearLayoutCompat linear6 = _view.findViewById(R.id.linear6);
			final FrameLayout linear2 = _view.findViewById(R.id.linear2);
			final androidx.constraintlayout.utils.widget.ImageFilterView img = _view.findViewById(R.id.img);
			final androidx.appcompat.widget.AppCompatImageView linear4 = _view.findViewById(R.id.linear4);
			final ProgressBar pb_player = _view.findViewById(R.id.pb_player);
			final androidx.appcompat.widget.AppCompatImageView iv_delete = _view.findViewById(R.id.iv_delete);
			final androidx.appcompat.widget.AppCompatTextView textview1 = _view.findViewById(R.id.textview1);
			final androidx.appcompat.widget.AppCompatImageView iv_info = _view.findViewById(R.id.iv_info);
			
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
			_view.setLayoutParams(_lp);
			if (_data.get((int)_position).containsKey("title")) {
				textview1.setText(maplist.get((int)_position).get("title").toString());
			}
			if (_data.get((int)_position).containsKey("thumbnail")) {
				Glide.with(getApplicationContext()).load(Uri.parse(maplist.get((int)_position).get("thumbnail").toString())).into(img);
			}
			description = maplist.get((int)_position).get("description").toString();
			pb_player.setVisibility(View.GONE);
			iv_delete.setVisibility(View.GONE);
			linear1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					intent.setClass(getApplicationContext(), ExoPlayerActivity.class);
					intent.putExtra("link", maplist.get((int)_position).get("url").toString());
					intent.putExtra("user_agent", "a");
					intent.putExtra("refere", "a");
					intent.putExtra("title", maplist.get((int)_position).get("title").toString());
					intent.putExtra("videoid", getIntent().getStringExtra("videoid"));
					intent.putExtra("img", maplist.get((int)_position).get("thumbnail").toString());
					intent.putExtra("description", maplist.get((int)_position).get("description").toString());
					intent.putExtra("type", "Series");
					intent.putExtra("seriesKey", seriesKey);
					intent.putExtra("category", category);
					startActivity(intent);
				}
			});
			iv_info.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					
					LayoutInflater inflater = LayoutInflater.from(_view.getContext());
					View customView = inflater.inflate(R.layout.snackbar_custom, null);
					
					
					View rootView = ((Activity) _view.getContext()).findViewById(android.R.id.content);
					Snackbar snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_SHORT);
					
					Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
					snackbarLayout.setBackgroundColor(Color.TRANSPARENT);
					snackbarLayout.removeAllViews();
					
					
					TextView textView = customView.findViewById(R.id.snackbar_text);
					textView.setText(description); 
					
					
					BlurView blurView = customView.findViewById(R.id.linear1);
					float radius = 20f;
					
					View decorView = ((Activity) _view.getContext()).getWindow().getDecorView();
					ViewGroup root = (ViewGroup) decorView.findViewById(android.R.id.content);
					Drawable windowBackground = decorView.getBackground();
					
					blurView.setupWith(root)
					.setFrameClearDrawable(windowBackground)
					.setBlurRadius(radius);
					
					
					
					snackbarLayout.addView(customView);
					snackbar.show();
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
