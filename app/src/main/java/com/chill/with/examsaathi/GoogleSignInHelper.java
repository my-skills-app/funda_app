package com.chill.with.examsaathi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/** Firebase Auth + Google account picker for SignInActivity. */
public final class GoogleSignInHelper {

    private static final String TAG = "GoogleSignIn";

    public interface Callback {
        void onSuccess();

        void onCancelled();

        void onError(@NonNull String message);
    }

    private GoogleSignInHelper() {}

    @NonNull
    public static GoogleSignInClient createClient(@NonNull Context context) {
        String webClientId = resolveWebClientId(context);
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(context, options);
    }

    @NonNull
    private static String resolveWebClientId(@NonNull Context context) {
        try {
            int resId = context.getResources().getIdentifier("default_web_client_id", "string", context.getPackageName());
            if (resId != 0) {
                String generated = context.getString(resId);
                if (generated != null && !generated.trim().isEmpty()) {
                    return generated.trim();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "default_web_client_id missing, using fallback", e);
        }
        return context.getString(R.string.google_web_client_id);
    }

    public static void launchSignIn(
            @NonNull Activity activity,
            @NonNull androidx.activity.result.ActivityResultLauncher<Intent> launcher) {
        GoogleSignInClient client = createClient(activity);
        launcher.launch(client.getSignInIntent());
    }

    public static void handleSignInResult(
            int resultCode,
            @Nullable Intent data,
            @NonNull Activity activity,
            @NonNull Callback callback) {
        if (resultCode == Activity.RESULT_CANCELED && data == null) {
            callback.onCancelled();
            return;
        }
        if (data == null) {
            callback.onCancelled();
            return;
        }
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null || account.getIdToken() == null) {
                Log.e(TAG, "Google account or idToken is null");
                callback.onError(activity.getString(R.string.google_sign_in_failed));
                return;
            }
            signInWithGoogleToken(activity, account, callback);
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in ApiException, status=" + e.getStatusCode(), e);
            if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                callback.onCancelled();
                return;
            }
            if (e.getStatusCode() == GoogleSignInStatusCodes.DEVELOPER_ERROR) {
                callback.onError(buildConfigErrorMessage(activity));
                return;
            }
            callback.onError(activity.getString(R.string.google_sign_in_failed)
                    + " (code " + e.getStatusCode() + ")");
        }
    }

    private static void signInWithGoogleToken(
            @NonNull Activity activity,
            @NonNull GoogleSignInAccount account,
            @NonNull Callback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .addOnCompleteListener(activity, authTask -> {
                    if (!authTask.isSuccessful()) {
                        Exception ex = authTask.getException();
                        Log.e(TAG, "Firebase signInWithCredential failed", ex);
                        callback.onError(mapFirebaseAuthError(activity, ex));
                        return;
                    }
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        callback.onError(activity.getString(R.string.google_sign_in_failed));
                        return;
                    }
                    syncUserProfile(activity, user, account, callback);
                });
    }

    @NonNull
    private static String buildConfigErrorMessage(@NonNull Activity activity) {
        String base = activity.getString(R.string.google_sign_in_config_error);
        String sha1 = SigningCertHelper.getSha1Fingerprint(activity);
        if (sha1.isEmpty()) {
            return base;
        }
        return base + "\n\n" + activity.getString(R.string.google_sign_in_sha1_hint, sha1);
    }

    @NonNull
    private static String mapFirebaseAuthError(@NonNull Activity activity, @Nullable Exception ex) {
        if (ex == null) {
            return activity.getString(R.string.google_sign_in_failed);
        }
        String message = ex.getMessage();
        if (ex instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) ex).getErrorCode();
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(code)) {
                return activity.getString(R.string.google_sign_in_not_enabled);
            }
        }
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("not allowed") || lower.contains("operation is not allowed")) {
                return activity.getString(R.string.google_sign_in_not_enabled);
            }
        }
        return message != null ? message : activity.getString(R.string.google_sign_in_failed);
    }

    private static void syncUserProfile(
            @NonNull Context context,
            @NonNull FirebaseUser user,
            @NonNull GoogleSignInAccount account,
            @NonNull Callback callback) {
        String uid = user.getUid();
        String name = firstNonEmpty(
                account.getDisplayName(),
                user.getDisplayName(),
                context.getString(R.string.seeker));
        String email = firstNonEmpty(account.getEmail(), user.getEmail(), "");
        String photo = "";
        if (account.getPhotoUrl() != null) {
            photo = account.getPhotoUrl().toString();
        } else if (user.getPhotoUrl() != null) {
            photo = user.getPhotoUrl().toString();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", name);
        if (!email.isEmpty()) {
            map.put("email", email);
        }
        final String profileUrl = photo.isEmpty() ? null : photo;
        if (profileUrl != null) {
            map.put("profile", profileUrl);
        }

        final String displayName = name;
        UserProfileCache.save(context, uid, displayName, profileUrl);
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .updateChildren(map)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "users profile sync failed (login still OK)", task.getException());
                    }
                    PushNotificationHelper.registerToken(context);
                    PushBroadcastListener.start(context);
                    callback.onSuccess();
                });
    }

    @NonNull
    private static String firstNonEmpty(@Nullable String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
