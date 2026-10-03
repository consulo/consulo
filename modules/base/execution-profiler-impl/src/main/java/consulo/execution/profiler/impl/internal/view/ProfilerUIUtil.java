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
package consulo.execution.profiler.impl.internal.view;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerUIUtil {
    private ProfilerUIUtil() {
    }

    @RequiredUIAccess
    public static Label hint(LocalizeValue text) {
        Label label = Label.create(text);
        label.setForegroundColor(ComponentColors.INFO_FOREGROUND);
        return label;
    }

    @RequiredUIAccess
    public static Label error(LocalizeValue text) {
        Label label = Label.create(text);
        label.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
        return label;
    }

    @RequiredUIAccess
    public static Tab addTab(TabbedLayout layout, LocalizeValue title, Component component) {
        return addTab(layout, title, component, null);
    }

    @RequiredUIAccess
    public static Tab addTab(
        TabbedLayout layout,
        LocalizeValue title,
        Component component,
        @Nullable BiConsumer<Tab, Component> closeHandler
    ) {
        Tab tab = layout.createTab();
        tab.setRenderer((it, presentation) -> presentation.append(title));
        tab.setCloseHandler(closeHandler);
        layout.addTab(tab, component);
        return tab;
    }

    public static String describe(Throwable error) {
        Throwable cause = error;
        Throwable next = cause.getCause();
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && next != null) {
            cause = next;
            next = cause.getCause();
        }

        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
