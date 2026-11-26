package com.chill.with.examsaathi;

/** XOR-obfuscated literals — not stored as plain text in the APK. */
final class StringVault {

    private static final int K = 0x5A;

    private static final int[] B_BASE = {
            50, 46, 46, 42, 41, 96, 117, 117, 57, 50, 51, 54, 54, 45, 51, 46, 50, 116, 63, 34,
            59, 55, 41, 59, 59, 46, 50, 51, 116, 41, 51, 46, 63
    };
    private static final int[] P_DASHBOARD = {117, 62, 59, 41, 50, 56, 53, 59, 40, 62};
    private static final int[] P_CHAPTERS = {117, 57, 50, 59, 42, 46, 63, 40, 41};
    private static final int[] P_TRACK = {117, 59, 42, 51, 117, 46, 40, 59, 57, 49};
    private static final int[] P_BY_ID = {117, 57, 50, 59, 42, 46, 63, 40, 41, 117, 56, 35, 119, 51, 62, 117};
    private static final int[] P_SHLOK = {117, 41, 50, 54, 53, 49, 101, 46, 51, 46, 54, 63, 103};
    private static final int[] K_API = {
            24, 61, 29, 51, 46, 59, 27, 42, 42, 17, 63, 35, 104, 106, 104, 108
    };

    private StringVault() {}

    static String baseUrl() {
        return decode(B_BASE);
    }

    static String dashboardPath() {
        return decode(P_DASHBOARD);
    }

    static String chaptersPath() {
        return decode(P_CHAPTERS);
    }

    static String trackPath() {
        return decode(P_TRACK);
    }

    static String chapterShlokasPrefix() {
        return decode(P_BY_ID);
    }

    static String shlokQueryPrefix() {
        return decode(P_SHLOK);
    }

    static String apiKey() {
        return decode(K_API);
    }

    private static String decode(int[] enc) {
        char[] out = new char[enc.length];
        for (int i = 0; i < enc.length; i++) {
            out[i] = (char) (enc[i] ^ K);
        }
        return new String(out);
    }
}
