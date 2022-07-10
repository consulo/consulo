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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.vcs.VcsBundle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static consulo.ide.impl.idea.util.ObjectUtils.notNull;
import static consulo.ide.impl.idea.util.WaitForProgressToShow.runOrInvokeLaterAboveProgress;

class FictiveBackgroundable extends Task.Backgroundable {
  @Nonnull
  private final Waiter myWaiter;
  @Nullable private final IdeaModalityState myState;

  FictiveBackgroundable(@Nonnull Project project,
                        @Nonnull Runnable runnable,
                        String title,
                        boolean cancellable,
                        @Nullable IdeaModalityState state) {
    super(project, VcsBundle.message("change.list.manager.wait.lists.synchronization", title), cancellable);
    myState = state;
    myWaiter = new Waiter(project, runnable, title, cancellable);
  }

  public void run(@Nonnull ProgressIndicator indicator) {
    myWaiter.run(indicator);
    runOrInvokeLaterAboveProgress(() -> myWaiter.onSuccess(), (IdeaModalityState)notNull(myState, IdeaModalityState.NON_MODAL), (Project)myProject);
  }

  @Override
  public boolean isHeadless() {
    return false;
  }

  public void done() {
    myWaiter.done();
  }
}
