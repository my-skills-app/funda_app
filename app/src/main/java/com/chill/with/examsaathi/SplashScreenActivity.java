package com.chill.with.examsaathi;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Context;
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
import android.os.Vibrator;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
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
import androidx.media3.ui.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iambedant.text.*;
import com.makeramen.roundedimageview.*;
import com.sanojpunchihewa.glowbutton.*;
import eightbitlab.com.blurview.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import jp.wasabeef.glide.transformations.*;
import me.mattak.outlined_text.*;
import org.json.*;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;

public class SplashScreenActivity extends AppCompatActivity {

	public static final String EXTRA_SKIP_UPDATE_CHECK = "skip_update_check";
	
	private Timer _timer = new Timer();
	
	private String appVersion = "";
	private HashMap<String, Object> res = new HashMap<>();
	
	private LinearLayout linear1;
	private LinearLayout linear2;
	private LinearLayout linear5;
	private ImageView imageview1;
	private LinearLayout linear4;
	private LinearLayout linear3;
	private TextView textview1;
	private TextView textview2;
	
	private Intent i = new Intent();
	private TimerTask t;
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
	private SharedPreferences s;
	private RequestNetwork net;
	private RequestNetwork.RequestListener _net_request_listener;
	private Vibrator v;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.splash_screen);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
		linear2 = findViewById(R.id.linear2);
		linear5 = findViewById(R.id.linear5);
		imageview1 = findViewById(R.id.imageview1);
		linear4 = findViewById(R.id.linear4);
		linear3 = findViewById(R.id.linear3);
		textview1 = findViewById(R.id.textview1);
		textview2 = findViewById(R.id.textview2);
		auth = FirebaseAuth.getInstance();
		s = getSharedPreferences("s", Activity.MODE_PRIVATE);
		net = new RequestNetwork(this);
		v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		_net_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				res = new Gson().fromJson(_response, new TypeToken<HashMap<String, Object>>(){}.getType());
				if (!res.get("version").toString().equals(s.getString("version", ""))) {
					i.setClass(getApplicationContext(), UpdateActivityActivity.class);
					startActivity(i);
				} else {
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Intent i = new Intent();
									
									if (_isConnected()) {
										if (FirebaseAuth.getInstance().getCurrentUser() != null) {
											// User is logged in
											SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
											String passcode = prefs.getString("passcode", null);
											
											if (passcode != null) {
												// Passcode is set → go to Passcode screen
												i.setClass(getApplicationContext(), PasscodeActivity.class); // your PasscodeActivity
											} else {
												i.setClass(getApplicationContext(), MainActivity.class);
												i.putExtra(MainActivity.EXTRA_REQUEST_PRO_PROMO, true);
											}
											
										} else {
											// Not signed in
											i.setClass(getApplicationContext(), SignInActivity.class);
										}
									} else {
										// No Internet
										i.setClass(getApplicationContext(), NoInternetActivity.class);
									}
									v.vibrate((long)(100));
									startActivity(i);
									finish();
								}
							});
						}
					}, 2000);
				}
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
	
	private void initializeLogic() {
		DownloadCount.send(this);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.setNavigationBarColor(getColor(R.color.colorPrimary));
			window.getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			appVersion = pInfo.versionName != null ? pInfo.versionName : "1.0";
			s.edit().putString("version", appVersion).commit();
		} catch (PackageManager.NameNotFoundException e) {
			appVersion = "1.0";
			textview1.setText(appVersion);
		}

		if (getIntent().getBooleanExtra(EXTRA_SKIP_UPDATE_CHECK, false)) {
			scheduleNavigation(2000);
			return;
		}

		AppUpdateHelper.readOnce(config -> runOnUiThread(() -> {
			if (config.shouldPromptUpdate(appVersion)) {
				Intent update = new Intent(getApplicationContext(), UpdateActivityActivity.class);
				update.putExtra(UpdateActivityActivity.EXTRA_TITLE,
						config.title.isEmpty() ? getString(R.string.app_update_default_title) : config.title);
				update.putExtra(UpdateActivityActivity.EXTRA_MESSAGE,
						config.message.isEmpty() ? getString(R.string.app_update_default_message) : config.message);
				update.putExtra(UpdateActivityActivity.EXTRA_STORE_URL, config.storeUrl);
				update.putExtra(UpdateActivityActivity.EXTRA_FORCE, config.forceUpdate);
				startActivity(update);
				finish();
			} else {
				scheduleNavigation(2000);
			}
		}));
	}

	private void scheduleNavigation(long delayMs) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(() -> navigateAfterSplash());
			}
		}, delayMs);
	}

	private void navigateAfterSplash() {
		Intent i = new Intent();
		if (_isConnected()) {
			// Local Prep emulator path: static session flag (backend holds real tokens).
			if (PrepLocalSession.isLoggedIn(getApplicationContext())) {
				if (!PrepCourseStore.isOnboardingDone(getApplicationContext())) {
					i.setClass(getApplicationContext(), ExamOnboardingActivity.class);
				} else {
					i.setClass(getApplicationContext(), MainActivity.class);
				}
			} else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
				SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
				String passcode = prefs.getString("passcode", null);
				if (passcode != null) {
					i.setClass(getApplicationContext(), PasscodeActivity.class);
				} else if (!PrepCourseStore.isOnboardingDone(getApplicationContext())) {
					i.setClass(getApplicationContext(), ExamOnboardingActivity.class);
				} else {
					i.setClass(getApplicationContext(), MainActivity.class);
					i.putExtra(MainActivity.EXTRA_REQUEST_PRO_PROMO, true);
				}
			} else {
				i.setClass(getApplicationContext(), SignInActivity.class);
			}
		} else {
			i.setClass(getApplicationContext(), NoInternetActivity.class);
		}
		v.vibrate(100L);
		startActivity(i);
		finish();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (_timer != null) {
			_timer.cancel();
		}
	}
	
	public boolean _isConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			return activeNetwork != null && activeNetwork.isConnected();
		}
		return false;
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
