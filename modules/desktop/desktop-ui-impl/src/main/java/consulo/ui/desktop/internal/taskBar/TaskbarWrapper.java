/*
 * Copyright 2013-2018 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.desktop.internal.taskBar;

import com.intellij.openapi.util.ThrowableComputable;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 8/29/18
 */
public class TaskbarWrapper {
  private static Class ourTaskbarClass;
  private static Method ourTaskbar;
  private static Method ourIsTaskbarSupported;
  private static Method ourIsSupported;
  private static Method ourSetMenu;
  private static Method ourSetProgressValue;
  private static Method ourRequestUserAttention;

  private static Object ourFeature_MENU;
  private static Object ourFeature_PROGRESS_VALUE;
  private static Object ourFeature_USER_ATTENTION;

  static {
    try {
      ourTaskbarClass = Class.forName("java.awt.Taskbar");
      ourIsTaskbarSupported = ourTaskbarClass.getDeclaredMethod("isTaskbarSupported");
      ourTaskbar = ourTaskbarClass.getDeclaredMethod("getTaskbar");

      Class<?> featureClass = Class.forName(ourTaskbarClass.getName() + "$Feature");
      ourFeature_MENU = featureClass.getField("MENU").get(null);
      ourFeature_PROGRESS_VALUE = featureClass.getField("PROGRESS_VALUE").get(null);
      ourFeature_USER_ATTENTION = featureClass.getField("USER_ATTENTION").get(null);

      ourIsSupported = ourTaskbarClass.getDeclaredMethod("isSupported", featureClass);

      ourSetMenu = ourTaskbarClass.getDeclaredMethod("setMenu", PopupMenu.class);
      ourSetProgressValue = ourTaskbarClass.getDeclaredMethod("setProgressValue", int.class);
      ourRequestUserAttention = ourTaskbarClass.getDeclaredMethod("requestUserAttention", boolean.class, boolean.class);
    }
    catch (Throwable e) {
      ourTaskbarClass = null;
    }
  }

  public static enum FeatureWrapper {
    MENU(() -> ourFeature_MENU),
    PROGRESS_VALUE(() -> ourFeature_PROGRESS_VALUE),
    USER_ATTENTION(() -> ourFeature_USER_ATTENTION);

    private Supplier<Object> myValue;

    FeatureWrapper(Supplier<Object> value) {
      myValue = value;
    }

    Object get() {
      return myValue.get();
    }
  }

  public static TaskbarWrapper getTaskbar() {
    return new TaskbarWrapper();
  }

  public static boolean isTaskbarSupported() {
    if (ourTaskbarClass == null) {
      return false;
    }

    return call(() -> (boolean)ourIsTaskbarSupported.invoke(null), false);
  }

  private Object myTaskbarObject;

  private TaskbarWrapper() {
    myTaskbarObject = call(() -> ourTaskbar.invoke(null), null);
  }

  public boolean isSupported(FeatureWrapper featureWrapper) {
    return call(() -> (boolean)ourIsSupported.invoke(myTaskbarObject, featureWrapper.get()), false);
  }

  public void setMenu(PopupMenu popupMenu) {
    call(() -> ourSetMenu.invoke(myTaskbarObject, popupMenu), null);
  }

  public void setProgressValue(int value) {
    call(() -> ourSetProgressValue.invoke(myTaskbarObject, value), null);
  }

  public void requestUserAttention(boolean enabled, boolean critical) {
    call(() -> ourSetProgressValue.invoke(myTaskbarObject, enabled, critical), null);
  }

  private static <T> T call(ThrowableComputable<T, Throwable> computable, T defaultValue) {
    if (ourTaskbarClass == null) {
      return defaultValue;
    }

    try {
      return computable.compute();
    }
    catch (Throwable throwable) {
      throwable.printStackTrace();
      return defaultValue;
    }
  }
}