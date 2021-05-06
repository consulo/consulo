/*
 * Copyright 2013-2020 consulo.io
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
package consulo.awt.hacking;

import consulo.logging.Logger;

import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.lang.reflect.Method;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class BasicLookAndFeelHacking {
  private static final Logger LOG = Logger.getInstance(BasicLookAndFeelHacking.class);

  public static void initComponentDefaults(BasicLookAndFeel basicLookAndFeel, UIDefaults uiDefaults) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("initComponentDefaults", UIDefaults.class);
      superMethod.setAccessible(true);
      superMethod.invoke(basicLookAndFeel, uiDefaults);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  public static void initSystemColorDefaults(BasicLookAndFeel basicLookAndFeel, UIDefaults uiDefaults) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("initSystemColorDefaults", UIDefaults.class);
      superMethod.setAccessible(true);
      superMethod.invoke(basicLookAndFeel, uiDefaults);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  public static void initClassDefaults(BasicLookAndFeel basicLookAndFeel, UIDefaults uiDefaults) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("initClassDefaults", UIDefaults.class);
      superMethod.setAccessible(true);
      superMethod.invoke(basicLookAndFeel, uiDefaults);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  public static void initialize(BasicLookAndFeel basicLookAndFeel) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("initialize");
      superMethod.setAccessible(true);
      superMethod.invoke(basicLookAndFeel);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  public static void uninitialize(BasicLookAndFeel basicLookAndFeel) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("uninitialize");
      superMethod.setAccessible(true);
      superMethod.invoke(basicLookAndFeel);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  public static void loadSystemColors(BasicLookAndFeel basicLookAndFeel, UIDefaults defaults, String[] systemColors, boolean useNative) {
    try {
      final Method superMethod = BasicLookAndFeel.class.getDeclaredMethod("loadSystemColors", UIDefaults.class, String[].class, boolean.class);
      superMethod.setAccessible(true);
      superMethod.invoke(basicLookAndFeel, defaults, systemColors, useNative);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }
}
