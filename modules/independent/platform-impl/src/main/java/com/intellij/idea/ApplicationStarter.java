/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.idea.starter.ApplicationPostStarter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ReflectionUtil;
import consulo.extensions.AreaInstanceEx;
import consulo.start.CommandLineArgs;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;

public class ApplicationStarter {
  private static final Logger LOG = Logger.getInstance(ApplicationStarter.class);

  private static ApplicationStarter ourInstance;
  public volatile static boolean ourLoaded;

  public static ApplicationStarter getInstance() {
    return ourInstance;
  }

  public static boolean isLoaded() {
    return ourLoaded;
  }

  private final CommandLineArgs myArgs;
  private final Class<? extends ApplicationPostStarter> myPostStarterClass;
  private boolean myPerformProjectLoad = true;
  private ApplicationPostStarter myPostStarter;

  private Application myApplication;

  public ApplicationStarter(@Nonnull Class<? extends ApplicationPostStarter> postStarterClass, @Nonnull CommandLineArgs args) {
    myPostStarterClass = postStarterClass;
    LOG.assertTrue(ourInstance == null);
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    ourInstance = this;

    myArgs = args;

    boolean headless = Main.isHeadless();

    patchSystem(headless);

    myPostStarter = createPostStarter();
    myApplication = myPostStarter.createApplication(headless, args);
    myPostStarter.premain(args);
  }

  protected void patchSystem(boolean headless) {
  }

  @Nonnull
  private ApplicationPostStarter createPostStarter() {
    try {
      Constructor<? extends ApplicationPostStarter> constructor = myPostStarterClass.getConstructor(ApplicationStarter.class);
      constructor.setAccessible(true);
      return ReflectionUtil.createInstance(constructor, this);
    }
    catch (NoSuchMethodException e) {
      throw new Error(e);
    }
  }

  public void run(boolean newConfigFolder) {
    try {

      ApplicationEx applicationEx = (ApplicationEx)myApplication;

      applicationEx.load(PathManager.getOptionsPath());

      AreaInstanceEx areaInstanceEx = (AreaInstanceEx)applicationEx;

      // inject members from fields
      areaInstanceEx.getInjector().injectMembers(myPostStarter);

      myPostStarter.run(applicationEx, newConfigFolder, myArgs);

      myPostStarter = null;

      ourLoaded = true;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isPerformProjectLoad() {
    return myPerformProjectLoad;
  }

  public void setPerformProjectLoad(boolean performProjectLoad) {
    myPerformProjectLoad = performProjectLoad;
  }
}
