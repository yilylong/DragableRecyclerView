package com.zhl.dragablerecyclerview.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.zhl.dragablerecyclerview.swipemenu.SwipeMenu;
import com.zhl.dragablerecyclerview.swipemenu.SwipeMenuAdapter;
import com.zhl.dragablerecyclerview.swipemenu.SwipeMenuCreator;
import com.zhl.dragablerecyclerview.swipemenu.SwipeMenuLayout;
import com.zhl.dragablerecyclerview.swipemenu.SwipeMenuView;
import com.zhl.dragablerecyclerview.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 描述：可拖拽排序，侧滑删除，下拉刷新上拉加载的recyclerview
 * Created by zhaohl on 2017-7-27.
 */

public class DragableRecyclerView extends RecyclerView {
    private static final int TOUCH_STATE_NONE = 0;
    private static final int TOUCH_STATE_X = 1;
    private static final int TOUCH_STATE_Y = 2;
    private int mTouchPosition;
    private int mTouchState = TOUCH_STATE_NONE;
    private float mDownX;
    private float mDownY;
    private float disX, disY;
    private ItemTouchHelper itemTouchHelper;
    private SwipeMenuCreator mMenuCreator;
    private GestureDetectorCompat mGestureDetector;
    private GestureDetector.OnGestureListener mGestureListener;
    private float mLastY = -1;
    private boolean swipeEnable = false;
    private boolean swipeFlag = false;
    private SwipeMenuLayout mTouchView;
    private boolean isFling;
    private int MIN_FLING;
    private int MAX_VELOCITYX;
    private int touchSlop;
    private boolean isLongPressDragEnabled;
    private boolean longpressDragFlag;
    private OnItemDragListener onItemDragListener;
    private OnSwipedMenuItemClickListener onSwipedMenuItemClickListener;
    private OnItemClickListener onItemClickListener;
    private OnPullRefreshListener mPullRefreshListener;
    private SwipeMenuAdapter mWrapAdapter;
    private final RecyclerView.AdapterDataObserver mDataObserver = new DataObserver();
    // -- header view
    private CBRefreshHeaderView mHeaderView;
    // -- footer view
    private CBRefreshHeaderView mFooterView;
    private int mHeaderViewHeight; // header view's height
    private int mFooterViewHeight; // header view's height
    private boolean mEnablePullRefresh = true;
    private boolean mPullRefreshing = false; // is refreashing.
    private boolean mEnablePullLoad;
    private boolean mPullLoading;// isLoading
    //    private boolean mIsFooterReady = false;
    // total list items, used to detect is at the bottom of listview.
//    private int mTotalItemCount = 0;
    // for mScroller, scroll back from header or footer.
    private int mScrollBack;
    private final static int SCROLLBACK_HEADER = 0;
    private final static int SCROLLBACK_FOOTER = 1;
    private final static int SCROLL_DURATION = 300; // scroll back duration
    //    private final static int PULL_LOAD_MORE_DELTA = 150; // when pull up >= 150px
    private final static float OFFSET_RADIO = 2.3f; // support iOS like pull
    // feature.
//    private boolean showTopSearchBar = false;
//    private CBRefreshHeaderView topSearchView;
//    private int topSearchBarHeight = 0;
    private Scroller mScroller;
    private long refreshTime;
    private LinearLayout.LayoutParams headerParams;

    public DragableRecyclerView(Context context) {
        this(context, null);
    }

    public DragableRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public DragableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        MIN_FLING = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
        MAX_VELOCITYX = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        headerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHeaderView = new CBRefreshHeader(context);
        mHeaderView.setLayoutParams(headerParams);
        // init footer view
        mFooterView = new CBRefreshFooter(context);
        mFooterView.setLayoutParams(headerParams);
        initHeaderHeight();
        // headerview and footer divider
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return isLongPressDragEnabled;
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
                int dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlag = ItemTouchHelper.LEFT;
                LayoutManager layoutmanager = getLayoutManager();
                if (layoutmanager != null && (layoutmanager instanceof GridLayoutManager || layoutmanager instanceof StaggeredGridLayoutManager)) {
                    dragFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                }
                return makeMovementFlags(dragFlag, swipeFlag);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (mEnablePullRefresh && to == 0) {
                    return false;
                }
                if (mEnablePullLoad && to == recyclerView.getAdapter().getItemCount() - 1) {
                    return false;
                }
                getAdapter().notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onMoved(RecyclerView recyclerView, ViewHolder viewHolder, int fromPos, ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
                if (onItemDragListener != null) {
                    onItemDragListener.onMoved(recyclerView, ((SwipeMenuAdapter.SwipeMenuViewHolder) viewHolder).getOrignalHolder(), fromPos, target, toPos, x, y);
                }
            }

