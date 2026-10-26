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
package consulo.review.util;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import kava.beans.PropertyChangeListener;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class ReviewUIUtil {
    private ReviewUIUtil() {
    }

    /**
     * Applies the current value now and every later one on the UI thread, until {@code parentDisposable} goes away.
     */
    @RequiredUIAccess
    public static <T extends @Nullable Object> void bind(Disposable parentDisposable, ObservableValue<T> value, @RequiredUIAccess Consumer<T> uiSetter) {
        UIAccess uiAccess = UIAccess.current();
        AtomicBoolean disposed = new AtomicBoolean();
        uiSetter.accept(value.get());
        Runnable unsubscribe = value.addListener(newValue -> uiAccess.give(() -> {
            if (!disposed.get()) {
                uiSetter.accept(newValue);
            }
        }));
        Disposer.register(parentDisposable, () -> {
            disposed.set(true);
            unsubscribe.run();
        });
    }

    /**
     * Applies both current values now and again whenever either changes, until {@code parentDisposable} goes away.
     */
    @RequiredUIAccess
    public static <A extends @Nullable Object, B extends @Nullable Object> void bind(Disposable parentDisposable,
                                   ObservableValue<A> first,
                                   ObservableValue<B> second,
                                   @RequiredUIAccess BiConsumer<A, B> uiSetter) {
        UIAccess uiAccess = UIAccess.current();
        AtomicBoolean disposed = new AtomicBoolean();
        Runnable update = () -> uiAccess.give(() -> {
            if (!disposed.get()) {
                uiSetter.accept(first.get(), second.get());
            }
        });
        uiSetter.accept(first.get(), second.get());
        Runnable firstUnsubscribe = first.addListener(value -> update.run());
        Runnable secondUnsubscribe = second.addListener(value -> update.run());
        Disposer.register(parentDisposable, () -> {
            disposed.set(true);
            firstUnsubscribe.run();
            secondUnsubscribe.run();
        });
    }

    /**
     * Performs the action in the data context of {@code source}.
     */
    @RequiredUIAccess
    public static void performAction(AnAction action, Component source, String place) {
        DataContext context = DataManager.getInstance().getDataContext(source);
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, place, context);
        ActionManager.getInstance().performActionDumbAware(action, event);
    }

    /**
     * A button following the text and the description of the template presentation of the action and the enabled state
     * of a {@link ReviewAction}, which performs the action when clicked.
     */
    @RequiredUIAccess
    public static Button createActionButton(Disposable parentDisposable, AnAction action, String place) {
        Presentation presentation = action.getTemplatePresentation();
        Button button = Button.create(presentation.getTextValue());
        button.setToolTipText(presentation.getDescription());
        if (action instanceof ReviewAction reviewAction) {
            bind(parentDisposable, reviewAction.getEnabled(), button::setEnabled);
        }

        UIAccess uiAccess = UIAccess.current();
        AtomicBoolean disposed = new AtomicBoolean();
        PropertyChangeListener listener = event -> uiAccess.give(() -> {
            if (disposed.get()) {
                return;
            }
            button.setText(presentation.getTextValue());
            button.setToolTipText(presentation.getDescription());
        });
        presentation.addPropertyChangeListener(listener);
        Disposer.register(parentDisposable, () -> {
            disposed.set(true);
            presentation.removePropertyChangeListener(listener);
        });

        button.addClickListener(event -> performAction(action, button, place));
        return button;
    }
}
