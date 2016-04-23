package licola.demo.com.huabandemo.MyFollowing;


import android.content.Context;
import android.view.View;

import java.util.List;

import butterknife.BindString;
import de.greenrobot.event.EventBus;
import licola.demo.com.huabandemo.API.OnPinsFragmentInteractionListener;
import licola.demo.com.huabandemo.API.OnRefreshFragmentInteractionListener;
import licola.demo.com.huabandemo.Adapter.RecyclerPinsHeadCardAdapter;
import licola.demo.com.huabandemo.Base.BaseRecyclerHeadFragment;
import licola.demo.com.huabandemo.Bean.PinsAndUserEntity;
import licola.demo.com.huabandemo.HttpUtils.RetrofitService;
import licola.demo.com.huabandemo.R;
import licola.demo.com.huabandemo.Util.Logger;
import licola.demo.com.huabandemo.Util.NetUtils;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by LiCola on  2016/04/04  14:46
 */
public class MyAttentionPinsFragment
        extends BaseRecyclerHeadFragment<RecyclerPinsHeadCardAdapter,
        List<PinsAndUserEntity>> {
    //联网关键参数
    private int mMaxId;//下一次联网的pinsId开始

    private OnPinsFragmentInteractionListener mListener;
    private OnRefreshFragmentInteractionListener mRefreshListener;

    @BindString(R.string.snack_message_not_notify)
    String mStringNotNotify;

    @Override
    protected String getTAG() {
        return this.toString();
    }

    public static MyAttentionPinsFragment newInstance() {

        return new MyAttentionPinsFragment();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if ((context instanceof OnRefreshFragmentInteractionListener)
                && (context instanceof OnPinsFragmentInteractionListener)) {
            mListener = (OnPinsFragmentInteractionListener) context;
            mRefreshListener = (OnRefreshFragmentInteractionListener) context;
        } else {
            throwRuntimeException(context);
        }

        if (context instanceof MyAttentionActivityNew) {
            mAuthorization = ((MyAttentionActivityNew) context).mAuthorization;
        }
    }

    @Override
    protected Subscription getHttpFirst() {
        return RetrofitService.createAvatarService()
                .httpsMyFollowingPinsRx(mAuthorization, mLimit)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(FollowingPinsBean::getPins)
                .filter(getFilterFunc1())
                .subscribe(new Action1<List<PinsAndUserEntity>>() {
                    @Override
                    public void call(List<PinsAndUserEntity> pinsAndUserEntities) {
                        if (checkNotify(pinsAndUserEntities)) {
                            Logger.d();
                            mAdapter.setListNotify(pinsAndUserEntities);
                            mMaxId = getMaxId(pinsAndUserEntities);
                        } else {
                            Logger.d("not notify");
                            NetUtils.showSnackBar(mRootView, mStringNotNotify);
                        }

                    }
                }, getErrorAction(), getCompleteAction());
    }

    private Action1<Throwable> getErrorAction() {
        return throwable -> {
            Logger.d(throwable.toString());
            checkException(throwable);
            mRefreshListener.OnRefreshState(false);
        };
    }

    private Action0 getCompleteAction() {
        return () -> {
            Logger.d();
            mRefreshListener.OnRefreshState(false);
        };
    }

    private boolean checkNotify(List<PinsAndUserEntity> result) {
        if (!mAdapter.getList().isEmpty()) {
            if (mAdapter.getList().get(0).getFile().getKey().equals(result.get(0).getFile().getKey())) {
                return false;
            }
        }
        return true;
    }

    private int getMaxId(List<PinsAndUserEntity> result) {
        return result.get(result.size() - 1).getPin_id();
    }

    @Override
    protected Subscription getHttpScroll() {
        return RetrofitService.createAvatarService()
                .httpsMyFollowingPinsMaxRx(mAuthorization, mMaxId, mLimit)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<FollowingPinsBean, List<PinsAndUserEntity>>() {
                    @Override
                    public List<PinsAndUserEntity> call(FollowingPinsBean followingPinsBean) {
                        return followingPinsBean.getPins();
                    }
                })
                .filter(getFilterFunc1())
                .subscribe(new Subscriber<List<PinsAndUserEntity>>() {
                    @Override
                    public void onCompleted() {
                        Logger.d();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Logger.d(e.toString());
                        checkException(e);
                    }

                    @Override
                    public void onNext(List<PinsAndUserEntity> pinsAndUserEntities) {
                        mAdapter.addListNotify(pinsAndUserEntities);
                        mMaxId = getMaxId(pinsAndUserEntities);
                    }
                });
    }

    @Override
    protected void initListener() {
        mAdapter.setOnClickItemListener(new RecyclerPinsHeadCardAdapter.OnAdapterListener() {
            @Override
            public void onClickImage(PinsAndUserEntity bean, View view) {
                EventBus.getDefault().postSticky(bean);
                mListener.onClickPinsItemImage(bean, view);
            }

            @Override
            public void onClickTitleInfo(PinsAndUserEntity bean, View view) {
                EventBus.getDefault().postSticky(bean);
                mListener.onClickPinsItemText(bean, view);
            }

            @Override
            public void onClickInfoGather(PinsAndUserEntity bean, View view) {
                Logger.d();
                //todo 收集时间 类内部处理不传递
            }

            @Override
            public void onClickInfoLike(PinsAndUserEntity bean, View view) {
                Logger.d();
            }
        });
    }


    @Override
    protected View getHeadView() {
        return null;
    }

    @Override
    protected int getAdapterPosition() {
        return mAdapter.getAdapterPosition();
    }

    @Override
    protected RecyclerPinsHeadCardAdapter setAdapter() {
        return new RecyclerPinsHeadCardAdapter(mRecyclerView);
    }


}
