package com.chill.with.examsaathi;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/**
 * Light security checks — does not block normal app use or API loading.
 * Only optional hard-block for known traffic-capture apps (off by default).
 */
public final class AppSecurityManager {

    public static final String EXTRA_BLOCK_REASON = "security_block_reason";

    /**
     * When false (default), app always runs and APIs load normally.
     * Set true only if you want to block HttpCanary-style tools on release builds.
     */
    private static final boolean BLOCK_KNOWN_SNIFFER_APPS = false;

    /** SHA-256 of release signing cert (hex). Leave empty to skip signature check. */
    private static final String EXPECTED_RELEASE_CERT_SHA256 = "";

    /** Exact package names only — avoids false positives from substring matching. */
    private static final String[] KNOWN_SNIFFER_PACKAGES = {
            "com.guoshi.httpcanary",
            "com.guoshi.httpcanary.premium",
            "com.minhui.networkcapture",
            "com.minhui.networkcapture.pro",
            "com.egorovandreyrm.pcapremote",
            "com.egorovandreyrm.pcapremotepro",
            "com.reqable.android",
            "app.greyshirts.sslcapture",
    };

    private AppSecurityManager() {}

    /** Always allows API/network for normal users. */
    public static boolean isEnvironmentAllowed(@NonNull Context context) {
        return getBlockReason(context) == 0;
    }

    public static boolean isNetworkAllowed(@NonNull Context context) {
        return true;
    }

    @StringRes
    public static int getBlockReason(@NonNull Context context) {
        if (!BLOCK_KNOWN_SNIFFER_APPS || isDebugBuild(context)) {
            return 0;
        }
        if (hasKnownSnifferApp(context)) {
            return R.string.security_block_accessibility;
        }
        if (!EXPECTED_RELEASE_CERT_SHA256.isEmpty() && !isReleaseSignatureValid(context)) {
            return R.string.security_block_tamper;
        }
        return 0;
    }

    private static boolean isDebugBuild(@NonNull Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static boolean hasKnownSnifferApp(@NonNull Context context) {
        AccessibilityManager manager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null) {
            List<AccessibilityServiceInfo> enabled =
                    manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            if (enabled != null) {
                for (AccessibilityServiceInfo info : enabled) {
                    if (info == null || info.getResolveInfo() == null
                            || info.getResolveInfo().serviceInfo == null) {
                        continue;
                    }
                    String pkg = info.getResolveInfo().serviceInfo.packageName;
                    if (matchesKnownSniffer(pkg)) {
                        return true;
                    }
                }
            }
        }
        String enabledSetting = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledSetting != null) {
            String lower = enabledSetting.toLowerCase(Locale.US);
            for (String pkg : KNOWN_SNIFFER_PACKAGES) {
                if (lower.contains(pkg.toLowerCase(Locale.US))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesKnownSniffer(@Nullable String packageName) {
        if (packageName == null) {
            return false;
        }
        String lower = packageName.toLowerCase(Locale.US);
        for (String known : KNOWN_SNIFFER_PACKAGES) {
            if (lower.equals(known) || lower.startsWith(known + ".")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isReleaseSignatureValid(@NonNull Context context) {
        if (EXPECTED_RELEASE_CERT_SHA256.isEmpty()) {
            return true;
        }
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = pm.getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo == null) {
                    return false;
                }
                Signature[] signatures = info.signingInfo.getApkContentsSigners();
                if (signatures == null) {
                    return false;
                }
                for (Signature signature : signatures) {
                    if (EXPECTED_RELEASE_CERT_SHA256.equalsIgnoreCase(sha256Hex(signature.toByteArray()))) {
                        return true;
                    }
                }
                return false;
            }
            info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            if (info.signatures == null) {
                return false;
            }
            for (Signature signature : info.signatures) {
                if (EXPECTED_RELEASE_CERT_SHA256.equalsIgnoreCase(sha256Hex(signature.toByteArray()))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString();
    }
}
