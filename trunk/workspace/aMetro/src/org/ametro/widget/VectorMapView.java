/*
 * http://code.google.com/p/ametro/
 * Transport map viewer for Android platform
 * Copyright (C) 2009-2010 Roman.Golovanov@gmail.com and other
 * respective project committers (see project home page)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.ametro.widget;

import java.util.Date;

import org.ametro.model.Model;
import org.ametro.render.RenderProgram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.ScrollView;
import android.widget.Scroller;

public class VectorMapView extends ScrollView {

	private Model mModel;
	private RenderProgram mRenderProgram;
	private Context mContext;

	public VectorMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initializeControls();
	}

	public VectorMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initializeControls();
	}

	public VectorMapView(Context context) {
		super(context);
		mContext = context;
		initializeControls();
	}

	protected void onDraw(Canvas canvas) {
		if(mInitialized){
			
			final int scrollX = mScrollX;
			final int scrollY = mScrollY;
			final int width = (int)( getViewWidth() / mScale );
			final int height = (int)( getViewHeight() / mScale );
			
			final int left = (int)(scrollX);
			final int top = (int)(scrollY);
			final int right = left + (int)((width));
			final int bottom = top + (int)((height));
			
			Rect viewport = new Rect(left,top,right, bottom);
			
			//Date time = new Date();
			mRenderProgram.invalidateVisible(viewport);
			canvas.save();
			canvas.scale(mScale, mScale);
			canvas.translate(-scrollX, -scrollY);
			mRenderProgram.draw(canvas);
			canvas.restore();
            //Log.i("aMetro", String.format("Rendering time is %sms", Long.toString((new Date().getTime() - time.getTime()))));
			
			super.onDraw(canvas);
		}
	}


	public void setModel(Model model) {
		if(model!=null){
			mModel = model;
			mRenderProgram = new RenderProgram(model);
			//mRenderProgram.setRenderFilter(RenderProgram.ONLY_TRANSPORT);
			mInitialized = true;
			calculateDimensions();
		}else{
			mInitialized = false;
			mRenderProgram = null;
			mModel = null;
		}
	}	

	protected int computeVerticalScrollOffset() {
		return  mInitialized ? mScrollY : 0;
	}

	protected int computeVerticalScrollRange() {
		return mInitialized ? mContentHeight : 0;
	}

	protected int computeHorizontalScrollOffset() {
		return  mInitialized ? mScrollX : 0;
	}

	protected int computeHorizontalScrollRange() {
		return mInitialized ? mContentWidth : 0;
	}


	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			mScrollX = mScroller.getCurrX();
			mScrollY = mScroller.getCurrY();

//			if(mScroller.isFinished()){
//				mRenderProgram.setRenderFilter(RenderProgram.ALL);
//			}
			
//            Log.i("aMetro", String.format("Scroll to %sx%s"
//            		,Integer.toString(mScrollX)
//            		,Integer.toString(mScrollY)
//            		));
			
            postInvalidate();
		}
	}
	
	public void setScrollCenter(int x, int y){
		setScrollCenter(new Point(x,y));
	}
	
	public void setScrollCenter(Point p) {
		mScrollX = (int)(p.x*mScale);
		mScrollY = (int)(p.y*mScale);
		invalidateScroll();
		postInvalidate();
	}

	public Point getScrollCenter(){
		final int screenX = mScrollX;
		final int screenY = mScrollY;
		return new Point( (int)(screenX/mScale), (int)(screenY/mScale) );
	}

	public void setScale(float scale){
		Point p = getScrollCenter();
		mScale = scale;
		calculateDimensions();
		setScrollCenter(p);
		postInvalidate();
	}
	
	public boolean onTouchEvent(MotionEvent event) {

		int action = event.getAction();
		float x = event.getX();
		float y = event.getY();
		long eventTime = event.getEventTime();
		if (x > getViewWidth() - 1) {
			x = getViewWidth() - 1;
		}
		if (y > getViewHeight() - 1) {
			y = getViewHeight() - 1;
		}

		int deltaX = (int) (mLastTouchX - x);
		int deltaY = (int) (mLastTouchY - y);

		switch(action){
		case MotionEvent.ACTION_DOWN:
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
				mTouchMode = TOUCH_DRAG_START_MODE;
			}else{
				mTouchMode = TOUCH_INIT_MODE;				
			}
			mTouchMode = TOUCH_INIT_MODE;
			mLastTouchX = x;
			mLastTouchY = y;
			mLastTouchTime = eventTime;
			mSnapScrollMode = SNAP_NONE;
			mVelocityTracker = VelocityTracker.obtain();
			break;
		case MotionEvent.ACTION_MOVE:
			if (mTouchMode == TOUCH_DONE_MODE) {
				// no dragging during scroll zoom animation
				break;
			}
			mVelocityTracker.addMovement(event);

			if (mTouchMode != TOUCH_DRAG_MODE) {

				// if it starts nearly horizontal or vertical, enforce it
				if(SNAP_ENABLED){
					int ax = Math.abs(deltaX);
					int ay = Math.abs(deltaY);
					if (ax > MAX_SLOPE_FOR_DIAG * ay) {
						mSnapScrollMode = SNAP_X;
						mSnapPositive = deltaX > 0;
					} else if (ay > MAX_SLOPE_FOR_DIAG * ax) {
						mSnapScrollMode = SNAP_Y;
						mSnapPositive = deltaY > 0;
					}
				}
				mTouchMode = TOUCH_DRAG_MODE;

			}

			// do pan
			int newScrollX = pinLocX(mScrollX + deltaX);
			deltaX = newScrollX - mScrollX;
			int newScrollY = pinLocY(mScrollY + deltaY);
			deltaY = newScrollY - mScrollY;
			boolean done = false;
			if (deltaX == 0 && deltaY == 0) {
				done = true;
			} else {
				if (mSnapScrollMode == SNAP_X || mSnapScrollMode == SNAP_Y) {
					int ax = Math.abs(deltaX);
					int ay = Math.abs(deltaY);
					if (mSnapScrollMode == SNAP_X) {
						// radical change means getting out of snap mode
						if (ay > MAX_SLOPE_FOR_DIAG * ax
								&& ay > MIN_BREAK_SNAP_CROSS_DISTANCE) {
							mSnapScrollMode = SNAP_NONE;
						}
						// reverse direction means lock in the snap mode
						if ((ax > MAX_SLOPE_FOR_DIAG * ay) &&
								((mSnapPositive && 
										deltaX < -mMinLockSnapReverseDistance)
										|| (!mSnapPositive &&
												deltaX > mMinLockSnapReverseDistance))) {
							mSnapScrollMode = SNAP_X_LOCK;
						}
					} else {
						// radical change means getting out of snap mode
						if ((ax > MAX_SLOPE_FOR_DIAG * ay)
								&& ax > MIN_BREAK_SNAP_CROSS_DISTANCE) {
							mSnapScrollMode = SNAP_NONE;
						}
						// reverse direction means lock in the snap mode
						if ((ay > MAX_SLOPE_FOR_DIAG * ax) &&
								((mSnapPositive && 
										deltaY < -mMinLockSnapReverseDistance)
										|| (!mSnapPositive && 
												deltaY > mMinLockSnapReverseDistance))) {
							mSnapScrollMode = SNAP_Y_LOCK;
						}
					}
				}

				if (mSnapScrollMode == SNAP_X
						|| mSnapScrollMode == SNAP_X_LOCK) {
					//scrollBy(deltaX, 0);
					internalScroll(deltaX,0);
					mLastTouchX = x;
				} else if (mSnapScrollMode == SNAP_Y
						|| mSnapScrollMode == SNAP_Y_LOCK) {
					//scrollBy(0, deltaY);
					internalScroll(0,deltaY);
					mLastTouchY = y;
				} else {
					//scrollBy(deltaX, deltaY);
					internalScroll(deltaX,deltaY);
					mLastTouchX = x;
					mLastTouchY = y;
				}
				mLastTouchTime = eventTime;
			}
			if (done) {
				// return false to indicate that we can't pan out of the
				// view space
				return false;
			}
			break;
		case MotionEvent.ACTION_UP:
			switch (mTouchMode) {
			case TOUCH_INIT_MODE: // tap
				mTouchMode = TOUCH_DONE_MODE;
				doShortPress();
				break;
			case TOUCH_DRAG_MODE:
				// if the user waits a while w/o moving before the
				// up, we don't want to do a fling
				if (eventTime - mLastTouchTime <= MIN_FLING_TIME) {
					mVelocityTracker.addMovement(event);
					doFling();
					break;
				}
				break;
			case TOUCH_DRAG_START_MODE:
			case TOUCH_DONE_MODE:
				// do nothing
				break;
			}
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			break;
		case MotionEvent.ACTION_CANCEL: 
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			mTouchMode = TOUCH_DONE_MODE;
			break;
		}
		return true;
	}

	private void doFling() {
		if (mVelocityTracker == null) {
			return;
		}
		int maxX = Math.max(mContentWidth - getWidth(), 0);
		int maxY = Math.max(mContentHeight - getHeight(), 0);

		mVelocityTracker.computeCurrentVelocity(1000);
		int vx = (int) mVelocityTracker.getXVelocity();
		int vy = (int) mVelocityTracker.getYVelocity();

		if(SNAP_ENABLED){
			if (mSnapScrollMode != SNAP_NONE) {
				if (mSnapScrollMode == SNAP_X || mSnapScrollMode == SNAP_X_LOCK) {
					vy = 0;
				} else {
					vx = 0;
				}
			}
		}
		vx = vx / 2;
		vy = vy / 2;

		//mRenderProgram.setRenderFilter(RenderProgram.ONLY_TRANSPORT);
		mScroller.fling(mScrollX, mScrollY, -vx, -vy, 0, maxX, 0, maxY);
		postInvalidate();
	}

	private void doShortPress() {
	}

	private void calculateDimensions(){
		mContentWidth = (int)(mModel.getWidth() * mScale);
		mContentHeight = (int)(mModel.getHeight() * mScale);
	}
		
	private void initializeControls() {
		setVerticalScrollBarEnabled(true);
		setHorizontalScrollBarEnabled(true);
		mScroller = new Scroller(mContext);
	}

	private void internalScroll(int dx, int dy){
		int x = mScrollX+dx;
		int y = mScrollY+dy;
		mScrollX = x;
		mScrollY = y;
		invalidateScroll();
		postInvalidate();
	}


	private void invalidateScroll() {
		int maxX = Math.max(mContentWidth - (int)(getWidth()*mScale), 0);
		int maxY = Math.max(mContentHeight - (int)(getHeight()*mScale), 0);
		mScrollX = Math.max(0, Math.min(maxX, mScrollX));
		mScrollY = Math.max(0,Math.min(maxY, mScrollY));
	} 	
	
	// Expects x in view coordinates
	private int pinLocX(int x) {
		return pinLoc(x, getViewWidth(), mContentWidth);
	}

	// Expects y in view coordinates
	private int pinLocY(int y) {
		return pinLoc(y, getViewHeight(), mContentHeight) ;
	}

	private static int pinLoc(int x, int viewMax, int docMax) {
		if (docMax < viewMax) {   // the doc has room on the sides for "blank"
			x = 0;
		} else if (x < 0) {
			x = 0;
		} else if (x + viewMax > docMax) {
			x = docMax - viewMax;
		}
		return x;
	}

	private int getViewWidth() {
		if (!isVerticalScrollBarEnabled() ) {
			return getWidth();
		} else {
			return Math.max(0, getWidth() - getVerticalScrollbarWidth());
		}
	}

	private int getViewHeight() {
		if (!isHorizontalScrollBarEnabled() ) {
			return getHeight();
		} else {
			return Math.max(0, getHeight() - getHorizontalScrollbarHeight());
		}
	}
	
	private int mContentWidth;
	private int mContentHeight;
	private boolean mInitialized;
	private float mScale = 1.0f;
	
	
	// adjustable parameters
	private static final boolean SNAP_ENABLED = false;
	private static final float MAX_SLOPE_FOR_DIAG = 1.5f;
	private static final int MIN_BREAK_SNAP_CROSS_DISTANCE = 20; //20
	private static final int MIN_FLING_TIME = 250; //250

	private int mScrollX = 0;
	private int mScrollY = 0;

	private float mLastTouchX;
	private float mLastTouchY;
	private long mLastTouchTime;
	private int mMinLockSnapReverseDistance;

	private int mTouchMode = TOUCH_DONE_MODE;
	private static final int TOUCH_INIT_MODE = 1;
	private static final int TOUCH_DRAG_START_MODE = 2;
	private static final int TOUCH_DRAG_MODE = 3;
	private static final int TOUCH_DONE_MODE = 7;

	private int mSnapScrollMode = SNAP_NONE;
	private static final int SNAP_NONE = 1;
	private static final int SNAP_X = 2;
	private static final int SNAP_Y = 3;
	private static final int SNAP_X_LOCK = 4;
	private static final int SNAP_Y_LOCK = 5;
	private boolean mSnapPositive;

	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;


}
