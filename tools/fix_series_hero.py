from pathlib import Path

p = Path(r"C:\Users\apple\AndroidStudioProjects\BhagawatGita-aap-\app\src\main\res\layout\activity_series_details.xml")
text = p.read_text(encoding="utf-8")

start_marker = '            <FrameLayout\n                android:layout_width="match_parent"\n                android:layout_height="300dp">'
# Cut until just before minHeight of the sheet LinearLayout (after removing -18dp open)
sheet_min = '                android:minHeight="420dp"\n                android:orientation="vertical"\n                android:paddingTop="14dp"\n                android:paddingBottom="28dp">'

start = text.index(start_marker)
sheet_idx = text.index(sheet_min)

# Find the LinearLayout that owns this minHeight — go back to its start
before = text.rfind("<LinearLayout", 0, sheet_idx)
# We want to replace from start_marker through the sheet LinearLayout opening attrs,
# and keep from minHeight onward, but prefix a clean LinearLayout open without -18dp.

replacement = '''            <!-- Compact banner only — CTAs live below so they never get clipped -->
            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="220dp">

                <ImageView
                    android:id="@+id/imgSeriesBanner"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:scaleType="centerCrop"
                    tools:ignore="ContentDescription"
                    tools:src="@color/view_bg_color" />

                <View
                    android:id="@+id/viewSeriesScrim"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:background="@drawable/bg_syllabus_hero_scrim" />

                <LinearLayout
                    android:id="@+id/layoutSeriesTopBar"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:paddingStart="12dp"
                    android:paddingTop="8dp"
                    android:paddingEnd="12dp"
                    android:paddingBottom="8dp">

                    <ImageView
                        android:id="@+id/btnBack"
                        android:layout_width="40dp"
                        android:layout_height="40dp"
                        android:background="@drawable/bg_toolbar_back_btn"
                        android:contentDescription="@string/back"
                        android:padding="11dp"
                        android:scaleType="centerInside"
                        android:src="@drawable/arrow_back_24"
                        app:tint="@color/white" />

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:gravity="center"
                        android:orientation="horizontal">

                        <LinearLayout
                            android:id="@+id/chipExam"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:background="@drawable/bg_syllabus_exam_chip"
                            android:gravity="center_vertical"
                            android:orientation="horizontal"
                            android:paddingStart="8dp"
                            android:paddingTop="5dp"
                            android:paddingEnd="12dp"
                            android:paddingBottom="5dp">

                            <ImageView
                                android:id="@+id/imgExamThumb"
                                android:layout_width="18dp"
                                android:layout_height="18dp"
                                android:contentDescription="@null"
                                android:scaleType="centerCrop"
                                android:src="@drawable/default_image" />

                            <TextView
                                android:id="@+id/txtExamChip"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:layout_marginStart="6dp"
                                android:ellipsize="end"
                                android:fontFamily="@font/outfit_medium"
                                android:includeFontPadding="false"
                                android:maxLines="1"
                                android:textColor="@color/white"
                                android:textSize="12sp"
                                tools:text="RRB ALP" />
                        </LinearLayout>
                    </LinearLayout>

                    <ImageView
                        android:id="@+id/btnShare"
                        android:layout_width="40dp"
                        android:layout_height="40dp"
                        android:background="@drawable/bg_toolbar_back_btn"
                        android:contentDescription="@string/share"
                        android:padding="10dp"
                        android:scaleType="centerInside"
                        android:src="@drawable/share_24"
                        app:tint="@color/white" />
                </LinearLayout>
            </FrameLayout>

            <LinearLayout
                android:id="@+id/layoutHeroContent"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="@color/colorPrimary"
                android:orientation="vertical"
                android:paddingTop="12dp"
                android:paddingBottom="16dp">

                <TextView
                    android:id="@+id/txtBreadcrumb"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:ellipsize="end"
                    android:fontFamily="@font/outfit_regular"
                    android:includeFontPadding="false"
                    android:maxLines="1"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:textColor="@color/text_color_40"
                    android:textSize="12sp"
                    tools:text="Ancient India > Stone to Civilisation…" />

                <TextView
                    android:id="@+id/txtSeriesTitle"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:fontFamily="@font/outfit_semi_bold"
                    android:includeFontPadding="false"
                    android:lineSpacingExtra="2dp"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:textColor="@color/white"
                    android:textSize="22sp"
                    tools:text="Stone to Civilisation: India's First Steps" />

                <HorizontalScrollView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:clipToPadding="false"
                    android:overScrollMode="never"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:scrollbars="none">

                    <LinearLayout
                        android:id="@+id/layoutContentChips"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:gravity="center_vertical"
                        android:orientation="horizontal" />
                </HorizontalScrollView>

                <TextView
                    android:id="@+id/txtProgressLabel"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="14dp"
                    android:fontFamily="@font/outfit_medium"
                    android:includeFontPadding="false"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp"
                    android:textColor="@color/text_color"
                    android:textSize="12sp"
                    tools:text="0/36 topics completed" />

                <ProgressBar
                    android:id="@+id/progressSeries"
                    style="?android:attr/progressBarStyleHorizontal"
                    android:layout_width="match_parent"
                    android:layout_height="5dp"
                    android:layout_marginStart="16dp"
                    android:layout_marginTop="8dp"
                    android:layout_marginEnd="16dp"
                    android:max="100"
                    android:progress="0"
                    android:progressDrawable="@drawable/progress_series_hero" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="16dp"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:paddingStart="16dp"
                    android:paddingEnd="16dp">

                    <TextView
                        android:id="@+id/btnWatchSeries"
                        android:layout_width="0dp"
                        android:layout_height="48dp"
                        android:layout_weight="1"
                        android:background="@drawable/bg_series_watch_btn"
                        android:fontFamily="@font/outfit_semi_bold"
                        android:gravity="center"
                        android:includeFontPadding="false"
                        android:text="@string/prep_watch_video_series"
                        android:textColor="@color/white"
                        android:textSize="15sp" />

                    <ImageView
                        android:id="@+id/btnBookmark"
                        android:layout_width="48dp"
                        android:layout_height="48dp"
                        android:layout_marginStart="10dp"
                        android:background="@drawable/bg_series_bookmark_btn"
                        android:contentDescription="@string/prep_bookmark"
                        android:padding="12dp"
                        android:src="@drawable/ic_bookmark_not"
                        app:tint="@color/white" />
                </LinearLayout>
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:background="@drawable/bg_series_sheet"
'''

out = text[:start] + replacement + sheet_min + text[sheet_idx + len(sheet_min):]
p.write_text(out, encoding="utf-8")
print("OK")
print("300dp left:", 'layout_height="300dp"' in out)
print("-18dp left:", 'marginTop="-18dp"' in out and 'bg_series_sheet' in out[out.find('marginTop="-18dp"')-80:out.find('marginTop="-18dp"')+80] if 'marginTop="-18dp"' in out else False)
print("hero content:", "layoutHeroContent" in out)
print("220dp:", 'layout_height="220dp"' in out)
