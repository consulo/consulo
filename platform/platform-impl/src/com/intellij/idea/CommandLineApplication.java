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
package com.intellij.idea;

import com.intellij.ide.impl.DataManagerImpl;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.impl.local.FileWatcher;
import com.intellij.ui.Splash;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

@Deprecated
public class CommandLineApplication {
  private static final Logger LOG = Logger.getInstance("#com.intellij.idea.CommandLineApplication");

  protected static CommandLineApplication ourInstance = null;

  static {
    System.setProperty(FileWatcher.PROPERTY_WATCHER_DISABLED, Boolean.TRUE.toString());
  }

  protected CommandLineApplication() {}

  protected CommandLineApplication(boolean isInternal, boolean isUnitTestMode, boolean isHeadless) {
    this(isInternal, isUnitTestMode, isHeadless, "idea");
  }

  protected CommandLineApplication(boolean isInternal, boolean isUnitTestMode, boolean isHeadless, @NotNull @NonNls String appName) {
    LOG.assertTrue(ourInstance == null, "Only one instance allowed.");
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    ourInstance = this;
    createApplication(isInternal, isUnitTestMode, isHeadless, true, appName, null);
  }


  /**
   * @param appName used to load default configs; if you are not sure, use {@link #IDEA_APPLICATION}.
   */
  public static void createApplication(boolean internal,
                                       boolean isUnitTestMode,
                                       boolean isHeadlessMode,
                                       boolean isCommandline,
                                       @NotNull @NonNls String appName,
                                       @Nullable Splash splash) {
    new ApplicationImpl(internal, isUnitTestMode, isHeadlessMode, isCommandline, appName, splash);
  }

  public Object getData(String dataId) {
    return null;
  }

  public static class MyDataManagerImpl extends DataManagerImpl {
    @Override
    @NotNull
    public DataContext getDataContext() {
      return new CommandLineDataContext();
    }

    @Override
    public DataContext getDataContext(Component component) {
      return getDataContext();
    }

    @Override
    public DataContext getDataContext(@NotNull Component component, int x, int y) {
      return getDataContext();
    }

    private static class CommandLineDataContext extends UserDataHolderBase implements DataContext {
      @Override
      public Object getData(String dataId) {
        return ourInstance.getData(dataId);
      }
    }
  }
}