            @Override
            public void onSwiped(ViewHolder viewHolder, int direction) {
//                int removedPos = viewHolder.getAdapterPosition();
//                getAdapter().notifyItemRemoved(removedPos);
//                if (null!=onItemDragListener) {
//                    onItemDragListener.onSwiped(viewHolder,direction);
//                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (isLongPressDragEnabled) {
                    swipeEnable = false;
                }
                super.onSelectedChanged(viewHolder, actionState);
                if (null != onItemDragListener && onItemDragListener.onDragScaleable() && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    ViewCompat.setScaleX(viewHolder.itemView, 1.1f);
                    ViewCompat.setScaleY(viewHolder.itemView, 1.1f);
                }
                if (null != onItemDragListener && viewHolder != null) {
                    onItemDragListener.onSelectedChanged(((SwipeMenuAdapter.SwipeMenuViewHolder) viewHolder).getOrignalHolder(), actionState);
                }
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                ViewCompat.setScaleX(viewHolder.itemView, 1.0f);
                ViewCompat.setScaleY(viewHolder.itemView, 1.0f);
                if (swipeFlag) {
                    swipeEnable = true;
                }
                if (null != onItemDragListener) {
                    onItemDragListener.onDragCompleted(recyclerView, ((SwipeMenuAdapter.SwipeMenuViewHolder) viewHolder).getOrignalHolder());
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(this);
        mGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                isFling = false;
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null) {
                    return false;
                }
                if ((e1.getX() - e2.getX()) > MIN_FLING && velocityX < MAX_VELOCITYX) {
                    isFling = true;
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        };
        mGestureDetector = new GestureDetectorCompat(getContext(), mGestureListener);
        mScroller = new Scroller(context, new DecelerateInterpolator());
    }

    private void initHeaderHeight() {
        mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mHeaderViewHeight = mHeaderView.getRealHeaderContentHeight();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
        mFooterView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mFooterViewHeight = mFooterView.getRealHeaderContentHeight();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    /**
     * 设置侧滑菜单构造器
     *
     * @param menuCreator
     */
    public void setMenuCreator(SwipeMenuCreator menuCreator) {
        this.mMenuCreator = menuCreator;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(mWrapAdapter = new SwipeMenuAdapter(getContext(), adapter) {
            @Override
            protected void createMenu(SwipeMenu menu) {
                if (mMenuCreator != null) {
                    mMenuCreator.create(menu);
                }
            }

            @Override
            protected boolean isPullRefreshEnable() {
                return mEnablePullRefresh;
            }

            @Override
            protected boolean isPullLoadMoreEnable() {
                return mEnablePullLoad;
            }

            @Override
            protected View getHeaderView() {
                return mHeaderView;
            }

            @Override
            protected View getFooterView() {
                return mFooterView;
            }

            @Override
            public void onItemClick(SwipeMenuView view, SwipeMenu menu, int index) {
                if (mTouchView != null) {
                    mTouchView.smoothCloseMenu();
                }
                if (null != onSwipedMenuItemClickListener) {
                    onSwipedMenuItemClickListener.onMenuItemClick(mTouchPosition, getAdapter(), menu, index);
                }
            }
        });
        adapter.registerAdapterDataObserver(mDataObserver);
        mDataObserver.onChanged();
    }

    private class DataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            if (mWrapAdapter != null) {
                mWrapAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mWrapAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mWrapAdapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mWrapAdapter.notifyItemMoved(fromPosition, toPosition);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mGestureDetector.onTouchEvent(ev);
        if (mLastY == -1) {
            mLastY = ev.getRawY();
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                mDownX = ev.getX();
                mDownY = ev.getY();
                int oldPos = mTouchPosition;
                mTouchPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
                View view = getChildAt(mTouchPosition - getFirstVisiblePosition());
                if (swipeEnable && mTouchView != null && (mTouchView.isOpen() || mTouchView.isActive()) && oldPos != mTouchPosition) {
                    mTouchView.smoothCloseMenu();
                    mTouchView = null;
                    mTouchState = TOUCH_STATE_NONE;
                }
                if (view instanceof SwipeMenuLayout) {
                    mTouchView = (SwipeMenuLayout) view;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final float deltaY = ev.getRawY() - mLastY;
                mLastY = ev.getRawY();
                disX = ev.getX() - mDownX;
                disY = ev.getY() - mDownY;

                if (Math.abs(disX) > touchSlop && Math.abs(disX) > Math.abs(disY)) {// 左右滑动
                    mTouchState = TOUCH_STATE_X;
                    if (swipeEnable && mTouchView != null) {
                        if (longpressDragFlag) {
                            isLongPressDragEnabled = false;
                        }
                        mTouchView.swipeMenuSlide(-(int) disX);
                        if (mTouchView.isActive()) {
                            return true;
                        }
                    }
                } else if (Math.abs(disY) > touchSlop && Math.abs(disY) > Math.abs(disX)) {// 上下滑动
                    if ((swipeEnable && mTouchView != null && mTouchView.isOpen()) || (swipeEnable && mTouchView != null && mTouchView.isActive())) {
                        mTouchView.smoothCloseMenu();
                        mTouchView = null;
                        mTouchState = TOUCH_STATE_NONE;
                    }
                    mTouchState = TOUCH_STATE_Y;
                    if (mEnablePullRefresh && getFirstVisiblePosition() <= 1 && (mHeaderView.getVisiableHeight() > 0 || deltaY > 0)) {// 下拉
                        updateHeaderHeight(deltaY / OFFSET_RADIO);
                        mHeaderView.onDragSlide((float) mHeaderView.getVisiableHeight() + Math.abs(deltaY / OFFSET_RADIO));
                        invokeOnScrolling();

                    } else if (mEnablePullLoad && getLastVisiblePosition() >= getAdapter().getItemCount() - 2 && (mFooterView.getLoadMorePullUpDistance() > 0 || deltaY < 0)) {// 上拉
                        updateFooterHeight(-deltaY / OFFSET_RADIO);
                        mFooterView.onDragSlide((float) mFooterView.getLoadMorePullUpDistance() + (-deltaY / OFFSET_RADIO));
                    }
                }
                break;
            default:
                mLastY = -1; // reset
                switch (mTouchState) {
                    case TOUCH_STATE_X:
                        if (swipeEnable && mTouchView != null) {
                            mTouchView.swipeMenuFling(isFling, -(int) disX);
                            if (mTouchView.isActive()) {
                                return true;
                            }
                        }
                        break;
                    case TOUCH_STATE_Y:
                        if (getFirstVisiblePosition() == 0) {
                            // invoke refresh
                            if (mEnablePullRefresh && mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
                                mPullRefreshing = true;
                                mHeaderView.onRefreshing();
                                mHeaderView.setState(CBRefreshState.STATE_REFRESHING);
                                if (mPullRefreshListener != null) {
                                    mPullRefreshListener.onRefresh();
                                    mPullRefreshListener.onUpdateRefreshTime(System.currentTimeMillis());
                                }
                                setRefreshTime(System.currentTimeMillis());
                            }
                            resetHeaderHeight();
                        } else if (getLastVisiblePosition() == getAdapter().getItemCount() - 1) {
                            // invoke load more.
                            if (mEnablePullLoad && mFooterView.getLoadMorePullUpDistance() > mFooterViewHeight) {
                                startLoadMore();
                            }
                            resetFooterHeight();
                        } else {
                            if (mHeaderView.getVisiableHeight() > 0 || disY > 0) {
                                resetHeaderHeight();
                            }
                        }
                        break;
                    case TOUCH_STATE_NONE:
                        if (onItemClickListener != null) {
                            int clickPos = mEnablePullRefresh ? getFirstVisiblePosition() == 1 ? mTouchPosition - 2 : mTouchPosition - 1 : mTouchPosition;
                            if (clickPos >= 0) {
                                onItemClickListener.onItemClick(clickPos, getAdapter(), mTouchView);
                            }
                        }
                        break;
                }
                mTouchState = TOUCH_STATE_NONE;
                if (longpressDragFlag) {
                    isLongPressDragEnabled = true;
                }
                break;
        }
        return super.onTouchEvent(ev);
    }


    private int getFirstVisiblePosition() {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        } else if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] span = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
            ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(span);
            return findMin(span);
        }
        return 0;
    }

    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private int findMin(int[] lastPositions) {
        int min = lastPositions[0];
        for (int value : lastPositions) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private int getLastVisiblePosition() {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] span = new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
            ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(span);
            return findMax(span);
        }
        return 0;
    }

    private int pointToPosition(int x, int y) {
        final int count = getChildCount();
        Rect frame = new Rect();
        int pos = 0;
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    pos = getFirstVisiblePosition() + i;
                }
            }
        }
        return pos;
    }

    private void startLoadMore() {
        mPullLoading = true;
        mFooterView.onLoading();
        mFooterView.setState(CBRefreshState.STATE_REFRESHING);
        if (mPullRefreshListener != null) {
            mPullRefreshListener.onLoadMore();
        }
    }

    private void updateHeaderHeight(float delta) {
        mHeaderView.setVisiableHeight((int) delta + mHeaderView.getVisiableHeight());
        if (mEnablePullRefresh && !mPullRefreshing) {
            if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
                mHeaderView.releaseToRefresh();
                mHeaderView.setState(CBRefreshState.STATE_RELEASE_TO_REFRESH);
            } else {
                mHeaderView.pullToRefresh();
                mHeaderView.setState(CBRefreshState.STATE_PULL_TO_REFRESH);
            }
        }
    }

    /**
     * 恢复刷新头高度
     */
    private void resetHeaderHeight() {
        int height = mHeaderView.getVisiableHeight();
        if (height == 0)
            return;
        // refreshing and header isn't shown fully. do nothing.
        if (mPullRefreshing && height <= mHeaderViewHeight) {
            return;
        }
        int finalHeight = 0; // default: scroll back to dismiss header.
        // is refreshing, just scroll back to show all the header.
        if (mPullRefreshing && height > mHeaderViewHeight) {
            finalHeight = mHeaderViewHeight;
        }
        mScrollBack = SCROLLBACK_HEADER;
        mScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);
        // trigger computeScroll
        invalidate();
    }

    /**
     * 更新下拉头高度
     *
     * @param delta
     */
    private void updateFooterHeight(float delta) {
        if (!mEnablePullLoad) {
            return;
        }
        int height = mFooterView.getLoadMorePullUpDistance() + (int) delta;
        Log.i("mytag", "footer height =" + height);
        if (mEnablePullLoad && !mPullLoading) {
            if (height > mFooterViewHeight) { // height enough to invoke load more.
                mFooterView.releaseToLoadmore();
                mFooterView.setState(CBRefreshState.STATE_RELEASE_TO_LOADMORE);
            } else {
                mFooterView.pullUpToLoadmore();
                mFooterView.setState(CBRefreshState.STATE_PULL_UP_TO_LOADMORE);
            }
        }
        mFooterView.setLoadMorePullUpDistance(height);

        // setSelection(mTotalItemCount - 1); // scroll to bottom
    }

    /**
     * 恢复下拉头高度
     */
    private void resetFooterHeight() {
        int footerHeight = mFooterView.getLoadMorePullUpDistance();
        if (footerHeight == 0) {
            return;
        }
        if (mPullLoading && footerHeight <= mFooterViewHeight) {
            return;
        }

        int finalHeight = 0;
        if (mPullLoading && footerHeight > mFooterViewHeight) {
            finalHeight = mFooterViewHeight;
        }
        mScrollBack = SCROLLBACK_FOOTER;
        mScroller.startScroll(0, footerHeight, 0, finalHeight - footerHeight, SCROLL_DURATION);
        invalidate();

    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScrollBack == SCROLLBACK_HEADER) {
                mHeaderView.setVisiableHeight(mScroller.getCurrY());
                mHeaderView.onDragSlide(mScroller.getCurrY());
            } else {
                mFooterView.setLoadMorePullUpDistance(mScroller.getCurrY());
                mFooterView.onDragSlide(mScroller.getCurrY());
            }
            postInvalidate();
            invokeOnScrolling();
        }
        super.computeScroll();
    }

    /**
     * 是否开启下拉刷新
     *
     * @param enable
     */
    public void setPullRefreshEnable(boolean enable) {
        mEnablePullRefresh = enable;
        mHeaderView.setPullRefreshEnable(mEnablePullRefresh);
    }

    /**
     * 设置刷新头刷新图片
     *
     * @param resName
     */
    public void setHeaderRefreshIcon(int resName) {
        if (mHeaderView != null) {
            mHeaderView.setHeaderIcon(resName);
        }
    }

    /**
     * 设置刷新头背景
     *
     * @param resName
     */
    public void setHeaderBg(int resName) {
        if (mHeaderView != null) {
            ((CBRefreshHeader) mHeaderView).setHeaderBg(resName);
        }
    }

    /**
     * set footer view background
     *
     * @param resName
     */
    public void setFooterBg(int resName) {
        if (mFooterView != null) {
            ((CBRefreshFooter) mFooterView).setFooterBg(resName);
        }
    }

    /**
     * 可以需要换肤的情况下调用此方法（自己定义刷新头去实现相应的换肤操作）
     *
     * @param state
     */
    public void setStyleChange(int state) {
        mHeaderView.onStyleChange(state);
        mFooterView.onStyleChange(state);
    }

    /**
     * 是否开启上拉加载
     *
     * @param enable
     */
    public void setPullLoadMoreEnable(boolean enable) {
        mEnablePullLoad = enable;
        if (!mEnablePullLoad) {
            mFooterView.footerViewHide();
            mFooterView.setOnClickListener(null);
        } else {
            mPullLoading = false;
            mFooterView.footerViewShow();
            mFooterView.setState(CBRefreshState.STATE_PULL_UP_TO_LOADMORE);
            mFooterView.pullUpToLoadmore();
            mFooterView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startLoadMore();
                }
            });
        }
    }

    /**
     * 设置刷新头
     *
     * @param header
     * @param <T>
     */
    public <T extends CBRefreshHeaderView> void setRefreshHeader(T header) {
        if (header == null) {
            return;
        }
        setPullRefreshEnable(true);
        if (mWrapAdapter != null) {
            mWrapAdapter.notifyItemRemoved(0);
        }
        mHeaderView = header;
        mHeaderView.setLayoutParams(headerParams);
        initHeaderHeight();
        if (mWrapAdapter != null) {
            mWrapAdapter.notifyItemInserted(0);
        }
    }

    /**
     * 设置加载更多的footerview
     *
     * @param footer
     * @param <T>
     */
    public <T extends CBRefreshHeaderView> void setLoadMoreFooter(T footer) {
        if (footer == null) {
            return;
        }
        setPullLoadMoreEnable(true);
        if (mWrapAdapter != null) {
            mWrapAdapter.notifyItemRemoved(mWrapAdapter.getItemCount() - 1);
        }
        mFooterView = footer;
        mFooterView.setLayoutParams(headerParams);
        initHeaderHeight();
        if (mWrapAdapter != null) {
            mWrapAdapter.notifyItemInserted(mWrapAdapter.getItemCount() - 1);
        }
    }

    /**
     * 设置刷新时间
     *
     * @param time timestamp
     */
    public void setRefreshTime(long time) {
        if (time <= 0) {
            setRefreshTime(null);
        } else {
            refreshTime = time;
            setRefreshTime(Utils.getTimeDifferent(getContext(), time));
        }
    }

    /**
     * set last refresh time
     *
     * @param time
     */
    @SuppressLint("SimpleDateFormat")
    public void setRefreshTime(String time) {
        if (null == time) {
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            mHeaderView.setRefreshTime(dateFormat.format(now));
        } else {
            mHeaderView.setRefreshTime(time);
        }
    }

    /**
     * 显示刷新头动画 对应的停止{@link #stopHeaderAnim()}
     */
    public void showHeaderAnim() {
        if (mEnablePullRefresh && mHeaderView != null) {
            mHeaderView.showHeaderAnim();
        }
    }

    /**
     * 停止刷新头动画 对应的开始{@link #showHeaderAnim()}
     */
    public void stopHeaderAnim() {
        if (mEnablePullRefresh && mHeaderView != null) {
            mHeaderView.stopHeaderAnim();
        }
    }

    /**
     * 停止刷新 恢复刷新头
     */
    public void stopRefresh() {
        if (mPullRefreshing == true) {
            mPullRefreshing = false;
            resetHeaderHeight();
            mHeaderView.setState(CBRefreshState.STATE_PULL_TO_REFRESH);
        }
    }

    /**
     * stop load more, reset footer view.
     */
    public void stopLoadMore() {
        if (mPullLoading == true) {
            mPullLoading = false;
            resetFooterHeight();
            mFooterView.pullUpToLoadmore();
            mFooterView.setState(CBRefreshState.STATE_PULL_UP_TO_LOADMORE);
        }
    }

    /**
     * 是否开启侧滑菜单
     *
     * @param swipeEnable
     */
    public void setSwipeEnable(boolean swipeEnable) {
        this.swipeEnable = swipeEnable;
        this.swipeFlag = swipeEnable;
    }

    /**
     * 是否开启长按拖拽
     *
     * @param longPressDragEnabled
     */
    public void setLongPressDragEnabled(boolean longPressDragEnabled) {
        this.isLongPressDragEnabled = longPressDragEnabled;
        this.longpressDragFlag = longPressDragEnabled;
    }

    /**
     * 设置长按拖拽监听
     *
     * @param onItemDragListener
     */
    public void setOnItemDragListener(OnItemDragListener onItemDragListener) {
        this.onItemDragListener = onItemDragListener;
    }


    /**
     * 长按拖拽监听
     */
    public interface OnItemDragListener {
        /**
         * 长按拖动view 发生改变
         *
         * @param viewHolder
         * @param actionState
         */
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState);

        /**
         * 移动交换一个位置调用
         *
         * @param recyclerView
         * @param viewHolder
         * @param fromPos
         * @param target
         * @param toPos
         * @param x
         * @param y
         */
        public void onMoved(RecyclerView recyclerView, ViewHolder viewHolder, int fromPos, ViewHolder target, int toPos, int x, int y);

        /**
         * 拖动view是否具有放大效果
         *
         * @return
         */
        public boolean onDragScaleable();

        /**
         * 拖拽完成
         *
         * @param recyclerView
         * @param viewHolder
         */
        public void onDragCompleted(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder);
    }

    /**
     * 设置侧滑菜单监听
     *
     * @param onSwipedMenuItemClickListener
     */
    public void setOnSwipedMenuItemClickListener(OnSwipedMenuItemClickListener onSwipedMenuItemClickListener) {
        this.onSwipedMenuItemClickListener = onSwipedMenuItemClickListener;
    }

    /**
     * 侧滑菜单点击监听回调
     */
    public interface OnSwipedMenuItemClickListener {
        /**
         * 侧滑菜单点击监听回调
         *
         * @param position recyclerview position
         * @param adapter  recyclerview adapter
         * @param menu     swipemenu
         * @param index    swipemenu index 侧滑菜单索引
         */
        public void onMenuItemClick(int position, Adapter<ViewHolder> adapter, SwipeMenu menu, int index);
    }

    /**
     * 设置recycleview的onitem监听
     *
     * @param onItemClickListener
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    /**
     * recyclerview的item点击事件监听回调
     */
    public interface OnItemClickListener {
        /**
         * recyclerview的item点击事件监听回调
         *
         * @param position
         * @param adapter
         * @param ConvertView
         */
        public void onItemClick(int position, Adapter<ViewHolder> adapter, View ConvertView);
    }

    private void invokeOnScrolling() {
//        if (mScrollListener instanceof OnXScrollListener) {
//            OnXScrollListener l = (OnXScrollListener) mScrollListener;
//            l.onXScrolling(this);
//        }
    }

    /**
     * 设置下拉刷新监听
     *
     * @param mPullRefreshListener
     */
    public void setPullRefreshListener(OnPullRefreshListener mPullRefreshListener) {
        this.mPullRefreshListener = mPullRefreshListener;
    }

    /**
     * the refresh listener
     */
    public interface OnPullRefreshListener {
        public void onRefresh();

        public void onLoadMore();

        public void onUpdateRefreshTime(long time);
    }

}
