package com.zhl.dragablerecyclerview.swipemenu;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;


public abstract class SwipeMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements SwipeMenuView.OnSwipeItemClickListener {
    public static final int TYPE_ITEM_HEADER = 1000000;
    public static final int TYPE_ITEM_FOOTER = 1000001;
    public static final int TYPE_ITEM_CONTENT = 1000002;
    private RecyclerView.Adapter mAdapter;
    private Context mContext;

    public SwipeMenuAdapter(Context context, RecyclerView.Adapter adapter) {
        mAdapter = adapter;
        mContext = context;
    }

    protected abstract void createMenu(SwipeMenu menu);

    protected abstract boolean isPullRefreshEnable();

    protected abstract boolean isPullLoadMoreEnable();

    protected abstract View getHeaderView();

    protected abstract View getFooterView();
//    {
//        // Test Code
//        SwipeMenuItem item = new SwipeMenuItem(mContext);
//        item.setTitle("Item 1");
//        item.setBackground(new ColorDrawable(Color.GRAY));
//        item.setWidth(300);
//        menu.addMenuItem(item);
//
//        item = new SwipeMenuItem(mContext);
//        item.setTitle("Item 2");
//        item.setBackground(new ColorDrawable(Color.RED));
//        item.setWidth(300);
//        menu.addMenuItem(item);
//    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType==TYPE_ITEM_HEADER){
            return new SimpleViewHolder(getHeaderView());
        }else if(viewType==TYPE_ITEM_FOOTER){
            return new SimpleViewHolder(getFooterView());
        }else {
            RecyclerView.ViewHolder viewHolder = mAdapter.onCreateViewHolder(parent, viewType);
            SwipeMenu menu = new SwipeMenu(mContext);
            menu.setViewType(getItemViewType(viewHolder.getAdapterPosition()));
            createMenu(menu);
            SwipeMenuView menuView = new SwipeMenuView(menu);
            menuView.setOnSwipeItemClickListener(this);
            SwipeMenuLayout layout = new SwipeMenuLayout(viewHolder.itemView, menuView, new LinearInterpolator(),
                    new LinearInterpolator());
            SwipeMenuViewHolder swipeMenuViewHolder = new SwipeMenuViewHolder(layout);
            swipeMenuViewHolder.setOrignalHolder(viewHolder);
            return swipeMenuViewHolder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position)==TYPE_ITEM_HEADER||getItemViewType(position)==TYPE_ITEM_FOOTER){
            return;
        }
        int contentPos = position;
        if(isPullRefreshEnable()){
            contentPos = position-1;
        }
        ((SwipeMenuLayout) holder.itemView).setPosition(contentPos);
        mAdapter.onBindViewHolder(((SwipeMenuViewHolder) holder).getOrignalHolder(), contentPos);
    }

    @Override
    public int getItemViewType(int position) {
        if ((position == 0 && isPullRefreshEnable())) {
            return TYPE_ITEM_HEADER;
        } else if ((position == getItemCount()-1 && isPullLoadMoreEnable())) {
            return TYPE_ITEM_FOOTER;
        }
        int contentPos = position;
        if(isPullRefreshEnable()){
            contentPos = position-1;
        }
        return mAdapter.getItemViewType(contentPos);
    }

    @Override
    public int getItemCount() {
        if ((isPullRefreshEnable() && !isPullLoadMoreEnable()) || (!isPullRefreshEnable() && isPullLoadMoreEnable())) {
            return mAdapter.getItemCount() + 1;
        } else if (isPullRefreshEnable() && isPullLoadMoreEnable()) {
            return mAdapter.getItemCount() + 2;
        } else {
            return mAdapter.getItemCount();
        }
    }


    @Override
    public void onItemClick(SwipeMenuView view, SwipeMenu menu, int index) {

    }

    private class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class SwipeMenuViewHolder extends RecyclerView.ViewHolder {
        private RecyclerView.ViewHolder orignalHolder;

        public SwipeMenuViewHolder(View itemView) {
            super(itemView);
        }

        public RecyclerView.ViewHolder getOrignalHolder() {
            return orignalHolder;
        }

        public void setOrignalHolder(RecyclerView.ViewHolder orignalHolder) {
            this.orignalHolder = orignalHolder;
        }
    }

}
