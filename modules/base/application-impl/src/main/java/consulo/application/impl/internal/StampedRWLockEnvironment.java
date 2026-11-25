/*
 * Copyright 2013-2026 consulo.io
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
package consulo.application.impl.internal;

import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.ui.UIAccess;

/**
 * What {@link StampedRWLock} asks of the application while it waits.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
interface StampedRWLockEnvironment {
    StampedRWLockEnvironment DEFAULT = new StampedRWLockEnvironment() {
        @Override
        public boolean isReadWaitCancelled() {
            ProgressIndicator progress = ProgressManager.getGlobalProgressIndicator();
            return progress != null && progress.isCanceled() && !ProgressManager.getInstance().isInNonCancelableSection();
        }

        @Override
        public boolean isImpatienceAllowed() {
            return CoreProgressManager.ENABLED && !ProgressManager.getInstance().isInNonCancelableSection();
        }

        @Override
        public boolean isUIThread() {
            return UIAccess.isUIThread();
        }

        @Override
        public void beforeWriteRelock() {
            ProgressIndicatorUtils.cancelActionsToBeCancelledBeforeWrite();
        }
    };

    /**
     * @return true when a thread waiting for a read lock should give up with a {@link consulo.component.ProcessCanceledException}
     */
    boolean isReadWaitCancelled();

    /**
     * @return true when an impatient reader may fail instead of waiting for a pending write action
     */
    boolean isImpatienceAllowed();

    boolean isUIThread();

    /**
     * Called before a suspended write lock is taken again.
     */
    void beforeWriteRelock();
}
