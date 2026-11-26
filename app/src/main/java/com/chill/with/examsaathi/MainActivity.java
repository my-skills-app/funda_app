package com.chill.with.examsaathi;

import com.chill.with.examsaathi.SplashScreenActivity;
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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.media3.ui.*;
import com.chill.with.examsaathi.CustomViewPager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.iambedant.text.*;
import com.makeramen.roundedimageview.*;
import com.sanojpunchihewa.glowbutton.*;
import eightbitlab.com.blurview.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.regex.*;
import jp.wasabeef.glide.transformations.*;
import me.mattak.outlined_text.*;
import org.json.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import java.util.ArrayList;


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

public class MainActivity extends AppCompatActivity {

	public static final String EXTRA_REQUEST_PRO_PROMO = "request_pro_promo";

	private boolean proPromoPending;
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	FragmentManager fragmentManager;
	int lastVisitedPage = 0;
	private boolean isBackPressed = false;
	ArrayList<Integer> pageHistory = new ArrayList<>();;
	private boolean isBackNavigation = false;
	private boolean doubleBackToExitPressedOnce = false;
	private boolean wasInBackground = false;
	int REQUEST_CODE_PERMISSIONS = 1001;
	
	private CustomViewPager viewpager1;
	private LinearLayout linear9;
	private LinearLayout linear1;
	private LinearLayout linear2;
	private LinearLayout linear3;
	private LinearLayout linear4;
	private LinearLayout linear5;
	private LinearLayout linear6;
	private LinearLayout linear7;
	private LinearLayout linear8;
	private ImageView imageview1;
	private TextView textview1;
	private ImageView imageview2;
	private TextView textview2;
	private ImageView imageview3;
	private TextView textview3;
	private ImageView imageview4;
	private TextView textview4;
	
