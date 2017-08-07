package com.zhl.dragablerecyclerviewsample;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.zhl.dragablerecyclerview.swipemenu.SwipeMenu;
import com.zhl.dragablerecyclerview.swipemenu.SwipeMenuCreator;
import com.zhl.dragablerecyclerview.swipemenu.SwipeMenuItem;
import com.zhl.dragablerecyclerview.view.DragableRecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private DragableRecyclerView mRecyclerView;
    private ArrayList<String> Data = new ArrayList<String>();
    private MyAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        mRecyclerView = (DragableRecyclerView) findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 设置侧滑菜单
        mRecyclerView.setMenuCreator(new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem deleteItem = new SwipeMenuItem(getApplicationContext());
                deleteItem.setBackground(R.color.colorAccent);
                deleteItem.setWidth(dp2px(MainActivity.this, 90));
                deleteItem.setTitle("删除");
                deleteItem.setIcon(R.mipmap.icon_delete);
                deleteItem.setTitleSize(18);
                deleteItem.setTitleColor(Color.WHITE);
                menu.addMenuItem(deleteItem);

                SwipeMenuItem collectionItem = new SwipeMenuItem(getApplicationContext());
                collectionItem.setBackground(R.color.green);
                collectionItem.setWidth(dp2px(MainActivity.this, 90));
                collectionItem.setTitle("收藏");
                collectionItem.setTitleSize(18);
                collectionItem.setTitleColor(Color.WHITE);
                collectionItem.setIcon(R.mipmap.icon_collection);
                menu.addMenuItem(collectionItem);
            }
        });
        // 开启刷新头动画
        mRecyclerView.showHeaderAnim();
        // 是否开启侧滑
        mRecyclerView.setSwipeEnable(true);
        // 是否可以长按拖拽
        mRecyclerView.setLongPressDragEnabled(true);
        // 设置自定义的下拉刷新和上拉加载刷新头
//        mRecyclerView.setRefreshHeader(new CustomHeader(this));
//        mRecyclerView.setLoadMoreFooter(new CustomFooter(this));
        mRecyclerView.setAdapter(adapter = new MyAdapter());
        // 是否开启下拉刷新
        mRecyclerView.setPullRefreshEnable(true);
        // 是否开启上拉加载
        mRecyclerView.setPullLoadMoreEnable(true);
        // 设置长按拖拽的监听
        mRecyclerView.setOnItemDragListener(new DragableRecyclerView.OnItemDragListener() {
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if(viewHolder!=null){
                    viewHolder.itemView.setBackgroundResource(R.color.colorAccent);
                }
            }
            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                String movedTX = Data.remove(fromPos);
                Data.add(toPos,movedTX);
            }

            @Override
            public boolean onDragScaleable() {
                return true;
            }
            @Override
            public void onDragCompleted(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                viewHolder.itemView.setBackgroundResource(R.color.gray);
            }
        });
        // 设置侧滑菜单的监听
        mRecyclerView.setOnSwipedMenuItemClickListener(new DragableRecyclerView.OnSwipedMenuItemClickListener() {

            @Override
            public void onMenuItemClick(int position, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, SwipeMenu menu, int index) {
                if(index==0){
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(MainActivity.this,"删除第"+position+"项",Toast.LENGTH_SHORT).show();
                }else if(index==1){
                    Toast.makeText(MainActivity.this,"收藏第"+position+"项",Toast.LENGTH_SHORT).show();
                }
            }
        });
        // 设置recyclerview的item点击监听
        mRecyclerView.setOnItemClickListener(new DragableRecyclerView.OnItemClickListener() {
            @Override
            public void onItemClick(int position, RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, View ConvertView) {
                Toast.makeText(MainActivity.this,"点击第"+position+"项",Toast.LENGTH_SHORT).show();
            }
        });
        // 设置下拉刷新的监听
        mRecyclerView.setPullRefreshListener(new DragableRecyclerView.OnPullRefreshListener() {
            @Override
            public void onRefresh() {
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                        mRecyclerView.stopRefresh();
                    }
                },3000);
            }
            @Override
            public void onLoadMore() {

                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadMore();
                        mRecyclerView.stopLoadMore();
                    }
                },3000);
            }
            @Override
            public void onUpdateRefreshTime(long time) {

            }
        });
    }

    private void refresh() {
        Data.clear();
        initData();
        adapter.notifyDataSetChanged();
    }

    private void loadMore() {
       for(int i=0;i<10;i++){
           Data.add("load more "+i);
       }
        adapter.notifyItemRangeInserted(mRecyclerView.getAdapter().getItemCount(),10);
    }

    private void initData() {
        for(int i=0;i<50;i++){
            Data.add("MaterialDesign_"+i);
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<MyViewHolder>{
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_recycler,parent,false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.itemText.setText(Data.get(position));
        }

        @Override
        public int getItemCount() {
            return Data.size();
        }
    }

    private class MyViewHolder extends RecyclerView.ViewHolder{
        private TextView itemText;
        public MyViewHolder(View itemView) {
            super(itemView);
            itemText = (TextView) itemView.findViewById(R.id.item_tx);
        }
    }

    public int dp2px(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
