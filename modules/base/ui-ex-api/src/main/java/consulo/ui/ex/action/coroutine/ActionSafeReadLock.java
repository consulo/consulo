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
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An action update step which never waits for a write action. The lock-free part runs first and decides what it can from the
 * project model (project, editor, virtual file, modules, file index) - it must set the visibility and the enabled state it
 * decides, it starts from whatever the presentation holds. The part under the read lock runs only for a presentation the
 * lock-free part left visible and enabled, so an action which does not apply to the context never touches the read lock.
 * The read lock is only tried: while a write action holds it the presentation is disabled and {@code null} is produced.
 *
 * @author VISTALL
 * @since 2026-03-27
 */
public class ActionSafeReadLock<I, O> extends CoroutineStep<I, O> {
    public static <I, O> CoroutineStep<I, O> run(
        AnActionEvent event,
        Consumer<Presentation> lockFree,
        @RequiredReadAction Consumer<Presentation> underLock
    ) {
        return run(event.getPresentation(), lockFree, underLock);
    }

    public static <I, O> CoroutineStep<I, O> run(
        Presentation presentation,
        Consumer<Presentation> lockFree,
        @RequiredReadAction Consumer<Presentation> underLock
    ) {
        return apply(presentation, lockFree, p -> {
            underLock.accept(p);
            return null;
        });
    }

    public static <I, O> CoroutineStep<I, O> apply(
        AnActionEvent event,
        Consumer<Presentation> lockFree,
        @RequiredReadAction Function<Presentation, O> underLock
    ) {
        return apply(event.getPresentation(), lockFree, underLock);
    }

    public static <I, O> CoroutineStep<I, O> apply(
        Presentation presentation,
        Consumer<Presentation> lockFree,
        @RequiredReadAction Function<Presentation, O> underLock
    ) {
        return new ActionSafeReadLock<>(presentation, lockFree, underLock);
    }

    /**
     * @deprecated an update without a lock-free part is disabled while any write action runs - use
     * {@link #run(AnActionEvent, Consumer, Consumer)}
     */
    @Deprecated
    public static <I, O> CoroutineStep<I, O> run(AnActionEvent event, @RequiredReadAction Consumer<Presentation> action) {
        return run(event.getPresentation(), action);
    }

    /**
     * @deprecated use {@link #run(Presentation, Consumer, Consumer)}
     */
    @Deprecated
    public static <I, O> CoroutineStep<I, O> run(Presentation presentation, @RequiredReadAction Consumer<Presentation> action) {
        return apply(presentation, p -> {
            action.accept(p);
            return null;
        });
    }

    /**
     * @deprecated use {@link #apply(AnActionEvent, Consumer, Function)}
     */
    @Deprecated
    public static <I, O> CoroutineStep<I, O> apply(AnActionEvent event, @RequiredReadAction Function<Presentation, O> action) {
        return apply(event.getPresentation(), action);
    }

    /**
     * @deprecated use {@link #apply(Presentation, Consumer, Function)}
     */
    @Deprecated
    public static <I, O> CoroutineStep<I, O> apply(Presentation presentation, @RequiredReadAction Function<Presentation, O> action) {
        return new ActionSafeReadLock<>(presentation, null, action);
    }

    private final Presentation myPresentation;
    private final @Nullable Consumer<Presentation> myLockFree;
    private final Function<Presentation, O> myUnderLock;

    private ActionSafeReadLock(
        Presentation presentation,
        @Nullable Consumer<Presentation> lockFree,
        @RequiredReadAction Function<Presentation, O> underLock
    ) {
        myPresentation = presentation;
        myLockFree = lockFree;
        myUnderLock = underLock;
    }

    @Override
    protected @Nullable O execute(I input, Continuation<?> continuation) {
        if (myLockFree != null) {
            myLockFree.accept(myPresentation);
            if (!myPresentation.isVisible() || !myPresentation.isEnabled()) {
                return null;
            }
        }

        Application application = Objects.requireNonNull(continuation.getConfiguration(Application.KEY), "Application required");

        SimpleReference<O> ref = new SimpleReference<>();
        if (!application.tryRunReadAction(ref, () -> myUnderLock.apply(myPresentation))) {
            myPresentation.setEnabled(false);
            return null;
        }

        return ref.get();
    }
}
