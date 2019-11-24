/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.ApplicationInfo;
import consulo.annotation.DeprecationInfo;

@Deprecated
@DeprecationInfo("Use ApplicationInfo")
public class ApplicationInfoImpl {

  @Deprecated
  public static ApplicationInfo getInstance() {
    return ApplicationInfo.getInstance();
  }

  @Deprecated
  public static ApplicationInfo getShadowInstance() {
    return ApplicationInfo.getInstance();
  }

  private static volatile boolean myInPerformanceTest;

  public static boolean isInPerformanceTest() {
    return myInPerformanceTest;
  }

  public static boolean isInStressTest() {
    return false;
  }

  public static void setInPerformanceTest(boolean inPerformanceTest) {
    myInPerformanceTest = inPerformanceTest;
  }
}
