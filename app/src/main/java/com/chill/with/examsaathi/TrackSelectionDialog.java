package com.chill.with.examsaathi;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.TrackSelectionView;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@OptIn(markerClass = UnstableApi.class)
public final class TrackSelectionDialog extends DialogFragment {

  
  public interface TrackSelectionListener {

    
    void onTracksSelected(TrackSelectionParameters trackSelectionParameters);
  }

  public static final ImmutableList<Integer> SUPPORTED_TRACK_TYPES =
      ImmutableList.of(C.TRACK_TYPE_VIDEO);

  private final SparseArray<TrackSelectionViewFragment> tabFragments;
  private final ArrayList<Integer> tabTrackTypes;

  private int titleId;
  private DialogInterface.OnClickListener onClickListener;
  private DialogInterface.OnDismissListener onDismissListener;

  
  public static boolean willHaveContent(Player player) {
    return willHaveContent(player.getCurrentTracks());
  }

  
  public static boolean willHaveContent(Tracks tracks) {
    for (Tracks.Group trackGroup : tracks.getGroups()) {
      if (SUPPORTED_TRACK_TYPES.contains(trackGroup.getType())) {
        return true;
      }
    }
    return false;
  }

  
  public static TrackSelectionDialog createForPlayer(
      Player player, DialogInterface.OnDismissListener onDismissListener) {
    return createForTracksAndParameters(
        R.string.track_selection_title,
        player.getCurrentTracks(),
        player.getTrackSelectionParameters(),
         true,
         false,
        player::setTrackSelectionParameters,
        onDismissListener);
  }

  
  public static TrackSelectionDialog createForTracksAndParameters(
      int titleId,
      Tracks tracks,
      TrackSelectionParameters trackSelectionParameters,
      boolean allowAdaptiveSelections,
      boolean allowMultipleOverrides,
      TrackSelectionListener trackSelectionListener,
      DialogInterface.OnDismissListener onDismissListener) {
    TrackSelectionDialog trackSelectionDialog = new TrackSelectionDialog();
    trackSelectionDialog.init(
        tracks,
        trackSelectionParameters,
        titleId,
        allowAdaptiveSelections,
        allowMultipleOverrides,
         (dialog, which) -> {
          TrackSelectionParameters.Builder builder = trackSelectionParameters.buildUpon();
          for (int i = 0; i < SUPPORTED_TRACK_TYPES.size(); i++) {
            int trackType = SUPPORTED_TRACK_TYPES.get(i);
            builder.setTrackTypeDisabled(trackType, trackSelectionDialog.getIsDisabled(trackType));
            builder.clearOverridesOfType(trackType);
            Map<TrackGroup, TrackSelectionOverride> overrides =
                trackSelectionDialog.getOverrides(trackType);
            for (TrackSelectionOverride override : overrides.values()) {
              builder.addOverride(override);
            }
          }
          trackSelectionListener.onTracksSelected(builder.build());
        },
        onDismissListener);
    return trackSelectionDialog;
  }

  public TrackSelectionDialog() {
    tabFragments = new SparseArray<>();
    tabTrackTypes = new ArrayList<>();
    
    setRetainInstance(true);
  }

