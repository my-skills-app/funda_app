package com.chill.with.examsaathi;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.media3.ui.*;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iambedant.text.*;
import com.makeramen.roundedimageview.*;
import com.sanojpunchihewa.glowbutton.*;
import eightbitlab.com.blurview.*;
import java.io.*;
import java.io.File;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.regex.*;
import jp.wasabeef.glide.transformations.*;
import me.mattak.outlined_text.*;
import org.json.*;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import com.airbnb.lottie.LottieAnimationView;

public class ProfileEditActivity extends AppCompatActivity {
	
	public final int REQ_CD_PROFILE = 101;
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	private FirebaseStorage _firebase_storage = FirebaseStorage.getInstance();
	
	private HashMap<String, Object> map = new HashMap<>();
	private String path = "";
	private String name = "";
	private String profileurl = "";
	private String msg = "";
	
	private LinearLayout linear1;
	private LinearLayout linear4;
	private RelativeLayout linear2;
	private EditText edittext1;
	private LinearLayout linear5;
	private TextView btn_update;
	private ImageFilterView back_btn;
	private TextView textview1;
	private RelativeLayout linear3;
	private ImageFilterView img_profile;
	private ImageView img_user;
	private ImageFilterView img_edit;
	
	private DatabaseReference users = _firebase.getReference("users");
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
	private Intent profile = new Intent(Intent.ACTION_GET_CONTENT);
	private StorageReference user = _firebase_storage.getReference("user");
	private OnCompleteListener<Uri> _user_upload_success_listener;
	private OnSuccessListener<FileDownloadTask.TaskSnapshot> _user_download_success_listener;
	private OnSuccessListener _user_delete_success_listener;
	private OnProgressListener _user_upload_progress_listener;
	private OnProgressListener _user_download_progress_listener;
	private OnFailureListener _user_failure_listener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.profile_edit);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		
		if (hasMediaReadPermission()) {
			initializeLogic();
		} else {
			requestMediaReadPermission();
		}
	}
	
	private static final int REQ_MEDIA_READ = 1000;

	private boolean hasMediaReadPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
					== PackageManager.PERMISSION_GRANTED;
		}
		return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED;
	}

	private void requestMediaReadPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			ActivityCompat.requestPermissions(
					this,
					new String[]{Manifest.permission.READ_MEDIA_IMAGES},
					REQ_MEDIA_READ);
		} else {
			ActivityCompat.requestPermissions(
					this,
					new String[]{
							Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE
					},
					REQ_MEDIA_READ);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQ_MEDIA_READ) {
			initializeLogic();
		}
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
		linear4 = findViewById(R.id.linear4);
		linear2 = findViewById(R.id.linear2);
		edittext1 = findViewById(R.id.edittext1);
		linear5 = findViewById(R.id.linear5);
		btn_update = findViewById(R.id.btn_update);
		back_btn = findViewById(R.id.back_btn);
		textview1 = findViewById(R.id.textview1);
		linear3 = findViewById(R.id.linear3);
		img_profile = findViewById(R.id.img_profile);
		img_user = findViewById(R.id.img_user);
		img_edit = findViewById(R.id.img_edit);
		auth = FirebaseAuth.getInstance();
		profile.setType("image/*");
		profile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		
		btn_update.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
				if (currentUser == null) {
					AppToast.show(ProfileEditActivity.this, R.string.sign_in_required);
					finish();
					return;
				}
				if (edittext1.getText().toString().equals("")) {
					_showDarkToast("Empty");
				} else if (name.equals(edittext1.getText().toString())) {
					_showDarkToast("Profile Updated!");
					finish();
				} else {
					_Custom_Loading(true);
					map = new HashMap<>();
					map.put("name", edittext1.getText().toString());

					users.child(currentUser.getUid())
							.updateChildren(map)
							.addOnCompleteListener(task -> {
								_Custom_Loading(false);
								if (task.isSuccessful()) {
									UserProfileCache.save(
											ProfileEditActivity.this,
											currentUser.getUid(),
											edittext1.getText().toString(),
											profileurl.isEmpty() ? null : profileurl);
									_showDarkToast("Profile Updated");
									finish();
								} else {
									_showDarkToast("Failed to update profile");
								}
							})
							.addOnFailureListener(e -> {
								_Custom_Loading(false);
								String msg = e.getMessage();
								if (msg != null && msg.contains("Permission denied")) {
									Toast.makeText(ProfileEditActivity.this, "Name must be 20 characters or less", Toast.LENGTH_LONG).show();
								} else {
									Toast.makeText(ProfileEditActivity.this, "Failed: " + msg, Toast.LENGTH_LONG).show();
								}
							});
				}
			}
		});
		
		back_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});
		
		if (img_edit != null) {
			img_edit.setVisibility(View.GONE);
			img_edit.setClickable(false);
		}

		attachUserProfileListener();
		
		_user_upload_progress_listener = new OnProgressListener<UploadTask.TaskSnapshot>() {
			@Override
			public void onProgress(UploadTask.TaskSnapshot _param1) {
				double _progressValue = (100.0 * _param1.getBytesTransferred()) / _param1.getTotalByteCount();
				
			}
		};
		
		_user_download_progress_listener = new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
			@Override
			public void onProgress(FileDownloadTask.TaskSnapshot _param1) {
				double _progressValue = (100.0 * _param1.getBytesTransferred()) / _param1.getTotalByteCount();
				
			}
		};
		
		_user_upload_success_listener = new OnCompleteListener<Uri>() {
			@Override
			public void onComplete(Task<Uri> _param1) {
				FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
				if (currentUser == null || !_param1.isSuccessful() || _param1.getResult() == null) {
					_Custom_Loading(false);
					AppToast.show(ProfileEditActivity.this, R.string.sign_in_required);
					return;
				}
				final String _downloadUrl = _param1.getResult().toString();
				
				map = new HashMap<>();
				map.put("profile", _downloadUrl);
				map.put("name", edittext1.getText().toString());
				
				users.child(currentUser.getUid())
				.updateChildren(map)
				.addOnCompleteListener(task -> {
					_Custom_Loading(false);
					if (task.isSuccessful()) {
						UserProfileCache.save(
								ProfileEditActivity.this,
								currentUser.getUid(),
								edittext1.getText().toString(),
								_downloadUrl);
						_showDarkToast("Profile Updated");
						finish();
					} else {
						_showDarkToast("Failed to update profile");
					}
				})
				
				.addOnFailureListener(e -> {
					String msg = e.getMessage();
					if (msg != null && msg.contains("Permission denied")) {
						Toast.makeText(ProfileEditActivity.this, "Name must be 20 characters or less", Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(ProfileEditActivity.this, "Failed: " + msg, Toast.LENGTH_LONG).show();
					}
				});
			}
		};
		
		_user_download_success_listener = new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
			@Override
			public void onSuccess(FileDownloadTask.TaskSnapshot _param1) {
				final long _totalByteCount = _param1.getTotalByteCount();
				
			}
		};
		
		_user_delete_success_listener = new OnSuccessListener() {
			@Override
			public void onSuccess(Object _param1) {
				
			}
		};
		
		_user_failure_listener = new OnFailureListener() {
			@Override
			public void onFailure(Exception _param1) {
				final String _message = _param1.getMessage();
				_Custom_Loading(false);
				_showDarkToast(_message);
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
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			AppToast.show(this, R.string.sign_in_required);
			finish();
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Window window = getWindow();
			
			window.setNavigationBarColor(Color.parseColor("#100a0e"));
			window.getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			);
			window.setStatusBarColor(Color.TRANSPARENT);
		}
		
		back_btn.setImageResource(R.drawable.ic_back);
	}

	private void attachUserProfileListener() {
		detachUserProfileListener();
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			return;
		}
		userProfileRef = users.child(currentUser.getUid());
		userProfileListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				applyUserSnapshot(snapshot);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				_Custom_Loading(false);
				AppToast.show(ProfileEditActivity.this, error.getMessage());
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
	protected void onDestroy() {
		detachUserProfileListener();
		super.onDestroy();
	}

	private void applyUserSnapshot(DataSnapshot snapshot) {
		if (snapshot == null) {
			return;
		}
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			return;
		}
		HashMap<String, Object> childValue = FirebaseSnapshotHelper.asMap(snapshot);
		if (childValue == null) {
			return;
		}
		Object uidObj = childValue.get("uid");
		if (uidObj == null || !uidObj.toString().equals(currentUser.getUid())) {
			return;
		}
		if (img_profile != null) {
			img_profile.setImageResource(R.drawable.ic_profile);
		}
		Object profileObj = childValue.get("profile");
		if (profileObj != null && !profileObj.toString().trim().isEmpty()) {
			profileurl = profileObj.toString();
		}
		Object nameObj = childValue.get("name");
		if (nameObj != null) {
			edittext1.setText(nameObj.toString());
			name = nameObj.toString();
		}
	}
	
	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		
		switch (_requestCode) {
			case REQ_CD_PROFILE:
			if (_resultCode == Activity.RESULT_OK) {
				ArrayList<String> _filePath = new ArrayList<>();
				if (_data != null) {
					if (_data.getClipData() != null) {
						for (int _index = 0; _index < _data.getClipData().getItemCount(); _index++) {
							ClipData.Item _item = _data.getClipData().getItemAt(_index);
							_filePath.add(FileUtil.convertUriToFilePath(getApplicationContext(), _item.getUri()));
						}
					}
					else {
						_filePath.add(FileUtil.convertUriToFilePath(getApplicationContext(), _data.getData()));
					}
				}
				path = _filePath.get((int)(0));
				img_profile.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(_filePath.get((int)(0)), 1024, 1024));
			}
			else {
				
			}
			break;
			default:
			break;
		}
	}
	
	
	@Override
	public void onBackPressed() {
		finish();
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
