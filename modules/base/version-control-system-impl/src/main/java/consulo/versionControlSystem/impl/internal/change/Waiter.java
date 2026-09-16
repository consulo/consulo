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
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.application.util.Semaphore;
import consulo.logging.Logger;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.concurrent.atomic.AtomicBoolean;

public class Waiter extends Task.Modal {
    private static final Logger LOG = Logger.getInstance(Waiter.class);

    private final Runnable myRunnable;

    private final AtomicBoolean myStarted = new AtomicBoolean();

    private final Semaphore mySemaphore = new Semaphore();

    public Waiter(Project project, Runnable runnable, String title, boolean cancellable) {
        super(project, VcsLocalize.changeListManagerWaitListsSynchronization(title), cancellable);
        myRunnable = runnable;
        mySemaphore.down();
        setCancelText(LocalizeValue.localizeTODO("Skip"));
    }

    @Override
    public void run(ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        indicator.setText2(VcsLocalize.commitWaitUtilSynchedText());

        if (!myStarted.compareAndSet(false, true)) {
            LOG.error("Waiter running under progress being started again.");
        }
        else {
            while (!mySemaphore.waitFor(500)) {
                indicator.checkCanceled();
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void onCancel() {
        onSuccess();
    }

    @Override
    @RequiredUIAccess
    public void onSuccess() {
        // Be careful with changes here as "Waiter.onSuccess()" is explicitly invoked from "FictiveBackgroundable"
        if (!myProject.isDisposed()) {
            myRunnable.run();
            ChangesViewManagerImpl.getInstance((Project) myProject).scheduleRefresh();
        }
    }

    public void done() {
        mySemaphore.up();
    }
}
