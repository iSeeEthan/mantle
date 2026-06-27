package com.iseeethan.mantle.world;

public final class GenStatus {

    private static volatile String stage = "";
    private static volatile boolean simulating = false;

    private GenStatus() {}

    public static void begin() {
        simulating = true;
        stage = "Preparing simulation";
    }

    public static void stage(String s) {
        stage = s;
    }

    public static void done() {
        simulating = false;
        stage = "";
    }

    public static boolean isSimulating() {
        return simulating;
    }

    public static String stageText() {
        return stage;
    }
}
