/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2011 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viewpagerindicator;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.GpsStatus.NmeaListener;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This widget implements the dynamic action bar tab behavior that can change
 * across different configurations or circumstances.
 */
public class TabPageIndicator extends HorizontalScrollView implements PageIndicator
{
	/** Title text used when no title is provided by the adapter. */
	private static final CharSequence EMPTY_TITLE = "";

	/**
	 * Interface for a callback when the selected tab has been reselected.
	 */
	public interface OnTabReselectedListener
	{
		/**
		 * Callback when the selected tab has been reselected.
		 * 
		 * @param position
		 *            Position of the current center item.
		 */
		void onTabReselected(int position);
	}

	private Runnable mTabSelector;

	private final OnClickListener mTabClickListener = new OnClickListener() {
		public void onClick(View view)
		{
			TabView2 tabView = (TabView2) view;
			final int oldSelected = mViewPager.getCurrentItem();
			final int newSelected = tabView.getIndex();
			mViewPager.setCurrentItem(newSelected);
			if (oldSelected == newSelected && mTabReselectedListener != null)
			{
				mTabReselectedListener.onTabReselected(newSelected);
			}
		}
	};

	private final IcsLinearLayout mTabLayout;

	private ViewPager mViewPager;
	private ViewPager.OnPageChangeListener mListener;

	private int mMaxTabWidth;
	private int mSelectedTabIndex;

	private OnTabReselectedListener mTabReselectedListener;

	public TabPageIndicator(Context context) {
		this(context, null);
	}

