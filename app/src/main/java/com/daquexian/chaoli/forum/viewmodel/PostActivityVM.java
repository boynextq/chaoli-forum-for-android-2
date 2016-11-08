package com.daquexian.chaoli.forum.viewmodel;

import android.databinding.ObservableArrayList;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.util.Log;

import com.daquexian.chaoli.forum.ChaoliApplication;
import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.binding.PostLayoutSelector;
import com.daquexian.chaoli.forum.meta.Constants;
import com.daquexian.chaoli.forum.model.Conversation;
import com.daquexian.chaoli.forum.model.Post;
import com.daquexian.chaoli.forum.model.PostListResult;
import com.daquexian.chaoli.forum.network.MyRetrofit;
import com.daquexian.chaoli.forum.utils.MyUtils;

import java.util.List;

/**
 * Created by jianhao on 16-9-21.
 */

public class PostActivityVM extends BaseViewModel {
    public Conversation conversation;
    public int conversationId;
    public String title;
    public ObservableBoolean isRefreshing = new ObservableBoolean(false);
    //public ObservableField<SwipyRefreshLayoutDirection> direction = new ObservableField<>(SwipyRefreshLayoutDirection.BOTH);
    public ObservableBoolean canRefresh = new ObservableBoolean(true);
    public final ObservableArrayList<Post> postList = new ObservableArrayList();
    public int minPage;
    public int maxPage;
    boolean isAuthorOnly;

    private Boolean reversed = false;
    private Boolean preview = true;     //是否是只加载了第一条帖子的状态

    public ObservableInt listPosition = new ObservableInt();
    public ObservableBoolean showToast = new ObservableBoolean(false);
    public ObservableField<String> toastContent = new ObservableField<>();

    public ObservableBoolean goToReply = new ObservableBoolean(false);
    public ObservableBoolean goToQuote = new ObservableBoolean();
    public ObservableBoolean goToHomepage = new ObservableBoolean(false);
    public Post clickedPost;

    private static final String TAG = "PostActivityVM";

    public PostLayoutSelector layoutSelector = new PostLayoutSelector();

    public PostActivityVM(int conversationId, String title) {
        this.conversationId = conversationId;
        this.title = title;
    }

    public PostActivityVM(Conversation conversation) {
        this.conversation = conversation;
        postList.add(new Post(Integer.valueOf(conversation.getStartMemberId()), conversation.getStartMember(), conversation.getStartMemberAvatarSuffix(), conversation.getFirstPost(), conversation.getStartTime()));
        conversationId = conversation.getConversationId();
        title = conversation.getTitle();
    }

    public Boolean isReversed() {
        return reversed;
    }

    /*private void getList(final int page) {
        getList(page, false);
    }*/

