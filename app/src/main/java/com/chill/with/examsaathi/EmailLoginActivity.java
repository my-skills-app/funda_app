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
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import com.airbnb.lottie.LottieAnimationView;

public class EmailLoginActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private boolean password = false;
	private boolean password2 = false;
	private String backgroundColor = "";
	private HashMap<String, Object> map = new HashMap<>();
	private String uniquekey = "";
	private boolean doubleBackToExitPressedOnce = false;
	
	private LinearLayout linear1;
	private ImageView imageview5;
	private ScrollView vscroll2;
	private LinearLayout linear4;
	private LinearLayout signup;
	private LinearLayout signin;
	private TextView textview2;
	private EditText edittext1;
	private EditText edittext3;
	private LinearLayout linear6;
	private LinearLayout linear7;
	private TextView textview1;
	private TextView textview6;
	private EditText edittext2;
	private ImageView imageview3;
	private EditText edittext4;
	private TextView textview3;
	private EditText edittext5;
	private LinearLayout linear10;
	private TextView textview5;
	private TextView textview4;
	private TextView textview8;
	private EditText edittext7;
	private ImageView imageview4;
	
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
	private DatabaseReference users = _firebase.getReference("users");
	private Intent i = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.email_login);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
		imageview5 = findViewById(R.id.imageview5);
		vscroll2 = findViewById(R.id.vscroll2);
		linear4 = findViewById(R.id.linear4);
		signup = findViewById(R.id.signup);
		signin = findViewById(R.id.signin);
		textview2 = findViewById(R.id.textview2);
		edittext1 = findViewById(R.id.edittext1);
		edittext3 = findViewById(R.id.edittext3);
		linear6 = findViewById(R.id.linear6);
		linear7 = findViewById(R.id.linear7);
		textview1 = findViewById(R.id.textview1);
		textview6 = findViewById(R.id.textview6);
		edittext2 = findViewById(R.id.edittext2);
		imageview3 = findViewById(R.id.imageview3);
		edittext4 = findViewById(R.id.edittext4);
		textview3 = findViewById(R.id.textview3);
		edittext5 = findViewById(R.id.edittext5);
		linear10 = findViewById(R.id.linear10);
		textview5 = findViewById(R.id.textview5);
		textview4 = findViewById(R.id.textview4);
		textview8 = findViewById(R.id.textview8);
		edittext7 = findViewById(R.id.edittext7);
		imageview4 = findViewById(R.id.imageview4);
		auth = FirebaseAuth.getInstance();
		
		textview1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (edittext1.getText().toString().equals("")) {
					edittext1.setError("Empty");
				} else {
					if (edittext2.getText().toString().equals("")) {
						edittext2.setError("Empty");
					} else {
						if (edittext3.getText().toString().equals("")) {
							edittext3.setError("Empty");
						} else {
							if (edittext4.getText().toString().equals("")) {
								edittext4.setError("Empty");
							} else {
								if (edittext2.getText().toString().equals(edittext4.getText().toString())) {
									auth.createUserWithEmailAndPassword(edittext1.getText().toString(), edittext2.getText().toString()).addOnCompleteListener(EmailLoginActivity.this, _auth_create_user_listener);
									_Custom_Loading(true);
								} else {
									_showDarkToast("Password do not match!");
								}
							}
						}
					}
				}
			}
		});
		
		textview6.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				signup.setVisibility(View.GONE);
				signin.setVisibility(View.VISIBLE);
			}
		});
		
		imageview3.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (password) {
					password = false;
					imageview3.setImageResource(R.drawable.ic_hidden);
					edittext2.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
				} else {
					password = true;
					imageview3.setImageResource(R.drawable.ic_show);
					edittext2.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
				}
			}
		});
		
		textview4.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (edittext5.getText().toString().equals("")) {
					edittext5.setError("Empty");
				} else {
					if (edittext7.getText().toString().equals("")) {
						edittext7.setError("Empty");
					} else {
						auth.signInWithEmailAndPassword(edittext5.getText().toString(), edittext7.getText().toString()).addOnCompleteListener(EmailLoginActivity.this, _auth_sign_in_listener);
						_Custom_Loading(true);
					}
				}
			}
		});
		
		textview8.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				signin.setVisibility(View.GONE);
				signup.setVisibility(View.VISIBLE);
			}
		});
		
		imageview4.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (password2) {
					password2 = false;
					imageview4.setImageResource(R.drawable.ic_hidden);
					edittext7.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
				} else {
					password2 = true;
					imageview4.setImageResource(R.drawable.ic_show);
					edittext7.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
				}
			}
		});

		textview5.setOnClickListener(v -> AppToast.showComingSoon(EmailLoginActivity.this));
		
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
				if (_success) {
					map = new HashMap<>();
					map.put("email", edittext1.getText().toString());
					map.put("name", edittext3.getText().toString());
					map.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
					map.put("profile", "https://i.postimg.cc/DyR1xvDV/smiling-young-man-illustration-1308-173524.webp");
					
					users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
					.updateChildren(map)
					.addOnCompleteListener(task -> {
						_Custom_Loading(false);
						if (task.isSuccessful()) {
							String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
							UserProfileCache.save(
									EmailLoginActivity.this,
									uid,
									edittext3.getText().toString(),
									"https://i.postimg.cc/DyR1xvDV/smiling-young-man-illustration-1308-173524.webp");
							Intent i = new Intent();
							i.setClass(getApplicationContext(), MainActivity.class);
							startActivity(i);
							finish();
						} else {
							_showDarkToast("Failed to upload data");
						}
					});
				} else {
					SketchwareUtil.showMessage(getApplicationContext(), _errorMessage);
					_Custom_Loading(false);
				}
			}
		};
		
		_auth_sign_in_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				_Custom_Loading(false);
				if (_success) {
					PushNotificationHelper.registerToken(getApplicationContext());
					PushBroadcastListener.start(getApplicationContext());
					i.setClass(getApplicationContext(), MainActivity.class);
					startActivity(i);
					finish();
				} else {
					SketchwareUtil.showMessage(getApplicationContext(), _errorMessage);
				}
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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			window.setNavigationBarColor(getColor(R.color.colorPrimary));
			window.getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
		imageview3.setImageResource(R.drawable.ic_show);
		edittext2.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
		imageview4.setImageResource(R.drawable.ic_show);
		edittext7.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
		vscroll2.setVerticalScrollBarEnabled(false);
		signup.setVisibility(View.GONE);
	}
	
	
	@Override
	public void onBackPressed() {
		if (signup.getVisibility() == View.VISIBLE) {
			signup.setVisibility(View.GONE);
			signin.setVisibility(View.VISIBLE);
		} else {
			i.setClass(getApplicationContext(), SignInActivity.class);
			startActivity(i);
			finish();
		}
	}
	public void _setRipple1(final View _view) {
		ViewRippleHelper.applySelectableBackground(this, _view);
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
	
	
	public void _showDarkToast(final String _message) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.custom_toast, null);
		
		TextView text = layout.findViewById(R.id.toast_text);
		text.setText(_message);
		
		// Fade animation
		Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
		layout.startAnimation(fadeIn);
		
		Toast toast = new Toast(getApplicationContext());
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setView(layout);
		toast.setGravity(Gravity.BOTTOM, 0, 100); // Adjust position
		toast.show();
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
