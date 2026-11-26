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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.utils.widget.ImageFilterView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.media3.ui.*;
import com.bumptech.glide.Glide;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.NestedScrollView;


public class MovieListActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private String category = "";
	DatabaseReference dbRef;
	
	private ArrayList<HashMap<String, Object>> listmap = new ArrayList<>();
	
	private FrameLayout linear4;
	private LinearLayout linear6;
	private LinearLayout linear7;
	private BlurView blurView;
	private GridView gridview1;
	private ProgressBar progressbar1;
	private LinearLayout linear5;
	private ImageFilterView back_btn;
	private TextView textview1;
	
	private Intent i = new Intent();
	private DatabaseReference Action = _firebase.getReference("Action");
	private ChildEventListener _Action_child_listener;
	private DatabaseReference Adventure = _firebase.getReference("Adventure");
	private ChildEventListener _Adventure_child_listener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.movie_list);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear4 = findViewById(R.id.linear4);
		linear6 = findViewById(R.id.linear6);
		linear7 = findViewById(R.id.linear7);
		blurView = findViewById(R.id.blurView);
		gridview1 = findViewById(R.id.gridview1);
		progressbar1 = findViewById(R.id.progressbar1);
		linear5 = findViewById(R.id.linear5);
		back_btn = findViewById(R.id.back_btn);
		textview1 = findViewById(R.id.textview1);
		
		back_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});
		
		_Action_child_listener = new ChildEventListener() {
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
		Action.addChildEventListener(_Action_child_listener);
		
		_Adventure_child_listener = new ChildEventListener() {
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
		Adventure.addChildEventListener(_Adventure_child_listener);
	}
	
	private void initializeLogic() {
		float radius = 20f;
		
		View decorView = getWindow().getDecorView();
		ViewGroup rootView = (ViewGroup) decorView.findViewById(android.R.id.content);
		Drawable windowBackground = decorView.getBackground();
		
		blurView.setupWith(rootView)
		.setFrameClearDrawable(windowBackground)
		.setBlurRadius(radius);
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
		textview1.setText(getIntent().getStringExtra("categoryName"));
		category = getIntent().getStringExtra("categoryName");
		DatabaseReference dbRef = FirebaseDatabase.getInstance()
		.getReference("Categories")
		.child(getIntent().getStringExtra("categoryName"));
		dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot _dataSnapshot) {
				listmap = new ArrayList<>();
				try {
					for (DataSnapshot _data : _dataSnapshot.getChildren()) {
						HashMap<String, Object> _map = FirebaseSnapshotHelper.asMap(_data);
						listmap.add(_map);
					}
				} catch (Exception _e) {
					_e.printStackTrace();
				}
				gridview1.setAdapter(new Gridview1Adapter(listmap));
				gridview1.setNumColumns((int)3);
				gridview1.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
				_reverse(listmap);
			}
			@Override
			public void onCancelled(DatabaseError _databaseError) {
			}
		});
		gridview1.setVerticalSpacing((int)10);
		gridview1.setHorizontalSpacing((int)5);
		dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot _dataSnapshot) {
				listmap = new ArrayList<>();
				try {
					for (DataSnapshot _data : _dataSnapshot.getChildren()) {
						HashMap<String, Object> _map = FirebaseSnapshotHelper.asMap(_data);
						listmap.add(_map);
					}
				} catch (Exception _e) {
					_e.printStackTrace();
				}
				if (listmap.size() == 0) {
					linear7.setVisibility(View.VISIBLE);
					linear6.setVisibility(View.GONE);
				} else {
					linear7.setVisibility(View.GONE);
					linear6.setVisibility(View.VISIBLE);
				}
			}
			@Override
			public void onCancelled(DatabaseError _databaseError) {
			}
		});
	}
	
	
	@Override
	public void onBackPressed() {
		finish();
	}
	public void _reverse(final ArrayList<HashMap<String, Object>> _mapname) {
		Collections.reverse(_mapname);
	}
	
	public class Gridview1Adapter extends BaseAdapter {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Gridview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public int getCount() {
			return _data.size();
		}
		
		@Override
		public HashMap<String, Object> getItem(int _index) {
			return _data.get(_index);
		}
		
		@Override
		public long getItemId(int _index) {
			return _index;
		}
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater _inflater = getLayoutInflater();
			View _view = _v;
			if (_view == null) {
				_view = _inflater.inflate(R.layout.listitems, null);
			}
			
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final RelativeLayout RL1 = _view.findViewById(R.id.RL1);
			final RelativeLayout RL2 = _view.findViewById(R.id.RL2);
			final androidx.constraintlayout.utils.widget.ImageFilterView img = _view.findViewById(R.id.img);
			final View view1 = _view.findViewById(R.id.view1);
			final RelativeLayout RL3 = _view.findViewById(R.id.RL3);
			final TextView type = _view.findViewById(R.id.type);
			final View view2 = _view.findViewById(R.id.view2);
			
			if (_data.get((int)_position).containsKey("posterUrl")) {
				Glide.with(getApplicationContext()).load(Uri.parse(_data.get((int)_position).get("posterUrl").toString())).into(img);
			}
			view2.setVisibility(View.INVISIBLE);
			type.setVisibility(View.INVISIBLE);
			linear1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (_data.get((int)_position).get("type").toString().equals("Movie")) {
						i.setClass(getApplicationContext(), MovieviewActivity.class);
						i.putExtra("posterUrl", _data.get((int)_position).get("posterUrl").toString());
						i.putExtra("title", _data.get((int)_position).get("title").toString());
						i.putExtra("description", _data.get((int)_position).get("description").toString());
						i.putExtra("videoUrl", _data.get((int)_position).get("videoUrl").toString());
						i.putExtra("videoid", _data.get((int)_position).get("videoid").toString());
						i.putExtra("type", _data.get((int)_position).get("type").toString());
						i.putExtra("seriesKey", "null");
						startActivity(i);
					} else {
						i.setClass(getApplicationContext(), SeriesviewActivity.class);
						i.putExtra("posterUrl", _data.get((int)_position).get("posterUrl").toString());
						i.putExtra("title", _data.get((int)_position).get("title").toString());
						i.putExtra("description", _data.get((int)_position).get("description").toString());
						i.putExtra("videoid", _data.get((int)_position).get("videoid").toString());
						i.putExtra("type", _data.get((int)_position).get("type").toString());
						i.putExtra("seriesKey", _data.get((int)_position).get("videoid").toString());
						i.putExtra("category", _data.get((int)_position).get("category").toString());
						startActivity(i);
					}
				}
			});
			
			return _view;
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
