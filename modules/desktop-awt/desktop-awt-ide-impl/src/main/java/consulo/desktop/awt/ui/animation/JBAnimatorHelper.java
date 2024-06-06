// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.animation;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.logging.Logger;
import consulo.platform.Platform;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class JBAnimatorHelper {

  private static final String PROPERTY_NAME = "WIN_MM_LIB_HIGH_PRECISION_TIMER";
  private static final boolean DEFAULT_VALUE = ApplicationManager.getApplication().isInternal() && Platform.current().os().isWindows();
  private static final int PERIOD = 1;

  private final @Nonnull
  Set<JBAnimator> requestors;
  private final @Nonnull
  WinMM lib;

  private static
  @Nullable
  Throwable exceptionInInitialization = null;

  private static JBAnimatorHelper getInstance() {
    return JBAnimatorHelperHolder.INSTANCE;
  }

  /**
   * Used internally only, do not call it until it's really necessary.
   */
  public static void requestHighPrecisionTimer(@Nonnull JBAnimator requestor) {
    if (isAvailable()) {
      var helper = getInstance();
      if (helper.requestors.add(requestor)) {
        helper.lib.timeBeginPeriod(PERIOD);
      }
    }
  }

  /**
   * Used internally only, do not call it until it's really necessary.
   */
  public static void cancelHighPrecisionTimer(@Nonnull JBAnimator requestor) {
    if (isAvailable()) {
      var helper = getInstance();
      if (helper.requestors.remove(requestor)) {
        helper.lib.timeEndPeriod(PERIOD);
      }
    }
  }

  public static boolean isAvailable() {
    if (!Platform.current().os().isWindows() || exceptionInInitialization != null) {
      return false;
    }
    return PropertiesComponent.getInstance().getBoolean(PROPERTY_NAME, DEFAULT_VALUE);
  }

  public static void setAvailable(boolean value) {
    if (exceptionInInitialization != null) {
      Logger.getInstance(JBAnimatorHelper.class).error(exceptionInInitialization);
    }
    if (!Platform.current().os().isWindows()) {
      throw new IllegalArgumentException("This option can be set only on Windows");
    }
    PropertiesComponent.getInstance().setValue(PROPERTY_NAME, value, DEFAULT_VALUE);
    var helper = getInstance();
    if (!helper.requestors.isEmpty()) {
      helper.requestors.clear();
      helper.lib.timeEndPeriod(PERIOD);
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
    requestors = ConcurrentHashMap.newKeySet();
    WinMM library = null;
    try {
      if (Platform.current().os().isWindows()) {
        library = Native.load("winmm", WinMM.class);
      }
    }
    catch (Throwable t) {
      //should be called only once
      //noinspection AssignmentToStaticFieldFromInstanceMethod,InstanceofCatchParameter
      exceptionInInitialization = new RuntimeException(
        "Cannot load 'winmm.dll' library",
        t instanceof UnsatisfiedLinkError ? null : t
      );
    }
    lib = library != null ? library : new WinMM() {
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