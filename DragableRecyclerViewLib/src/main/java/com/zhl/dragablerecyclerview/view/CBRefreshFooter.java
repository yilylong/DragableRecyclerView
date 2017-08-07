package com.zhl.dragablerecyclerview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zhl.dragablerecyclerview.R;


public class CBRefreshFooter extends CBRefreshHeaderView {

    private Context mContext;
    private LinearLayout footerViewContanier;
    private RelativeLayout mContentView;
    private View mProgressBar;
    private TextView mHintView;
    private int state;

    public CBRefreshFooter(Context context) {
        super(context);
        initView(context);
    }

    public CBRefreshFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    @Override
    public void setState(int state) {
        this.state = state;
    }

    @Override
    public int getState() {
        return state;
    }

    private void initView(Context context) {
        LinearLayout.LayoutParams params =new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        mContext = context;
        footerViewContanier = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.cblistview_footer, null);
        mContentView = (RelativeLayout) footerViewContanier.findViewById(R.id.cbrefresh_footer_content);
        addView(footerViewContanier,params);
        setGravity(Gravity.TOP);
        mProgressBar = footerViewContanier.findViewById(R.id.cbrefresh_footer_progressbar);
        mHintView = (TextView) footerViewContanier.findViewById(R.id.cbrefresh_footer_hint_textview);
    }

    /**
     * set the footer bg
     *
     * @param resName
     */
    public void setFooterBg(int resName) {
        this.setBackgroundResource(resName);
    }

    @Override
    public void pullUpToLoadmore() {
        mProgressBar.setVisibility(View.GONE);
        mHintView.setVisibility(View.VISIBLE);
        mHintView.setText(getString(R.string.refresh_footer_tip_pullup_loadmore));
    }

    @Override
    public void releaseToLoadmore() {
        mProgressBar.setVisibility(View.GONE);
        mHintView.setVisibility(View.VISIBLE);
        mHintView.setText(getString(R.string.refresh_footer_tip_release_loadmore));
    }

    @Override
    public void onLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
        mHintView.setVisibility(View.VISIBLE);
        mHintView.setText(getString(R.string.refresh_footer_tip_loading));
    }

    @Override
    public void onStyleChange(int state) {
        super.onStyleChange(state);
    }

    @Override
    public int getLoadMorePullUpDistance() {
        return footerViewContanier.getHeight();
    }

    @Override
    public void setLoadMorePullUpDistance(int deltaY) {
        if (deltaY < 0)
            return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) footerViewContanier.getLayoutParams();
        lp.height = deltaY;
        footerViewContanier.setLayoutParams(lp);
    }

    @Override
    public void footerViewShow() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) footerViewContanier.getLayoutParams();
        lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        footerViewContanier.setLayoutParams(lp);
    }

    @Override
    public void footerViewHide() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) footerViewContanier.getLayoutParams();
        lp.height = 0;
        footerViewContanier.setLayoutParams(lp);
    }

    @Override
    public int getRealHeaderContentHeight() {
        if (mContentView != null) {
            return mContentView.getHeight();
        }
        return 0;
    }
}
