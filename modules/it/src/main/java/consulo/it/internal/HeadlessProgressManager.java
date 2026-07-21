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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.internal.CheckCanceledHook;
import consulo.application.internal.SuspenderProgressManager;
import consulo.application.internal.UnsafeProgressIndicator;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.progress.TaskInfo;
import consulo.component.ComponentManager;
import consulo.util.collection.ContainerUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Headless {@code ProgressManager}: ports the non-UI parts of {@code ProgressManagerImpl}
 * (check-canceled hooks for {@code ProgressSuspender}, unsafe-indicator detection) on top of
 * {@link CoreProgressManager}; only the backgroundable indicators are headless instead of the
 * status-bar UI ones. The real {@code ProgressManagerImpl} lives in {@code ide-impl}, which is not
 * on the test classpath.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl
public class HeadlessProgressManager extends CoreProgressManager implements SuspenderProgressManager {
    private final Set<CheckCanceledHook> myHooks = ConcurrentHashMap.newKeySet();
    private final CheckCanceledHook mySleepHook = __ -> sleepIfNeededToGivePriorityToAnotherThread();

    @Inject
    public HeadlessProgressManager(Application application) {
        super(application);
    }

    @Override
    public boolean hasUnsafeProgressIndicator() {
        return super.hasUnsafeProgressIndicator() || ContainerUtil.exists(
            getCurrentIndicators(),
            it -> it instanceof UnsafeProgressIndicator pi && pi.isUnsafeIndicator()
        );
    }

    @Override
    public ProgressIndicator newBackgroundableProcessIndicator(Task.Backgroundable backgroundable) {
        return new HeadlessBackgroundableProcessIndicator();
    }

    @Override
    public ProgressIndicator newBackgroundableProcessIndicator(
        @Nullable ComponentManager project,
        TaskInfo info,
        PerformInBackgroundOption option
    ) {
        return new HeadlessBackgroundableProcessIndicator();
    }

    /**
     * An absolutely guru method, very dangerous, don't use unless you're desperate,
     * because hooks will be executed on every checkCanceled and can dramatically slow down everything in the IDE.
     */
    @Override
    public void addCheckCanceledHook(CheckCanceledHook hook) {
        if (myHooks.add(hook)) {
            updateShouldCheckCanceled();
        }
    }

    @Override
    public void removeCheckCanceledHook(CheckCanceledHook hook) {
        if (myHooks.remove(hook)) {
            updateShouldCheckCanceled();
        }
    }

    @Override
    protected @Nullable CheckCanceledHook createCheckCanceledHook() {
        if (myHooks.isEmpty()) {
            return null;
        }

        CheckCanceledHook[] activeHooks = myHooks.toArray(CheckCanceledHook.EMPTY_ARRAY);
        return activeHooks.length == 1 ? activeHooks[0] : indicator -> {
            boolean result = false;
            for (CheckCanceledHook hook : activeHooks) {
                if (hook.runHook(indicator)) {
                    result = true; // but still continue to other hooks
                }
            }
            return result;
        };
    }

    @Override
    protected void prioritizingStarted() {
        addCheckCanceledHook(mySleepHook);
    }

    @Override
    protected void prioritizingFinished() {
        removeCheckCanceledHook(mySleepHook);
    }
}
