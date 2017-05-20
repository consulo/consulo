/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.components;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author nik
 */
public class PathMacroUtil {
  @NonNls public static final String PROJECT_DIR_MACRO_NAME = "PROJECT_DIR";
  @NonNls public static final String MODULE_DIR_MACRO_NAME = "MODULE_DIR";
  @NonNls public static final String APPLICATION_HOME_DIR = "APPLICATION_HOME_DIR";
  @NonNls public static final String USER_HOME_NAME = "USER_HOME";


  public static String getUserHomePath() {
    return StringUtil.trimEnd(FileUtil.toSystemIndependentName(SystemProperties.getUserHome()), "/");
  }

  public static Map<String, String> getGlobalSystemMacros() {
    final Map<String, String> map = new HashMap<String, String>();
    map.put(APPLICATION_HOME_DIR, getApplicationHomeDirPath());
    map.put(USER_HOME_NAME, getUserHomePath());
    return map;
  }

  private static String getApplicationHomeDirPath() {
    return FileUtil.toSystemIndependentName(PathManager.getHomePath());
  }

  @Nullable
  public static String getGlobalSystemMacroValue(String name) {
    if (APPLICATION_HOME_DIR.equals(name)) return getApplicationHomeDirPath();
    if (USER_HOME_NAME.equals(name)) return getUserHomePath();
    return null;
  }
}
