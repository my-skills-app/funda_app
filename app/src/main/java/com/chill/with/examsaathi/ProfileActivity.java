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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.media3.ui.*;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.iambedant.text.*;
import com.makeramen.roundedimageview.*;
import com.sanojpunchihewa.glowbutton.*;
import de.hdodenhof.circleimageview.*;
import eightbitlab.com.blurview.*;
import eightbitlab.com.blurview.BlurView;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.regex.*;
import jp.wasabeef.glide.transformations.*;
import me.mattak.outlined_text.*;
import org.json.*;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.appcompat.widget.SwitchCompat;
import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;

import eightbitlab.com.blurview.RenderScriptBlur;

public class ProfileActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private HashMap<String, Object> map = new HashMap<>();
	private String image = "";
	SharedPreferences prefs;
	
	private RelativeLayout rl1;
	private LinearLayout linear22;
	private BlurView blurview;
	private LinearLayout linear1;
	private ScrollView vscroll1;
	private LinearLayout linear2;
	private LinearLayout linear3;
	private ImageView back_btn;
	private TextView textview1;
	private LinearLayout linear4;
	private TextView name;
	private CircleImageView circleimageview1;
	private ImageView img_edit;
	private LinearLayout linear5;
	private LinearLayout lout_pro;
	private LinearLayout lout_notification;
	private LinearLayout lout_send_push;
	private LinearLayout lout_manage_subscription;
	private ValueEventListener premiumConfigListener;
	private DatabaseReference premiumConfigRef;
	private LinearLayout linear7;
	private LinearLayout linear9;
	private LinearLayout linear11;
	private LinearLayout linear13;
	private LinearLayout linear15;
	private LinearLayout linear17;
	private LinearLayout linear19;
	private ImageView imageview1;
	private TextView textview2;
	private TextView textview3;
	private ImageView imageview2;
	private TextView textview4;
	private LinearLayout linear6;
	private SwitchCompat passcodeSwitch;
	private ImageView imageview3;
	private TextView textview5;
	private LinearLayout linear8;
	private ImageView imageview4;
	private TextView textview6;
	private LinearLayout linear10;
	private ImageView imageview5;
	private TextView textview7;
	private LinearLayout linear12;
	private ImageView imageview6;
	private TextView textview8;
	private LinearLayout linear14;
	private ImageView imageview7;
	private TextView textview9;
	private LinearLayout linear16;
	private ImageView imageview8;
	private TextView textview10;
	private LinearLayout linear18;
	private ImageView imageview9;
	private TextView textview11;
	private LinearLayout linear20;
	
	private DatabaseReference userProfileRef;
	private ValueEventListener userProfileListener;
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
	private Intent i = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.profile);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		setupWindowInsets();
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		rl1 = findViewById(R.id.rl1);
		linear22 = findViewById(R.id.linear22);
		blurview = findViewById(R.id.blurview);
		linear1 = findViewById(R.id.linear1);
		vscroll1 = findViewById(R.id.vscroll1);
		linear2 = findViewById(R.id.linear2);
		linear3 = findViewById(R.id.linear3);
		back_btn = findViewById(R.id.back_btn);
		textview1 = findViewById(R.id.textview1);
		linear4 = findViewById(R.id.linear4);
		name = findViewById(R.id.name);
		circleimageview1 = findViewById(R.id.circleimageview1);
		img_edit = findViewById(R.id.img_edit);
		linear5 = findViewById(R.id.linear5);
		lout_pro = findViewById(R.id.lout_pro);
		lout_notification = findViewById(R.id.lout_notification);
		lout_send_push = findViewById(R.id.lout_send_push);
		lout_manage_subscription = findViewById(R.id.lout_manage_subscription);
		linear7 = findViewById(R.id.linear7);
		linear9 = findViewById(R.id.linear9);
		linear11 = findViewById(R.id.linear11);
		linear13 = findViewById(R.id.linear13);
		linear15 = findViewById(R.id.linear15);
		linear17 = findViewById(R.id.linear17);
		linear19 = findViewById(R.id.linear19);
		imageview1 = findViewById(R.id.imageview1);
		textview2 = findViewById(R.id.textview2);
		textview3 = findViewById(R.id.textview3);
		imageview2 = findViewById(R.id.imageview2);
		textview4 = findViewById(R.id.textview4);
		linear6 = findViewById(R.id.linear6);
		passcodeSwitch = findViewById(R.id.passcodeSwitch);
		imageview3 = findViewById(R.id.imageview3);
		textview5 = findViewById(R.id.textview5);
		linear8 = findViewById(R.id.linear8);
		imageview4 = findViewById(R.id.imageview4);
		textview6 = findViewById(R.id.textview6);
		linear10 = findViewById(R.id.linear10);
		imageview5 = findViewById(R.id.imageview5);
		textview7 = findViewById(R.id.textview7);
		linear12 = findViewById(R.id.linear12);
		imageview6 = findViewById(R.id.imageview6);
		textview8 = findViewById(R.id.textview8);
		linear14 = findViewById(R.id.linear14);
		imageview7 = findViewById(R.id.imageview7);
		textview9 = findViewById(R.id.textview9);
		linear16 = findViewById(R.id.linear16);
		imageview8 = findViewById(R.id.imageview8);
		textview10 = findViewById(R.id.textview10);
		linear18 = findViewById(R.id.linear18);
		imageview9 = findViewById(R.id.imageview9);
		textview11 = findViewById(R.id.textview11);
		linear20 = findViewById(R.id.linear20);
		auth = FirebaseAuth.getInstance();
		
		back_btn.setOnClickListener(v -> onBackPressed());

		circleimageview1.setOnClickListener(v -> openEditProfile());

		img_edit.setOnClickListener(v -> openEditProfile());

		lout_pro.setOnClickListener(v -> startActivity(new Intent(this, SubscriptionProActivity.class)));
		lout_send_push.setOnClickListener(v -> {
			if (PushAdminHelper.isCurrentUserPushAdmin()) {
				startActivity(new Intent(this, SendNotificationActivity.class));
			}
		});
		lout_manage_subscription.setOnClickListener(v -> {
			if (PushAdminHelper.isCurrentUserPushAdmin()) {
				startActivity(new Intent(this, ManageSubscriptionActivity.class));
			}
		});
		updateAdminMenuVisibility();
		attachPremiumConfigListener();
		linear7.setOnClickListener(v -> AppToast.showComingSoon(this));
		linear9.setOnClickListener(v -> AppToast.showComingSoon(this));
		linear11.setOnClickListener(v -> AppToast.showComingSoon(this));
		linear13.setOnClickListener(v -> AppToast.showComingSoon(this));
		linear15.setOnClickListener(v -> AppToast.showComingSoon(this));

		linear17.setOnClickListener(v -> showLogoutDialog());

		linear19.setOnClickListener(v -> AppToast.showComingSoon(this));

		attachUserProfileListener();
		
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

	private void attachUserProfileListener() {
		detachUserProfileListener();
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			return;
		}
		userProfileRef = _firebase.getReference("users").child(currentUser.getUid());
		userProfileListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				bindUserProfile(FirebaseSnapshotHelper.asMap(snapshot));
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				AppToast.show(ProfileActivity.this, error.getMessage());
			}
		};
		userProfileRef.addValueEventListener(userProfileListener);
	}

	private void detachUserProfileListener() {
		if (userProfileRef != null && userProfileListener != null) {
			userProfileRef.removeEventListener(userProfileListener);
		}
		userProfileRef = null;
		userProfileListener = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		attachUserProfileListener();
		updateAdminMenuVisibility();
	}

	private void attachPremiumConfigListener() {
		if (premiumConfigRef != null && premiumConfigListener != null) {
			return;
		}
		premiumConfigRef = FirebaseDatabase.getInstance().getReference("subscription_config");
		premiumConfigListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				boolean enabled = SubscriptionConfigHelper.parseEnabled(snapshot.child("premiumEnabled"));
				runOnUiThread(() -> {
					if (lout_pro != null) {
						lout_pro.setVisibility(enabled ? View.VISIBLE : View.GONE);
					}
				});
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		};
		premiumConfigRef.addValueEventListener(premiumConfigListener);
	}

	private void updateAdminMenuVisibility() {
		boolean admin = PushAdminHelper.isCurrentUserPushAdmin();
		if (lout_send_push != null) {
			lout_send_push.setVisibility(admin ? View.VISIBLE : View.GONE);
		}
		if (lout_manage_subscription != null) {
			lout_manage_subscription.setVisibility(admin ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	protected void onDestroy() {
		detachUserProfileListener();
		if (premiumConfigRef != null && premiumConfigListener != null) {
			premiumConfigRef.removeEventListener(premiumConfigListener);
		}
		super.onDestroy();
	}

	private void bindUserProfile(HashMap<String, Object> childValue) {
		if (childValue == null) {
			return;
		}
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			return;
		}
		Object uidObj = childValue.get("uid");
		if (uidObj == null || !uidObj.toString().equals(currentUser.getUid())) {
			return;
		}
		Object personName = childValue.get("name");
		if (personName != null) {
			name.setText(personName.toString());
		}
		Object profileUrl = childValue.get("profile");
		if (profileUrl != null && !profileUrl.toString().trim().isEmpty()) {
			Glide.with(ProfileActivity.this)
					.load(profileUrl.toString())
					.placeholder(R.drawable.ic_profile)
					.error(R.drawable.ic_profile)
					.into(circleimageview1);
			image = profileUrl.toString();
		} else {
			circleimageview1.setImageResource(R.drawable.ic_profile);
		}
	}

	private void openEditProfile() {
		if (FirebaseAuth.getInstance().getCurrentUser() == null) {
			AppToast.show(this, R.string.sign_in_required);
			return;
		}
		startActivity(new Intent(this, ProfileEditActivity.class));
	}

	private void showLogoutDialog() {
		if (FirebaseAuth.getInstance().getCurrentUser() == null) {
			AppToast.show(this, R.string.sign_in_required);
			return;
		}
		blurview.setVisibility(View.VISIBLE);
		AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
		View view = getLayoutInflater().inflate(R.layout.item_popup_log_out, null);
		AlertDialog dialog = builder.setView(view).create();
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		}
		LinearLayout btnCancel = view.findViewById(R.id.btn_cancel);
		TextView btnLogout = view.findViewById(R.id.btn_log_out);
		btnCancel.setOnClickListener(cancelView -> {
			dialog.dismiss();
			blurview.setVisibility(View.GONE);
		});
		btnLogout.setOnClickListener(logoutView -> {
			dialog.dismiss();
			blurview.setVisibility(View.GONE);
			performLogout();
		});
		dialog.setOnCancelListener(dialogInterface -> blurview.setVisibility(View.GONE));
		dialog.show();
	}

	private void performLogout() {
		UserProfileCache.clear(this);
		AdminSecretsLoader.clearCache();
		PushBroadcastListener.stop();
		FirebaseAuth.getInstance().signOut();
		AppToast.show(this, R.string.logged_out_success);
		Intent intent = new Intent(this, SignInActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}
	
	private void setupWindowInsets() {
		View root = findViewById(R.id.profileRoot);
		View toolbar = findViewById(R.id.linear2);
		if (root == null || toolbar == null) {
			return;
		}
		ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
			int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
			int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
			toolbar.setPadding(
					toolbar.getPaddingLeft(),
					top + dp(8),
					toolbar.getPaddingRight(),
					toolbar.getPaddingBottom());
			v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
			return insets;
		});
		ViewCompat.requestApplyInsets(root);
	}

	private int dp(int value) {
		return (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
	}

	private void initializeLogic() {
		float radius = 10f;
		
		View decorView = getWindow().getDecorView();
		ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
		Drawable windowBackground = decorView.getBackground();
		
		blurview.setupWith(rootView)
		.setFrameClearDrawable(windowBackground)
		.setBlurRadius(radius);
		circleimageview1.setImageResource(R.drawable.ic_profile);
		imageview1.setImageResource(R.drawable.ic_premium);
		imageview3.setImageResource(R.drawable.ic_download_box);
		imageview4.setImageResource(R.drawable.ic_language);
		imageview5.setImageResource(R.drawable.ic_privacy);
		imageview6.setImageResource(R.drawable.ic_tearms);
		imageview7.setImageResource(R.drawable.ic_rate);
		imageview8.setImageResource(R.drawable.ic_logout);
		imageview9.setImageResource(R.drawable.ic_delete);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			
			window.setNavigationBarColor(Color.parseColor("#100a0e"));
			window.getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
		prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
		
		// Check if passcode is already set
		String savedPasscode = prefs.getString("passcode", null);
		passcodeSwitch.setChecked(savedPasscode != null);
		
		passcodeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					// Re-check in real time if passcode is set
					String currentPasscode = prefs.getString("passcode", null);
					if (currentPasscode == null) {
						// Open MainActivity to set new passcode
						Intent intent = new Intent(ProfileActivity.this, PasscodeActivity.class);
						startActivity(intent);
						finish(); // Optional: close this activity
					}
				} else {
					// Remove passcode
					prefs.edit().remove("passcode").apply();
					Toast.makeText(ProfileActivity.this, "Passcode removed", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	
	@Override
	public void onBackPressed() {
		finish();
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
	
	
	public void _Custom_Loading(final boolean _ifShow) {
		if (_ifShow) {
			if (coreprog == null){
				coreprog = new ProgressDialog(this);
				coreprog.setCancelable(true);
				coreprog.setCanceledOnTouchOutside(true);
				
				coreprog.requestWindowFeature(Window.FEATURE_NO_TITLE);  coreprog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
				
			}
			coreprog.setMessage(null);
			coreprog.show();
			View _view = getLayoutInflater().inflate(R.layout.custom_dialog2, null);
			LinearLayout linear_base = (LinearLayout) _view.findViewById(R.id.linear_base);
			CircleImageView profileImage = _view.findViewById(R.id.circleimageview2);
			android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
			gd.setColor(Color.parseColor("#00000000"));
			gd.setCornerRadius(0);
			linear_base.setBackground(gd);
			coreprog.setContentView(_view);
			Glide.with(getApplicationContext()).load(Uri.parse(image)).into(profileImage);
		} else {
			if (coreprog != null){
				coreprog.dismiss();
			}
		}
	}
	private ProgressDialog coreprog;
	{
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
