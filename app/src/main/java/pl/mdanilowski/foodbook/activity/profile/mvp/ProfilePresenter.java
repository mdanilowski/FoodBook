package pl.mdanilowski.foodbook.activity.profile.mvp;

import android.widget.Toast;

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
        User userFromIntent = model.getUserFromIntent();
        if (userFromIntent == null)
            compositeSubscription.add(observeGetUser());
        else view.setUserData(foodBookSimpleStorage.getUser(), userFromIntent);
    }

    @Override
    public void onDestroy() {

    }

    private Subscription observeGetUser() {
        return foodBookService.findUserByUid(model.getUserUid())
                .subscribe(retrievedUser -> {
                    user = retrievedUser;
                    view.setUserData(foodBookSimpleStorage.getUser(), user);
                }, throwable -> {
                    Toast.makeText(view.getContext(), "Can't get user", Toast.LENGTH_SHORT).show();
                    throwable.printStackTrace();
                });
    }
}