    private void getList(final int page, final SuccessCallback callback) {
        MyRetrofit.getService()
                .listPosts(conversationId, page)
                .enqueue(new retrofit2.Callback<PostListResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<PostListResult> call, retrofit2.Response<PostListResult> response) {
                        callback.doWhenSuccess(response.body().getPosts());
                        removeCircle();
                        //moveToPosition(firstLoad ? 0 : oldLen);
                        //isRefreshing.set(false);

                        //PostActivityVM.this.page = page;
                    }

                    @Override
                    public void onFailure(retrofit2.Call<PostListResult> call, Throwable t) {
                        isRefreshing.set(false);
                        toastContent.set(ChaoliApplication.getAppContext().getString(R.string.network_err));
                        showToast.notifyChange();
                        t.printStackTrace();
                    }
                });
    }
    /*private void getList(final int page, final Boolean refresh) {
        MyRetrofit.getService()
                .listPosts(conversationId, page)
                .enqueue(new retrofit2.Callback<PostListResult>() {
                    @Override
                    public void onResponse(retrofit2.Call<PostListResult> call, retrofit2.Response<PostListResult> response) {
                        Log.d(TAG, "onResponse: " + call.request().url().toString());
                        int oldLen = postList.size();
                        List<Post> newPostList = response.body().getPosts();
                        if (!reversed)
                            MyUtils.expandUnique(postList, newPostList);
                        else
                            postList.addAll(MyUtils.reverse(newPostList));
                        moveToPosition(refresh ? 0 : oldLen);
                        isRefreshing.set(false);

                        PostActivityVM.this.page = page;

                        //direction.set(SwipyRefreshLayoutDirection.BOTTOM);
                    }

                    @Override
                    public void onFailure(retrofit2.Call<PostListResult> call, Throwable t) {
                        isRefreshing.set(false);
                        toastContent.set(ChaoliApplication.getAppContext().getString(R.string.network_err));
                        showToast.notifyChange();
                        t.printStackTrace();
                    }
                });
    }*/

    /*private Call<PostListResult> getList(int page) {
        return MyRetrofit.getService().listPosts(conversationId, page);
    }
*/
    public void reverse() {
        reversed = !reversed;
        firstLoad();
    }

    public void firstLoad() {
        //direction.set(SwipyRefreshLayoutDirection.BOTH);
        if (preview) {
            postList.clear();
            preview = false;
        }
        showCircle();
        postList.clear();
        maxPage = minPage = reversed ? (int) Math.ceil((conversation.getReplies() + 1) / 20.0) : 1;
        getList(maxPage, new SuccessCallback() {
            @Override
            public void doWhenSuccess(List<Post> newPostList) {
                if (reversed) postList.addAll(MyUtils.reverse(newPostList));
                else postList.addAll(newPostList);
            }
        });

    }

    private void moveToPosition(int position) {
        listPosition.set(position);
        listPosition.notifyChange();
    }
    /*public void loadMore() {
        if (reversed && page == 1) {
            Log.d(TAG, "loadMore: ");
            isRefreshing.set(false);
            isRefreshing.notifyChange();
            return;
        }
        int nextPage = page;
        if (reversed) {
            nextPage = page - 1;
        } else
            if (postList.size() >= page * Constants.POST_PER_PAGE)
                nextPage = page + 1;
        getList(nextPage);
        //getList(postList.size() < page * Constants.POST_PER_PAGE ? page : (reversed ? page - 1 : page + 1));
    }*/

    private void loadAfterward() {
        final int nextPage;
        if (postList.size() >= (maxPage - minPage) * Constants.POST_PER_PAGE)
            nextPage = maxPage + 1;
        else
            nextPage = maxPage;
        showCircle();
        //getList(nextPage);
        final int oldLen = postList.size();
        getList(nextPage, new SuccessCallback() {
            @Override
            public void doWhenSuccess(List<Post> newPostList) {
                if (reversed) MyUtils.expandUnique(postList, newPostList, false, true);
                else MyUtils.expandUnique(postList, newPostList);
                if (!reversed) moveToPosition(oldLen);
                maxPage = nextPage;
            }
        });
    }

    private void loadBackward() {
        if (minPage == 1) {
            removeCircle();
            return;
        }
        final int nextPage = minPage - 1;
        showCircle();
        //getList(nextPage);
        final int oldLen = postList.size();
        getList(nextPage, new SuccessCallback() {
            @Override
            public void doWhenSuccess(List<Post> newPostList) {
                if (reversed) postList.addAll(MyUtils.reverse(newPostList));
                else MyUtils.expandUnique(postList, newPostList, false);
                if (reversed) moveToPosition(oldLen);
                minPage = nextPage;
            }
        });
    }

    /**
     * 去掉刷新时的圆圈
     */
    public void removeCircle() {
        isRefreshing.set(false);
        isRefreshing.notifyChange();
    }

    /**
     * 显示刷新时的圆圈
     */
    public void showCircle() {
        isRefreshing.set(true);
        isRefreshing.notifyChange();
    }

    public void pullFromTop() {
        if (isReversed()) loadAfterward();
        else loadBackward();
    }

    public void pullFromBottom() {
        if (isReversed()) loadBackward();
        else loadAfterward();
    }

    public void clickFab() {
        goToReply.notifyChange();
    }

    public void quote(Post post) {
        if (!preview) {
            clickedPost = post;
            goToQuote.notifyChange();
        }
    }

    public void replyComplete() {
        isRefreshing.set(true);
        loadAfterward();
    }

    public void clickAvatar(Post post) {
        clickedPost = post;
        goToHomepage.notifyChange();
    }

    public void setPage(int page) {
        //this.page = page;
        maxPage = minPage = page;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isAuthorOnly() {
        return isAuthorOnly;
    }

    public void setAuthorOnly(boolean authorOnly) {
        isAuthorOnly = authorOnly;
    }

    private interface SuccessCallback {
        void doWhenSuccess(List<Post> newPostList);
    }
}
