package com.volokh.danylo.diagonallayoutmanager;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * Created by danylo.volokh on 6/16/16.
 */
public class DiagonalLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = DiagonalLayoutManager.class.getSimpleName();
    private static final boolean SHOW_LOGS = false;

    private final int mStepSize;
    private int mLastVisiblePosition;

    private int mFirstVisiblePosition;

    public DiagonalLayoutManager(Context context) {
        mStepSize = context.getResources().getDimensionPixelSize(R.dimen.step_size);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //We have nothing to show for an empty data set but clear any existing views
        int itemCount = getItemCount();
        if (itemCount == 0) {
            removeAllViews();
            return;
        }

        mLastVisiblePosition = 0;
        mFirstVisiblePosition = 0;

        // TODO: add this to a step
        // TODO: add padding
        int viewTop = getPaddingTop();
        int viewLeft = getPaddingLeft();

        if (SHOW_LOGS) Log.v(TAG, "onLayoutChildren");

        boolean isLastLaidOutView;
        do {
            if (SHOW_LOGS)
                Log.v(TAG, "onLayoutChildren, do, mLastVisiblePosition " + mLastVisiblePosition);

            View view = recycler.getViewForPosition(mLastVisiblePosition);
            addView(view);
            measureChildWithMargins(view, 0, 0);

            int measuredWidth = getDecoratedMeasuredWidth(view);
            int measuredHeight = getDecoratedMeasuredHeight(view);

            if (SHOW_LOGS) {
                Log.v(TAG, "onLayoutChildren, do, viewLeft " + viewLeft);
                Log.v(TAG, "onLayoutChildren, do, viewTop " + viewTop);
                Log.v(TAG, "onLayoutChildren, do, measuredWidth " + measuredWidth);
                Log.v(TAG, "onLayoutChildren, do, measuredHeight " + measuredHeight);
            }

            layoutDecorated(view, viewLeft, viewTop, viewLeft + measuredWidth, viewTop + measuredHeight);

            // this assumes that all views are the same height
            viewLeft += mStepSize;
            viewTop += measuredHeight;

            mLastVisiblePosition++;

            isLastLaidOutView = isLastLaidOutView(view);

        } while (!isLastLaidOutView && mLastVisiblePosition < itemCount);

    }

    /**
     * TODO: question:
     * How to deal if device is in landscape.
     * This way view will reach the down edge before the right.
     *
     * @param view
     * @return
     */
    private boolean isLastLaidOutView(View view) {
        int left = view.getLeft(); // space from left edge of RecyclerView to left side of the view
        if (SHOW_LOGS) {
            Log.v(TAG, "isLastLaidOutView, left " + left);
            Log.v(TAG, "isLastLaidOutView, mStepSize " + mStepSize);
        }

        int nextViewLeft = left + mStepSize;
        if (SHOW_LOGS) {
            Log.v(TAG, "isLastLaidOutView, width " + getWidth());
            Log.v(TAG, "isLastLaidOutView, leftPadding " + getPaddingLeft());
            Log.v(TAG, "isLastLaidOutView, rightPadding " + getPaddingRight());
        }

        int recyclerViewRightEdge = getWidth() - getPaddingRight();

        if (nextViewLeft > recyclerViewRightEdge) {
            return true;
        }

        return false;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int childCount = getChildCount();
        if (SHOW_LOGS) {
            Log.v(TAG, "scrollVerticallyBy dy " + dy);
            Log.v(TAG, "scrollVerticallyBy childCount " + childCount);
        }

        if (childCount == 0) {
            // we cannot scroll if we don't have views
            return 0;
        }

        boolean isFirstItemReached = isFirstItemReached();
        boolean isLastItemReached = isLastItemReached();

        View firstView = getChildAt(0);
        View lastView = getChildAt(
                getChildCount() - 1
        );

        /** scroll views*/
        int delta;

        if (dy > 0) { // Contents are scrolling up
            //Check against bottom bound
            if (isLastItemReached) {
                //If we've reached the last row, enforce limits
                int recyclerViewRightEdge = getWidth() - getPaddingRight();

                int rightOffset = recyclerViewRightEdge - lastView.getRight();
                delta = Math.max(-dy, rightOffset);
            } else {

                delta = -dy;
            }
        } else { // Contents are scrolling down
            //Check against top bound
            int leftOffset = firstView.getLeft();
            if (SHOW_LOGS) {
                Log.v(TAG, "checkBoundsReached, leftOffset " + leftOffset);
                Log.v(TAG, "checkBoundsReached, dy " + dy);
            }

            if (isFirstItemReached) {
                delta = -Math.max(dy, leftOffset);
            } else {
                delta = -dy;
            }
        }

        for (int indexOfView = 0; indexOfView < childCount; indexOfView++) {
            View view = getChildAt(indexOfView);

            view.offsetTopAndBottom(delta);
            view.offsetLeftAndRight(delta);
        }
        /** scroll views*/

        /** perform recycling*/
        if (delta < 0) {
            /** Scroll down*/
            recycleTopIfNeeded(firstView, recycler);
            addToBottomIfNeeded(lastView, recycler);

        } else {
            /** Scroll up*/
            recycleBottomIfNeeded(lastView, recycler);
            addTopIfNeeded(firstView, recycler);
        }
        /** perform recycling*/

        return delta;
    }

    private void addTopIfNeeded(View firstView, RecyclerView.Recycler recycler) {
        int leftOffset = firstView.getLeft();
        if (SHOW_LOGS) Log.v(TAG, "addTopIfNeeded, leftOffset " + leftOffset);

        if (leftOffset >= 0) {

            if (SHOW_LOGS)
                Log.v(TAG, "addTopIfNeeded, firstVisiblePosition " + mFirstVisiblePosition);

            if (mFirstVisiblePosition > 0) {
                if (SHOW_LOGS) Log.i(TAG, "addTopIfNeeded, add to top");

                View newFirstView = recycler.getViewForPosition(mFirstVisiblePosition - 1);


                addView(newFirstView, 0);
                measureChildWithMargins(newFirstView, 0, 0);

                int measuredWidth = getDecoratedMeasuredWidth(newFirstView);
                int measuredHeight = getDecoratedMeasuredHeight(newFirstView);

                int viewLeft = firstView.getLeft() - mStepSize;
                int viewTop = firstView.getTop() - measuredHeight;
                if (SHOW_LOGS) {
                    Log.v(TAG, "onLayoutChildren, do, viewLeft " + viewLeft);
                    Log.v(TAG, "onLayoutChildren, do, viewTop " + viewTop);
                    Log.v(TAG, "onLayoutChildren, do, measuredWidth " + measuredWidth);
                    Log.v(TAG, "onLayoutChildren, do, measuredHeight " + measuredHeight);
                }


                layoutDecorated(newFirstView, viewLeft, viewTop, viewLeft + measuredWidth, viewTop + measuredHeight);
                mFirstVisiblePosition--;
            } else {
                // this is first view there is no views to add to the top
            }
        }
    }

    private void recycleBottomIfNeeded(View lastView, RecyclerView.Recycler recycler) {
        /**
         * Scroll up. Finger is pulling down
         * This mean that view that goes to bottom-right direction might hide.
         * If view is hidden we will recycle it
         */

        int recyclerViewHeight = getHeight();

        int lastViewLeft = lastView.getLeft();

        if (SHOW_LOGS) {
            Log.v(TAG, "recycleBottomIfNeeded recyclerViewHeight " + recyclerViewHeight);
            Log.v(TAG, "recycleBottomIfNeeded lastViewLeft " + lastViewLeft);
        }

        int rightEdge = getWidth() - getPaddingRight();

        if (lastViewLeft > rightEdge) {
            removeView(lastView);
            mLastVisiblePosition--;
            recycler.recycleView(lastView);
        }
    }

    private void addToBottomIfNeeded(View lastView, RecyclerView.Recycler recycler) {

        int recyclerViewRightEdge = getWidth() - getPaddingRight();

        int rightOffset = recyclerViewRightEdge - lastView.getRight();
        if (SHOW_LOGS) Log.v(TAG, "addToBottomIfNeeded, rightOffset " + rightOffset);

        if (rightOffset > 0) {
            int itemCount = getItemCount();
            int nextPosition = mLastVisiblePosition + 1;

            if (SHOW_LOGS) {
                Log.v(TAG, "addToBottomIfNeeded, itemCount " + itemCount);
                Log.v(TAG, "addToBottomIfNeeded, nextPosition " + nextPosition);
            }

            if (nextPosition <= itemCount) {
                if (SHOW_LOGS) Log.i(TAG, "addToBottomIfNeeded, add new view to bottom");

                View newLastView = recycler.getViewForPosition(nextPosition - 1);

                addView(newLastView);
                measureChildWithMargins(newLastView, 0, 0);

                int measuredWidth = getDecoratedMeasuredWidth(newLastView);
                int measuredHeight = getDecoratedMeasuredHeight(newLastView);
                // TODO: add this to a step
                // TODO: add padding
                int viewTop = lastView.getBottom();
                int viewLeft = lastView.getLeft() + mStepSize;

                layoutDecorated(newLastView, viewLeft, viewTop, viewLeft + measuredWidth, viewTop + measuredHeight);

                mLastVisiblePosition++;
            } else {
                // last view is the last item. Do nothing
            }
        }
    }

    private void recycleTopIfNeeded(View firstView, RecyclerView.Recycler recycler) {
        int recyclerViewLeftEdge = getPaddingLeft();
        boolean needRecycling = firstView.getRight() < recyclerViewLeftEdge;

        if (SHOW_LOGS) Log.v(TAG, "recycleTopIfNeeded, needRecycling " + needRecycling);

        if (needRecycling) {
            // first view is hidden
            if (SHOW_LOGS) Log.i(TAG, "recycleTopIfNeeded, recycling first view");

            removeView(firstView);
            mFirstVisiblePosition++;
            recycler.recycleView(firstView);
        }
    }

    private boolean isFirstItemReached() {
        boolean isFirstItemReached = mFirstVisiblePosition == 0;
        if (SHOW_LOGS) Log.v(TAG, "isFirstItemReached, " + isFirstItemReached);
        return isFirstItemReached;
    }

    private boolean isLastItemReached() {
        boolean isLastItemReached = mLastVisiblePosition == getItemCount();
        if (SHOW_LOGS) Log.v(TAG, "isLastItemReached " + isLastItemReached);
        return isLastItemReached;
    }
}