/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.ui;

import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vcs.AbstractVcsHelper;
import consulo.ide.impl.idea.openapi.vcs.VcsException;
import consulo.util.lang.function.ThrowableRunnable;

/**
 * @author irengrig
 *         Date: 3/16/11
 *         Time: 3:22 PM
 */
public class VcsSynchronousProgressWrapper {
  private VcsSynchronousProgressWrapper() {
  }

  public static boolean wrap(final ThrowableRunnable<VcsException> runnable, final Project project, final String title) {
    final VcsException[] exc = new VcsException[1];
    final Runnable process = new Runnable() {
      @Override
      public void run() {
        try {
          runnable.run();
        }
        catch (VcsException e) {
          exc[0] = e;
        }
      }
    };
    final boolean notCanceled;
    if (ApplicationManager.getApplication().isDispatchThread()) {
      notCanceled = ProgressManager.getInstance().runProcessWithProgressSynchronously(process, title, true, project);
    } else {
      process.run();
      notCanceled = true;
    }
    if (exc[0] != null) {
      AbstractVcsHelper.getInstance(project).showError(exc[0], title);
      return false;
    }
    return notCanceled;
  }
}
