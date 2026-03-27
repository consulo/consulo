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
package consulo.ui.ex.action.coroutine;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineStep;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-03-27
 */
public class ActionSafeReadLock<I, O> extends CoroutineStep<I, O> {
    public static <I, O> CoroutineStep<I, O> apply(AnActionEvent event, @RequiredReadAction Consumer<Presentation> action) {
        return new ActionSafeReadLock<>(event.getPresentation(), action);
    }

    public static <I, O> CoroutineStep<I, O> apply(Presentation presentation, @RequiredReadAction Consumer<Presentation> action) {
        return new ActionSafeReadLock<>(presentation, action);
    }

    private final Presentation myPresentation;
    private final Consumer<Presentation> myAction;

    private ActionSafeReadLock(Presentation presentation, @RequiredReadAction Consumer<Presentation> action) {
        myPresentation = presentation;
        myAction = action;
    }

    @Override
    protected O execute(I input, Continuation<?> continuation) {
        Application application = Objects.requireNonNull(continuation.getConfiguration(Application.KEY), "Application required");

        boolean success = application.tryRunReadAction(() -> myAction.accept(myPresentation));

        if (!success) {
            myPresentation.setEnabled(false);
        }

        return null;
    }
}