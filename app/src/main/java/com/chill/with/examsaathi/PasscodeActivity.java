package com.chill.with.examsaathi;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.media3.ui.*;
import com.chill.with.examsaathi.PasscodeView;
import com.google.firebase.FirebaseApp;
import com.iambedant.text.*;
import com.makeramen.roundedimageview.*;
import com.sanojpunchihewa.glowbutton.*;
import eightbitlab.com.blurview.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import jp.wasabeef.glide.transformations.*;
import me.mattak.outlined_text.*;
import org.json.*;
import com.chill.with.examsaathi.PasscodeView;

public class PasscodeActivity extends AppCompatActivity {
	
	SharedPreferences prefs;
	private String savedPasscode = "";
	
	private PasscodeView passcodeView;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.passcode);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		passcodeView = findViewById(R.id.passcodeView);
	}
	
	private void initializeLogic() {
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
		savedPasscode = prefs.getString("passcode", null);
		
		if (savedPasscode == null) {
			// First time → ask to SET passcode
			passcodeView.setPasscodeType(PasscodeView.PasscodeViewType.TYPE_SET_PASSCODE);
		} else {
			// Second time → check passcode (must set local passcode!)
			passcodeView.setPasscodeType(PasscodeView.PasscodeViewType.TYPE_CHECK_PASSCODE);
			passcodeView.setLocalPasscode(savedPasscode); // ⬅️ REQUIRED to prevent crash
		}
		
		passcodeView.setListener(new PasscodeView.PasscodeViewListener() {
			@Override
			public void onFail(String wrongNumber) {
				Toast.makeText(PasscodeActivity.this, "Wrong Passcode!", Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void onSuccess(String number) {
				if (savedPasscode == null) {
					// Save passcode as raw string
					prefs.edit().putString("passcode", number).apply();
					Toast.makeText(PasscodeActivity.this, "Passcode Set!", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(PasscodeActivity.this, "Access Granted!", Toast.LENGTH_SHORT).show();
				}
				goToNext();
			}
		});
	}
	
	private void goToNext() {
		Intent main = new Intent(PasscodeActivity.this, MainActivity.class);
		main.putExtra(MainActivity.EXTRA_REQUEST_PRO_PROMO, true);
		startActivity(main);
		finish();
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