	private FragmentFragmentAdapter fragment;
	private DatabaseReference video = _firebase.getReference("video");
	private ChildEventListener _video_child_listener;
	private Intent i = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		proPromoPending = getIntent().getBooleanExtra(EXTRA_REQUEST_PRO_PROMO, false);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
		PushNotificationHelper.init(this);
	}
	
	private void initialize(Bundle _savedInstanceState) {
		viewpager1 = findViewById(R.id.viewpager1);
		linear9 = findViewById(R.id.linear9);
		linear1 = findViewById(R.id.linear1);
		linear2 = findViewById(R.id.linear2);
		linear3 = findViewById(R.id.linear3);
		linear4 = findViewById(R.id.linear4);
		linear5 = findViewById(R.id.linear5);
		linear6 = findViewById(R.id.linear6);
		linear7 = findViewById(R.id.linear7);
		linear8 = findViewById(R.id.linear8);
		imageview1 = findViewById(R.id.imageview1);
		textview1 = findViewById(R.id.textview1);
		imageview2 = findViewById(R.id.imageview2);
		textview2 = findViewById(R.id.textview2);
		imageview3 = findViewById(R.id.imageview3);
		textview3 = findViewById(R.id.textview3);
		imageview4 = findViewById(R.id.imageview4);
		textview4 = findViewById(R.id.textview4);
		fragment = new FragmentFragmentAdapter(getApplicationContext(), getSupportFragmentManager());
		
		linear2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				viewpager1.setCurrentItem(0, false);
			}
		});
		
		linear4.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				viewpager1.setCurrentItem(1, false);
			}
		});
		
		linear6.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				viewpager1.setCurrentItem(2, false);
			}
		});
		
		linear8.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				viewpager1.setCurrentItem(3, false);
			}
		});
		
		_video_child_listener = new ChildEventListener() {
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
		video.addChildEventListener(_video_child_listener);
	}
	
	private void initializeLogic() {
		_checkMaintenance();
		_ICC(imageview1, "#FF4433", "#9e9e9e");
		_ICC(imageview2, "#9e9e9e", "#9e9e9e");
		_ICC(imageview3, "#9e9e9e", "#9e9e9e");
		_ICC(imageview4, "#9e9e9e", "#9e9e9e");
		fragment.setTabCount(4);
		viewpager1.setAdapter(fragment);
		viewpager1.setBackgroundColor(0xFF060908);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			
			window.setNavigationBarColor(Color.parseColor("#100a0e"));
			window.getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
		isBackNavigation = false;
		pageHistory.add(viewpager1.getCurrentItem());
		viewpager1.setOffscreenPageLimit(3);
		viewpager1.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				// Optional
			}
			
			@Override
			public void onPageSelected(int _position) {
				
				
				if (!isBackNavigation) {
					pageHistory.add(_position);
				} else {
					isBackNavigation = false;
				}
				if (_position == 0) {
					textview1.setTextColor(0xFFFF4433);
					textview2.setTextColor(0xFF9E9E9E);
					textview3.setTextColor(0xFF9E9E9E);
					textview4.setTextColor(0xFF9E9E9E);
					_ICC(imageview1, "#FF4433", "#9e9e9e");
					_ICC(imageview2, "#9e9e9e", "#9e9e9e");
					_ICC(imageview3, "#9e9e9e", "#9e9e9e");
					_ICC(imageview4, "#9e9e9e", "#9e9e9e");
				}
				if (_position == 1) {
					_ICC(imageview2, "#FF4433", "#9e9e9e");
					_ICC(imageview1, "#9e9e9e", "#9e9e9e");
					_ICC(imageview3, "#9e9e9e", "#9e9e9e");
					_ICC(imageview4, "#9e9e9e", "#9e9e9e");
					textview2.setTextColor(0xFFFF4433);
					textview1.setTextColor(0xFF9E9E9E);
					textview3.setTextColor(0xFF9E9E9E);
					textview4.setTextColor(0xFF9E9E9E);
				}
				if (_position == 2) {
					_ICC(imageview3, "#FF4433", "#9e9e9e");
					_ICC(imageview1, "#9e9e9e", "#9e9e9e");
					_ICC(imageview2, "#9e9e9e", "#9e9e9e");
					_ICC(imageview4, "#9e9e9e", "#9e9e9e");
					textview3.setTextColor(0xFFFF4433);
					textview1.setTextColor(0xFF9E9E9E);
					textview2.setTextColor(0xFF9E9E9E);
					textview4.setTextColor(0xFF9E9E9E);
				}
				if (_position == 3) {
					_ICC(imageview4, "#FF4433", "#9e9e9e");
					_ICC(imageview1, "#9e9e9e", "#9e9e9e");
					_ICC(imageview2, "#9e9e9e", "#9e9e9e");
					_ICC(imageview3, "#9e9e9e", "#9e9e9e");
					textview4.setTextColor(0xFFFF4433);
					textview1.setTextColor(0xFF9E9E9E);
					textview2.setTextColor(0xFF9E9E9E);
					textview3.setTextColor(0xFF9E9E9E);
				}
			}
			
			@Override
			public void onPageScrollStateChanged(int state) {
				// Optional
			}
		});
		viewpager1.setPageTransformer(false, null);
	}
	
	public class FragmentFragmentAdapter extends FragmentStatePagerAdapter {
		// This class is deprecated, you should migrate to ViewPager2:
		// https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2
		Context context;
		int tabCount;
		
		public FragmentFragmentAdapter(Context context, FragmentManager manager) {
			super(manager);
			this.context = context;
		}
		
		public void setTabCount(int tabCount) {
			this.tabCount = tabCount;
		}
		
		@Override
		public int getCount() {
			return tabCount;
		}
		
		@Override
		public CharSequence getPageTitle(int _position) {
			return "";
		}
		
		
		@Override
		public Fragment getItem(int _position) {
			if (_position == 0) {
				return new HomeFragmentActivity();
			}
			if (_position == 1) {
				return new SearchFragmentActivity();
			}
			if (_position == 2) {
				return new LivetvFragmentActivity();
			}
			if (_position == 3) {
				return new WatchlistFragmentActivity();
			}
			return new Fragment();
		}
		
	}
	
	
	@Override
	public void onBackPressed() {
		int currentItem = viewpager1.getCurrentItem();
		
		if (currentItem == 0) {
			if (doubleBackToExitPressedOnce) {
				finish();
				return;
			}
			
			
			doubleBackToExitPressedOnce = true;
			_showDarkToast("Press back again to exit");
			
			
			new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
		} else {
			if (pageHistory.size() > 1) {
				
				pageHistory.remove(pageHistory.size() - 1);
				
				int previousPage = pageHistory.get(pageHistory.size() - 1);
				isBackNavigation = true;
				viewpager1.setCurrentItem(previousPage, false); 
			}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		_UtilsDestroy();
	}
	public void _ICC(final ImageView _img, final String _c1, final String _c2) {
		_img.setImageTintList(new android.content.res.ColorStateList(new int[][] {{-android.R.attr.state_pressed},{android.R.attr.state_pressed}},new int[]{Color.parseColor(_c1), Color.parseColor(_c2)}));
	}
	
	
	public void _showDarkToast(final String _message) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.custom_toast, null);
		
		TextView text = layout.findViewById(R.id.toast_text);
		text.setText(_message);
		
		
		Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
		layout.startAnimation(fadeIn);
		
		Toast toast = new Toast(getApplicationContext());
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setView(layout);
		toast.setGravity(Gravity.BOTTOM, 0, 100); 
		toast.show();
	}
	
	
	public void _checkMaintenance() {
		Utils.checkMaintenance(this);
	}
	
	
	public void _UtilsDestroy() {
		Utils.removeListener();
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

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		if (intent.getBooleanExtra(EXTRA_REQUEST_PRO_PROMO, false)) {
			proPromoPending = true;
		}
	}

	/** True once per launcher open (Splash/Passcode → Main). */
	public boolean consumeProPromoPending() {
		if (!proPromoPending) {
			return false;
		}
		proPromoPending = false;
		return true;
	}
}
