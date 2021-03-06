package pl.mdanilowski.foodbook.activity.login.mvp;


import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

import pl.mdanilowski.foodbook.R;
import pl.mdanilowski.foodbook.activity.base.BasePresenter;
import pl.mdanilowski.foodbook.app.App;
import rx.Subscription;

public class LoginPresenter extends BasePresenter implements GoogleApiClient.OnConnectionFailedListener {

    private LoginView view;
    private LoginModel model;

    private FirebaseAuth firebaseAuth;
    private GoogleApiClient googleApiClient;
    private static final String TAG = "GoogleActivity";
    public static final int RC_SIGN_IN = 9001;

    private CallbackManager callbackManager;

    public LoginPresenter(LoginView view, LoginModel model, FirebaseAuth firebaseAuth) {
        this.view = view;
        this.model = model;
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public void onCreate() {
        compositeSubscription.add(observeSignInGoogleClick());
        compositeSubscription.add(observeSignInFacebookAttachCallack());
        compositeSubscription.add(observeLoginClicked());

        setUpGoogleSignInUtils();
        setUpFacebookSignInUtils();
    }

    @Override
    public void onDestroy() {
        compositeSubscription.clear();
    }

    private void setUpGoogleSignInUtils() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(App.getApplicationInstance().getResources().getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(model.getActivity())
                .enableAutoManage(model.getActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void setUpFacebookSignInUtils() {
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                        Log.d("Success", "Login");
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(model.getActivity(), "Anulowano logowanie", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(model.getActivity(), exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private Subscription observeSignInGoogleClick() {
        return view.googleLoginClick().subscribe(__ -> signInGoogle());
    }

    private Subscription observeSignInFacebookAttachCallack() {
        return view.setUpFacebookSignInButton().subscribe(__ ->
                LoginManager.getInstance().logInWithReadPermissions(model.getActivity(), Arrays.asList("public_profile", "user_friends", "email")));
    }

    private Subscription observeLoginClicked() {
        return view.loginClicked().subscribe(__ -> {
            if (validateLoginForm()) {
                String email = String.valueOf(view.etEmail.getText());
                String password = String.valueOf(view.etPassword.getText());
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(model.getActivity(), new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d("REGISTERED", "signInWithEmail:success");
                                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                    Log.d("REG_ID", firebaseUser.getUid());
                                    model.startDashboardActivity();
                                } else {
                                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                        Toast.makeText(view.getContext(), R.string.email_in_use, Toast.LENGTH_SHORT).show();
                                    } else if (task.getException() instanceof FirebaseNetworkException) {
                                        Toast.makeText(view.getContext(), R.string.user_offline, Toast.LENGTH_SHORT).show();
                                    } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                        Toast.makeText(view.getContext(), R.string.invalid_password, Toast.LENGTH_SHORT).show();
                                    } else if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                        Toast.makeText(view.getContext(), R.string.invalid_email, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(view.getContext(), R.string.auth_failed, Toast.LENGTH_SHORT).show();
                                    }
                                    Log.w("ERROR_REG", "signInWithEmail:failure", task.getException());
                                }
                            }
                        });
            }
        });
    }

    private boolean validateLoginForm() {
        boolean isValid = true;
        if (TextUtils.isEmpty(view.etEmail.getText())) {
            isValid = false;
            view.displayInputEmptyError(view.tilEmail);
        } else {
            view.hideInputEmptyError(view.tilEmail);
        }
        if (TextUtils.isEmpty(view.etPassword.getText())) {
            isValid = false;
            view.displayInputEmptyError(view.tilPassword);
        } else {
            view.hideInputEmptyError(view.tilPassword);
        }
        return isValid;
    }

    //Google login code
    private void signInGoogle() {
        Intent singInGoogleIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        model.getActivity().startActivityForResult(singInGoogleIntent, RC_SIGN_IN);
    }

    public void onSignInGoogleActivityResult(int requestCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                Log.d("ERROR", result.getStatus().toString());
                Toast.makeText(model.getActivity(), R.string.auth_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        ProgressDialog progressDialog = new ProgressDialog(model.getActivity());
        progressDialog.setMessage(view.getResources().getString(R.string.logging_via_google));
        progressDialog.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(model.getActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        model.startDashboardActivity();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(model.getActivity(), R.string.auth_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.hide();
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(model.getActivity(), R.string.play_services_error, Toast.LENGTH_SHORT).show();
    }

    //Facebook login code
    public void onFacebookLoginActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        ProgressDialog progressDialog = new ProgressDialog(model.getActivity());
        progressDialog.setMessage(view.getResources().getString(R.string.logging_via_facebook));
        progressDialog.show();

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(model.getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            model.startDashboardActivity();
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(model.getActivity(), R.string.auth_failed,
                                    Toast.LENGTH_SHORT).show();
                        }
                        progressDialog.hide();
                    }
                });
    }
}
