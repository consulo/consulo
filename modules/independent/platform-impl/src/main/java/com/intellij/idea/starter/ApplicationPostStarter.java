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
package com.intellij.idea.starter;

import com.intellij.ide.StartupProgress;
import com.intellij.idea.ApplicationStarter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.util.Ref;
import consulo.annotations.Internal;
import consulo.application.TransactionGuardEx;
import consulo.start.CommandLineArgs;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@Internal
public abstract class ApplicationPostStarter {
  protected final Ref<StartupProgress> mySplashRef = Ref.create();
  protected ApplicationStarter myApplicationStarter;

  @Inject
  @Nonnull
  private TransactionGuard myTransactionGuard;

  public ApplicationPostStarter(ApplicationStarter applicationStarter) {
    myApplicationStarter = applicationStarter;
  }

  @Nonnull
  public abstract Application createApplication(boolean isHeadlessMode, CommandLineArgs args);

  public void premain(@Nonnull CommandLineArgs args) {
  }

  public void run(Application application, boolean newConfigFolder, @Nonnull CommandLineArgs args) {
    if (needStartInTransaction()) {
      ((TransactionGuardEx)myTransactionGuard).performUserActivity(() -> main(application, newConfigFolder, args));
    }
    else {
      main(application, newConfigFolder, args);
    }
  }

  public void main(Application application, boolean newConfigFolder, @Nonnull CommandLineArgs args) {
  }

  public boolean needStartInTransaction() {
    return true;
  }
}