  private void init(
      Tracks tracks,
      TrackSelectionParameters trackSelectionParameters,
      int titleId,
      boolean allowAdaptiveSelections,
      boolean allowMultipleOverrides,
      DialogInterface.OnClickListener onClickListener,
      DialogInterface.OnDismissListener onDismissListener) {
    this.titleId = titleId;
    this.onClickListener = onClickListener;
    this.onDismissListener = onDismissListener;

    for (int i = 0; i < SUPPORTED_TRACK_TYPES.size(); i++) {
      @C.TrackType int trackType = SUPPORTED_TRACK_TYPES.get(i);
      ArrayList<Tracks.Group> trackGroups = new ArrayList<>();
      for (Tracks.Group trackGroup : tracks.getGroups()) {
        if (trackGroup.getType() == trackType) {
          trackGroups.add(trackGroup);
        }
      }
      if (!trackGroups.isEmpty()) {
        TrackSelectionViewFragment tabFragment = new TrackSelectionViewFragment();
        tabFragment.init(
            trackGroups,
            trackSelectionParameters.disabledTrackTypes.contains(trackType),
            trackSelectionParameters.overrides,
            allowAdaptiveSelections,
            allowMultipleOverrides);
        tabFragments.put(trackType, tabFragment);
        tabTrackTypes.add(trackType);
      }
    }
  }

  
  public boolean getIsDisabled(int trackType) {
    TrackSelectionViewFragment trackView = tabFragments.get(trackType);
    return trackView != null && trackView.isDisabled;
  }

  
  public Map<TrackGroup, TrackSelectionOverride> getOverrides(int trackType) {
    TrackSelectionViewFragment trackView = tabFragments.get(trackType);
    return trackView == null ? Collections.emptyMap() : trackView.overrides;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    
    AppCompatDialog dialog =
        new AppCompatDialog(getActivity(), R.style.DarkDialogTheme);
    dialog.setTitle(titleId);
    return dialog;
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    super.onDismiss(dialog);
    onDismissListener.onDismiss(dialog);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false);
    TabLayout tabLayout = dialogView.findViewById(R.id.track_selection_dialog_tab_layout);
    ViewPager viewPager = dialogView.findViewById(R.id.track_selection_dialog_view_pager);
    Button cancelButton = dialogView.findViewById(R.id.track_selection_dialog_cancel_button);
    Button okButton = dialogView.findViewById(R.id.track_selection_dialog_ok_button);
    viewPager.setAdapter(new FragmentAdapter(getChildFragmentManager()));
    tabLayout.setupWithViewPager(viewPager);
    tabLayout.setVisibility(tabFragments.size() > 1 ? View.VISIBLE : View.GONE);
    cancelButton.setOnClickListener(view -> dismiss());
    okButton.setOnClickListener(
        view -> {
          onClickListener.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
          dismiss();
        });
    return dialogView;
  }

  private static String getTrackTypeString(Resources resources, @C.TrackType int trackType) {
    switch (trackType) {
      case C.TRACK_TYPE_VIDEO:
        return "Video";
      case C.TRACK_TYPE_AUDIO:
        return resources.getString(androidx.media3.ui.R.string.exo_track_selection_title_audio);
      case C.TRACK_TYPE_TEXT:
        return "Subtitles";
      default:
        throw new IllegalArgumentException();
    }
  }

  private final class FragmentAdapter extends FragmentPagerAdapter {

    public FragmentAdapter(FragmentManager fragmentManager) {
      super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    public Fragment getItem(int position) {
      return tabFragments.get(tabTrackTypes.get(position));
    }

    @Override
    public int getCount() {
      return tabTrackTypes.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return getTrackTypeString(getResources(), tabTrackTypes.get(position));
    }
  }

  
  public static final class TrackSelectionViewFragment extends Fragment
      implements TrackSelectionView.TrackSelectionListener {

    private List<Tracks.Group> trackGroups;
    private boolean allowAdaptiveSelections;
    private boolean allowMultipleOverrides;

     boolean isDisabled;
     Map<TrackGroup, TrackSelectionOverride> overrides;

    public TrackSelectionViewFragment() {
      
      setRetainInstance(true);
    }

    public void init(
        List<Tracks.Group> trackGroups,
        boolean isDisabled,
        Map<TrackGroup, TrackSelectionOverride> overrides,
        boolean allowAdaptiveSelections,
        boolean allowMultipleOverrides) {
      this.trackGroups = trackGroups;
      this.isDisabled = isDisabled;
      this.allowAdaptiveSelections = allowAdaptiveSelections;
      this.allowMultipleOverrides = allowMultipleOverrides;
      
      this.overrides =
          new HashMap<>(
              TrackSelectionView.filterOverrides(overrides, trackGroups, allowMultipleOverrides));
    }

    @Override
    public View onCreateView(
        LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
      View rootView =
          inflater.inflate(
              androidx.media3.ui.R.layout.exo_track_selection_dialog, container, /* attachToRoot= */ false);
      TrackSelectionView trackSelectionView = rootView.findViewById(androidx.media3.ui.R.id.exo_track_selection_view);
      trackSelectionView.setShowDisableOption(true);
      trackSelectionView.setAllowMultipleOverrides(allowMultipleOverrides);
      trackSelectionView.setAllowAdaptiveSelections(allowAdaptiveSelections);
      trackSelectionView.init(
          trackGroups,
          isDisabled,
          overrides,
           null,
           this);
      return rootView;
    }

    @Override
    public void onTrackSelectionChanged(
        boolean isDisabled, Map<TrackGroup, TrackSelectionOverride> overrides) {
      this.isDisabled = isDisabled;
      this.overrides = overrides;
    }
  }
}