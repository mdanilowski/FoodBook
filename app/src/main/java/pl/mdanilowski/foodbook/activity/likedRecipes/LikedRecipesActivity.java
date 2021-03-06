package pl.mdanilowski.foodbook.activity.likedRecipes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

import pl.mdanilowski.foodbook.activity.base.BaseActivity;
import pl.mdanilowski.foodbook.activity.likedRecipes.dagger.DaggerLikedRecipesComponent;
import pl.mdanilowski.foodbook.activity.likedRecipes.dagger.LikedRecipesModule;
import pl.mdanilowski.foodbook.activity.likedRecipes.mvp.LikedRecipesPresenter;
import pl.mdanilowski.foodbook.activity.likedRecipes.mvp.LikedRecipesView;
import pl.mdanilowski.foodbook.app.App;

public class LikedRecipesActivity extends BaseActivity {

    @Inject
    LikedRecipesView view;

    @Inject
    LikedRecipesPresenter presenter;

    public static void start(Context context) {
        Intent intent = new Intent(context, LikedRecipesActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerLikedRecipesComponent.builder()
                .likedRecipesModule(new LikedRecipesModule(this))
                .foodbookAppComponent(App.getApplicationInstance().getFoodbookAppComponent())
                .build().inject(this);
        setContentView(view);
        presenter.onCreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }
}
