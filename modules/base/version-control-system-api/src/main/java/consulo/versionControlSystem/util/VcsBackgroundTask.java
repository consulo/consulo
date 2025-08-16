/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package consulo.versionControlSystem.util;

import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.VcsException;
import consulo.application.ApplicationManager;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public abstract class VcsBackgroundTask<T> extends Task.ConditionalModal {
  private final Collection<T> myItems;
  private final List<VcsException> myExceptions = new ArrayList<>();

  public VcsBackgroundTask(Project project, @Nonnull String title, @Nonnull PerformInBackgroundOption backgroundOption,
                           Collection<T> itemsToProcess, boolean canBeCanceled) {
    super(project, title, canBeCanceled, backgroundOption);
    myItems = itemsToProcess;
  }

  public VcsBackgroundTask(Project project, @Nonnull String title, @Nonnull PerformInBackgroundOption backgroundOption,
                           Collection<T> itemsToProcess) {
    this(project, title, backgroundOption, itemsToProcess, false);
  }

  @Override
  public void run(@Nonnull ProgressIndicator indicator) {
    for(T item: myItems) {
      try {
        process(item);
      }
      catch(VcsException ex) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
          throw new RuntimeException(ex);
        }
        myExceptions.add(ex);
      }
    }
  }

  protected boolean executedOk() {
    return myExceptions.isEmpty();
  }

  @Override
  @RequiredUIAccess
  public void onSuccess() {
    if (!myExceptions.isEmpty()) {
      AbstractVcsHelper.getInstance((Project)myProject).showErrors(myExceptions, myTitle);
    }
  }

  protected abstract void process(T item) throws VcsException;
}
