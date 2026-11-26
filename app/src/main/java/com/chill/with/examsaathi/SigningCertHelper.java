package com.chill.with.examsaathi;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.MessageDigest;
import java.util.Locale;

/** SHA-1 of the installed APK signing cert (for Firebase / Google Sign-In setup). */
public final class SigningCertHelper {

    private SigningCertHelper() {}

    @NonNull
    public static String getSha1Fingerprint(@NonNull Context context) {
        String hex = sha1Hex(context);
        if (hex == null || hex.isEmpty()) {
            return "";
        }
        return formatColonSeparated(hex);
    }

    @Nullable
    private static String sha1Hex(@NonNull Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = pm.getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo == null) {
                    return null;
                }
                Signature[] signatures = info.signingInfo.getApkContentsSigners();
                if (signatures == null || signatures.length == 0) {
                    return null;
                }
                return digestSha1(signatures[0].toByteArray());
            }
            info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            if (info.signatures == null || info.signatures.length == 0) {
                return null;
            }
            return digestSha1(info.signatures[0].toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static String digestSha1(byte[] cert) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(cert);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format(Locale.US, "%02X", b));
        }
        return sb.toString();
    }

    @NonNull
    private static String formatColonSeparated(@NonNull String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(hex, i, Math.min(i + 2, hex.length()));
        }
        return sb.toString();
    }
}
