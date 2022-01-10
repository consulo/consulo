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
package com.intellij.util.ui.update;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import consulo.application.ApplicationProperties;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;

public abstract class LazyUiDisposable<T extends Disposable> implements Activatable {

  private Throwable myAllocation;

  private boolean myWasEverShown;

  private final Disposable myParent;
  private final T myChild;

  public LazyUiDisposable(@Nullable Disposable parent, @Nonnull JComponent ui, @Nonnull T child) {
    if (ApplicationProperties.isInSandbox()) {
      myAllocation = new Exception();
    }

    myParent = parent;
    myChild = child;

    new UiNotifyConnector.Once(ui, this);
  }

  @Override
  public final void showNotify() {
    if (myWasEverShown) return;

    try {
      findParentDisposable().doWhenDone(parent -> {
        Project project = null;
        if (ApplicationManager.getApplication() != null) {
          project = DataManager.getInstance().getDataContext().getData(CommonDataKeys.PROJECT);
        }
        initialize(parent, myChild, project);
        Disposer.register(parent, myChild);
      });
    }
    finally {
      myWasEverShown = true;
    }
  }

  @Override
  public final void hideNotify() {
  }

  protected abstract void initialize(@Nonnull Disposable parent, @Nonnull T child, @Nullable Project project);

  @Nonnull
  private AsyncResult<Disposable> findParentDisposable() {
    return findDisposable(myParent, PlatformDataKeys.UI_DISPOSABLE);
  }

  private static AsyncResult<Disposable> findDisposable(Disposable defaultValue, final Key<? extends Disposable> key) {
    if (defaultValue == null) {
      if (ApplicationManager.getApplication() != null) {
        final AsyncResult<Disposable> result = AsyncResult.undefined();
        DataManager.getInstance().getDataContextFromFocus().doWhenDone(context -> {
          Disposable disposable = context.getData(key);
          if (disposable == null) {
            disposable = Disposer.get("ui");
          }
          result.setDone(disposable);
        });
        return result;
      }
      else {
        return null;
      }
    }
    else {
      return AsyncResult.done(defaultValue);
    }
  }
}
