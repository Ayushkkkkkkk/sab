package com.sab.pawcare.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private AppExecutors() {}
}
