# DragableRecyclerView [![](https://jitpack.io/v/yilylong/DragableRecyclerView.svg)](https://jitpack.io/#yilylong/DragableRecyclerView)
dragable and swipeable pullrefresh recyclerview--支持拖拽排序，侧滑删除，和下拉刷新上拉加载更多

<img src='/GIF.gif'>

useage
---

Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
	
    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Step 2. Add the dependency

    dependencies {
	        compile 'com.github.yilylong:DragableRecyclerView:v1.0.2'
	}

in your xml:
---
    <com.zhl.dragablerecyclerview.view.DragableRecyclerView
        android:id="@+id/recyclerview"
        android:background="#5e5c5c"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
        
swipe menu
---

    // 是否开启侧滑
        mRecyclerView.setSwipeEnable(true);
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
 
 长按拖拽排序
 ---
 
    // 是否可以长按拖拽
        mRecyclerView.setLongPressDragEnabled(true);  
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
        
下拉刷新
---

    // 是否开启下拉刷新
        mRecyclerView.setPullRefreshEnable(true);
        // 是否开启上拉加载
        mRecyclerView.setPullLoadMoreEnable(true);
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
                mRecyclerView.setRefreshTime(time);
            }
        });


  自定义刷新头
  ----
     
     继承CBRefreshHeaderView，根据需要覆盖CBRefreshState中的各种状态方法，recyclerview会回调下拉刷新和上拉加载的各种方法。主要通过更改刷新头高度或者mragin来实现，参考CBRefreshHeader和CBRefreshFooter。
     
      // 设置自定义的下拉刷新和上拉加载刷新头
     mRecyclerView.setRefreshHeader(new CustomHeader(this));
     mRecyclerView.setLoadMoreFooter(new CustomFooter(this));


另外listview 版本的看这里：[CBPullRefreshListView](https://github.com/yilylong/CBPullRefreshListView)
感谢Xrecyclerview XlistView
















                    
  
        
