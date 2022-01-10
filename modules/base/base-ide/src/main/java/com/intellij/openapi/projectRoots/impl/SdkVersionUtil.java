/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: Anna.Kozlova
 * Date: 12-Aug-2006
 * Time: 21:25:38
 */
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.concurrent.Future;

@Deprecated
public class SdkVersionUtil {
  private static final NotNullFunction<Runnable, Future<?>> ACTION_RUNNER = new NotNullFunction<Runnable, Future<?>>() {
    @Override
    @Nonnull
    public Future<?> fun(Runnable runnable) {
      return ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }
  };

  private SdkVersionUtil() {
  }

  @javax.annotation.Nullable
  public static String readVersionFromProcessOutput(String homePath, @NonNls String[] command, @NonNls String versionLineMarker) {
    return JdkVersionDetector.getInstance().readVersionFromProcessOutput(homePath, command, versionLineMarker, ACTION_RUNNER);
  }

  @javax.annotation.Nullable
  public static String detectJdkVersion(String homePath) {
    return JdkVersionDetector.getInstance().detectJdkVersion(homePath, ACTION_RUNNER);
  }
}