	public TabPageIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		setHorizontalScrollBarEnabled(false);
		mTabLayout = new IcsLinearLayout(context, R.attr.vpiTabPageIndicatorStyle);
		//mTabLayout.setBackgroundColor(Color.BLACK);
		addView(mTabLayout, new ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT));
	}

	public void setOnTabReselectedListener(OnTabReselectedListener listener)
	{
		mTabReselectedListener = listener;
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final boolean lockedExpanded = widthMode == MeasureSpec.EXACTLY;
		setFillViewport(lockedExpanded);

		final int childCount = mTabLayout.getChildCount();
		if (childCount > 1 && (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST))
		{
			if (childCount > 2)
			{
				mMaxTabWidth = (int) (MeasureSpec.getSize(widthMeasureSpec) * 0.4f);
			} else
			{
				mMaxTabWidth = MeasureSpec.getSize(widthMeasureSpec) / 2;
			}
		} else
		{
			mMaxTabWidth = -1;
		}

		final int oldWidth = getMeasuredWidth();
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int newWidth = getMeasuredWidth();

		if (lockedExpanded && oldWidth != newWidth)
		{
			setCurrentItem(mSelectedTabIndex);
		}
	}

	public void animateToTab(final int position)
	{
		final TabView2 tabView = (TabView2) mTabLayout.getChildAt(position);
		tabView.setTabCount(tabView.getTabCount()+1);
		if(tabView.getTabCount() == 2)
		{
			tabView.removeTextView();
		}
		
		if (mTabSelector != null)
		{
			removeCallbacks(mTabSelector);
		}
		mTabSelector = new Runnable() {
			public void run()
			{
				final int scrollPos = tabView.getLeft() - (getWidth() - tabView.getWidth()) / 2;
				smoothScrollTo(scrollPos, 0);
				mTabSelector = null;
			}
		};
		post(mTabSelector);
	}

	@Override
	public void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		if (mTabSelector != null)
		{
			// Re-post the selector we saved
			post(mTabSelector);
		}
	}

	@Override
	public void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		if (mTabSelector != null)
		{
			removeCallbacks(mTabSelector);
		}
	}

	private void addTab(int index, CharSequence text, int iconResId)
	{
		final TabView2 tabView = new TabView2(getContext(), text);
		tabView.mIndex = index;
		tabView.setFocusable(true);
		tabView.setOnClickListener(mTabClickListener);
		tabView.setText();

		mTabLayout.addView(tabView, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
	}

	@Override
	public void onPageScrollStateChanged(int arg0)
	{
		if (mListener != null)
		{
			mListener.onPageScrollStateChanged(arg0);
		}
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2)
	{
		if (mListener != null)
		{
			mListener.onPageScrolled(arg0, arg1, arg2);
		}
	}

	@Override
	public void onPageSelected(int arg0)
	{
		setCurrentItem(arg0);
		if (mListener != null)
		{
			mListener.onPageSelected(arg0);
		}
	}

	@Override
	public void setViewPager(ViewPager view)
	{
		if (mViewPager == view)
		{
			return;
		}
		if (mViewPager != null)
		{
			mViewPager.setOnPageChangeListener(null);
		}
		final PagerAdapter adapter = view.getAdapter();
		if (adapter == null)
		{
			throw new IllegalStateException("ViewPager does not have adapter instance.");
		}
		mViewPager = view;
		view.setOnPageChangeListener(this);
		notifyDataSetChanged();
	}

	public void notifyDataSetChanged()
	{
		mTabLayout.removeAllViews();
		PagerAdapter adapter = mViewPager.getAdapter();
		IconPagerAdapter iconAdapter = null;
		if (adapter instanceof IconPagerAdapter)
		{
			iconAdapter = (IconPagerAdapter) adapter;
		}
		final int count = adapter.getCount();
		for (int i = 0; i < count; i++)
		{
			CharSequence title = adapter.getPageTitle(i);
			if (title == null)
			{
				title = EMPTY_TITLE;
			}
			int iconResId = 0;
			if (iconAdapter != null)
			{
				iconResId = iconAdapter.getIconResId(i);
			}
			addTab(i, title, iconResId);
		}
		if (mSelectedTabIndex > count)
		{
			mSelectedTabIndex = count - 1;
		}
		setCurrentItem(mSelectedTabIndex);
		requestLayout();
	}

	@Override
	public void setViewPager(ViewPager view, int initialPosition)
	{
		setViewPager(view);
		setCurrentItem(initialPosition);
	}

	@Override
	public void setCurrentItem(int item)
	{
		if (mViewPager == null)
		{
			throw new IllegalStateException("ViewPager has not been bound.");
		}
		mSelectedTabIndex = item;
		mViewPager.setCurrentItem(item);

		final int tabCount = mTabLayout.getChildCount();
		for (int i = 0; i < tabCount; i++)
		{
			final TabView2 child = (TabView2) mTabLayout.getChildAt(i);
			// child.
			final boolean isSelected = (i == item);
			child.setSelected(isSelected);
			// child.removeTextView();
			if (isSelected)
			{
				child.setBackgroundDrawable(child.getContext().getResources().getDrawable(R.drawable.select_background));
				animateToTab(item);
			} else
			{
				child.setBackgroundDrawable(child.getContext().getResources().getDrawable(R.drawable.unselect_background));
			}
		}
	}
	
	/*
	public void setCurrentChoise(int item)
	{
		if (mViewPager == null)
		{
			throw new IllegalStateException("ViewPager has not been bound.");
		}
		mSelectedTabIndex = item;
		final int tabCount = mTabLayout.getChildCount();
		for (int i = 0; i < tabCount; i++)
		{
			final TabView2 child = (TabView2) mTabLayout.getChildAt(i);
			// child.
			final boolean isSelected = (i == item);
			child.setSelected(isSelected);
			// child.removeTextView();
			if (isSelected)
			{
				child.setBackgroundDrawable(child.getContext().getResources().getDrawable(R.drawable.select_background));
				animateToTab(item);
			} else
			{
				child.setBackgroundDrawable(child.getContext().getResources().getDrawable(R.drawable.unselect_background));
			}
		}
	}
	*/

	@Override
	public void setOnPageChangeListener(OnPageChangeListener listener)
	{
		mListener = listener;
	}
	
	/*
	private class TabView extends TextView
	{
		private int mIndex;

		public TabView(Context context) {
			super(context, null, R.attr.vpiTabPageIndicatorStyle);
			// LinearLayout l = getLayoutParams()
			setGravity(Gravity.CENTER);
			setMinWidth((int) convertDpToPixel(80, context));
			setBackgroundDrawable(context.getResources().getDrawable(R.drawable.unselect_background));
			setTextColor(Color.BLACK);
			setPadding(10, 0, 10, 0);

		}

		@Override
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			// Re-measure if we went beyond our maximum size.
			if (mMaxTabWidth > 0 && getMeasuredWidth() > mMaxTabWidth)
			{
				super.onMeasure(MeasureSpec.makeMeasureSpec(mMaxTabWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
			}
		}

		public int getIndex()
		{
			return mIndex;
		}
	}
	*/

	private class TabView2 extends LinearLayout
	{
		private int mIndex;
		private String text = "";
		private String num = "";
		private TextView textView;
		private TextView numView;
		private Context context;
		private int tabCount;

		public TabView2(Context context, CharSequence text) {
			super(context);
			setOrientation(LinearLayout.HORIZONTAL);
			this.context = context;
			this.text = text.toString();
			this.tabCount = 0;
			setGravity(Gravity.CENTER);
			int w = 70;
			if (context instanceof Activity)
			{
				DisplayMetrics metrics = new DisplayMetrics();
				((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
				w =(int) (metrics.widthPixels / 4.5);
			} else
			{
				w = (int) convertDpToPixel(w, context);
			}
			setMinimumWidth(w);
			setBackgroundDrawable(context.getResources().getDrawable(R.drawable.unselect_background));
			//setPadding(10, 20, 10, 20);
		}

		public int getIndex()
		{
			return mIndex;
		}
		
		
		public int getTabCount()
		{
			return tabCount;
		}

		public void setTabCount(int tabCount)
		{
			this.tabCount = tabCount;
		}

		public void setText()
		{
			String[] s = text.split("#");
			if (s.length > 0)
				this.text = s[0];
			if (s.length == 2)
			{
				this.num = s[1];
			}
			textView = new TextView(context);
			textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			textView.setText(this.text);
			textView.setTextColor(Color.parseColor("#333333"));
			addView(textView);
			if (this.num != null && !this.num.equals(""))
			{
				numView = new TextView(context);
				LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.gravity = Gravity.TOP;
				numView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				numView.setText(num);
				numView.setGravity(Gravity.CENTER);
				numView.setTextColor(Color.WHITE);
				numView.setBackgroundResource(R.drawable.circle);
				addView(numView);
			}

		}

		public void removeTextView()
		{
			if (this.numView != null)
			{
				this.removeView(numView);
			}
		}

	}

	public static float convertDpToPixel(float dp, Context context)
	{
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float px = dp * (metrics.densityDpi / 160f);
		return px;
	}
}
