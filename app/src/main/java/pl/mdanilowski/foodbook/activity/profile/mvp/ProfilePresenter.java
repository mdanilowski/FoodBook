package pl.mdanilowski.foodbook.activity.profile.mvp;

import android.widget.Toast;

import pl.mdanilowski.foodbook.R;
import pl.mdanilowski.foodbook.activity.base.BasePresenter;
import pl.mdanilowski.foodbook.app.App;
import pl.mdanilowski.foodbook.model.User;
import rx.Subscription;

public class ProfilePresenter extends BasePresenter {

    private ProfileModel model;
    private ProfileView view;
    private User user;

    public ProfilePresenter(ProfileModel model, ProfileView view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void onCreate() {
        App.getApplicationInstance().getFoodbookAppComponent().inject(this);
        user = model.getUserFromIntent();
        if (user == null)
            compositeSubscription.add(observeGetUser());
        else view.setUserData(foodBookSimpleStorage.getUser(), user);
        compositeSubscription.add(observeRecipesClick());
        compositeSubscription.add(observeFollowersClick());
        compositeSubscription.add(observeFollow());
        compositeSubscription.add(observeStopFollowing());
    }

    @Override
    public void onDestroy() {
        compositeSubscription.clear();
    }

    private Subscription observeGetUser() {
        return foodBookService.getUserRealtime(model.getUserUid())
                .subscribe(retrievedUser -> {
                    user = retrievedUser;
                    view.setUserData(foodBookSimpleStorage.getUser(), user);
                }, Throwable::printStackTrace);
    }

    private Subscription observeRecipesClick() {
        return view.clicksRecipes().subscribe(__ -> model.startUsersRecipesActivity(user));
    }

    private Subscription observeFollowersClick() {
        return view.clicksFollowers().subscribe(__ -> model.startFollowersActivity(user.getUid()));
    }

    private Subscription observeFollow() {
        return view.clicksFollow()
                .doOnNext(__ -> view.hideFollow())
                .subscribe(__ ->
                        foodBookService.followUserBatch(user, foodBookSimpleStorage.getUser())
                                .subscribe(aVoid ->
                                                Toast.makeText(view.getContext(), R.string.following_new_user, Toast.LENGTH_SHORT).show(),
                                        e -> {
                                            view.showFollow();
                                            Toast.makeText(view.getContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                                        }));
    }

    private Subscription observeStopFollowing() {
        return view.clicksStopFollowing()
                .doOnNext(__ -> view.showFollow())
                .subscribe(__ -> foodBookService.unfollowUserBatch(user.getUid(), foodBookSimpleStorage.getUser().getUid()).subscribe(aVoid ->
                                Toast.makeText(view.getContext(), R.string.stopped_following_user, Toast.LENGTH_SHORT).show(),
                        e -> {
                            view.showFollow();
                            Toast.makeText(view.getContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                        }));
    }
}
