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
package consulo.project.impl.internal;

import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.UIAccessScheduler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.Clipboard;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-23
 */
public class ProjectAwareUIAccess implements UIAccess {
    private final Project myProject;
    private final UIAccess myUIAccess;

    private final ProjectAwareUIAccessScheduler myScheduler;

    public ProjectAwareUIAccess(Project project, UIAccess uiAccess) {
        myProject = project;
        myUIAccess = uiAccess;
        myScheduler = new ProjectAwareUIAccessScheduler(uiAccess.getScheduler(), this);
    }

    Runnable wrap(Runnable runnable) {
        return () -> {
            if (myProject.isDisposed()) {
                return;
            }

            runnable.run();
        };
    }

    <T> Supplier<T> wrap(Supplier<T> supplier) {
        return () -> {
            if (myProject.isDisposed()) {
                return null;
            }
            return supplier.get();
        };
    }

    <T> Callable<T> wrap(Callable<T> supplier) {
        return () -> {
            if (myProject.isDisposed()) {
                return null;
            }
            return supplier.call();
        };
    }

    @Override
    public Clipboard getClipboard() {
        return myUIAccess.getClipboard();
    }

    @RequiredUIAccess
    @Override
    public int getEventCount() {
        return myUIAccess.getEventCount();
    }

    @RequiredUIAccess
    @Override
    public Runnable markEventCount() {
        return myUIAccess.markEventCount();
    }

    @Override
    public boolean isValid() {
        if (myProject.isDisposed()) {
            return false;
        }

        return myUIAccess.isValid();
    }

    @Override
    public void give(Runnable runnable) {
        myUIAccess.give(wrap(runnable));
    }

    @Override
    public CompletableFuture<?> giveAsync(Runnable runnable) {
        return myUIAccess.giveAsync(wrap(runnable));
    }

    @Override
    public <T> CompletableFuture<T> giveAsync(Supplier<T> supplier) {
        return myUIAccess.giveAsync(wrap(supplier));
    }

    @Override
    public void giveAndWait(Runnable runnable) {
        myUIAccess.giveAndWait(wrap(runnable));
    }

    @Override
    public <T> T giveAndWaitIfNeed(Supplier<T> supplier) {
        return myUIAccess.giveAndWaitIfNeed(wrap(supplier));
    }

    @Override
    public void giveIfNeed(Runnable runnable) {
        myUIAccess.giveIfNeed(wrap(runnable));
    }

    @Override
    public void giveAndWaitIfNeed(Runnable runnable) {
        myUIAccess.giveAndWaitIfNeed(wrap(runnable));
    }

    @Override
    public boolean isInModalContext() {
        return myUIAccess.isInModalContext();
    }

    @Override
    public void execute(Runnable command) {
        myUIAccess.execute(wrap(command));
    }

    @Override
    public UIAccessScheduler getScheduler() {
        return myScheduler;
    }

    @Nullable
    @Override
    public <T> T getUserData(Key<T> key) {
        return myUIAccess.getUserData(key);
    }

    @Override
    public <T> void putUserData(Key<T> key, T value) {
        myUIAccess.putUserData(key, value);
    }
}
