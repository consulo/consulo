// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.impl.internal.animation;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import consulo.application.ApplicationManager;
import consulo.application.ApplicationPropertiesComponent;
import consulo.logging.Logger;
import consulo.platform.Platform;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class JBAnimatorHelper {
    private static final String PROPERTY_NAME = "WIN_MM_LIB_HIGH_PRECISION_TIMER";
    private static final boolean DEFAULT_VALUE =
        ApplicationManager.getApplication().isInternal() && Platform.current().os().isWindows();
    private static final int PERIOD = 1;

    private final Set<JBAnimator> myRequestors;
    private final WinMM myLib;

    private static @Nullable Throwable ourExceptionInInitialization = null;

    private static JBAnimatorHelper getInstance() {
        return JBAnimatorHelperHolder.INSTANCE;
    }

    /**
     * Used internally only, do not call it until it's really necessary.
     */
    public static void requestHighPrecisionTimer(JBAnimator requestor) {
        if (isAvailable()) {
            JBAnimatorHelper helper = getInstance();
            if (helper.myRequestors.add(requestor)) {
                helper.myLib.timeBeginPeriod(PERIOD);
            }
        }
    }

    /**
     * Used internally only, do not call it until it's really necessary.
     */
    public static void cancelHighPrecisionTimer(JBAnimator requestor) {
        if (isAvailable()) {
            JBAnimatorHelper helper = getInstance();
            if (helper.myRequestors.remove(requestor)) {
                helper.myLib.timeEndPeriod(PERIOD);
            }
        }
    }

    public static boolean isAvailable() {
        if (!Platform.current().os().isWindows() || ourExceptionInInitialization != null) {
            return false;
        }
        return ApplicationPropertiesComponent.getInstance().getBoolean(PROPERTY_NAME, DEFAULT_VALUE);
    }

    public static void setAvailable(boolean value) {
        if (ourExceptionInInitialization != null) {
            Logger.getInstance(JBAnimatorHelper.class).error(ourExceptionInInitialization);
        }
        if (!Platform.current().os().isWindows()) {
            throw new IllegalArgumentException("This option can be set only on Windows");
        }
        ApplicationPropertiesComponent.getInstance().setValue(PROPERTY_NAME, value, DEFAULT_VALUE);
        JBAnimatorHelper helper = getInstance();
        if (!helper.myRequestors.isEmpty()) {
            helper.myRequestors.clear();
            helper.myLib.timeEndPeriod(PERIOD);
        }
    }

    private interface WinMM extends StdCallLibrary {
        int timeBeginPeriod(int period);

        int timeEndPeriod(int period);
    }

    private static class JBAnimatorHelperHolder {
        private static final JBAnimatorHelper INSTANCE = new JBAnimatorHelper();
    }

    private JBAnimatorHelper() {
        myRequestors = ConcurrentHashMap.newKeySet();
        WinMM library = null;
        try {
            if (Platform.current().os().isWindows()) {
                library = Native.load("winmm", WinMM.class);
            }
        }
        catch (Throwable t) {
            //should be called only once
            //noinspection AssignmentToStaticFieldFromInstanceMethod,InstanceofCatchParameter
            ourExceptionInInitialization = new RuntimeException(
                "Cannot load 'winmm.dll' library",
                t instanceof UnsatisfiedLinkError ? null : t
            );
        }
        myLib = library != null ? library : new WinMM() {
            @Override
            public int timeBeginPeriod(int period) {
                return 0;
            }

            @Override
            public int timeEndPeriod(int period) {
                return 0;
            }
        };
    }
}
