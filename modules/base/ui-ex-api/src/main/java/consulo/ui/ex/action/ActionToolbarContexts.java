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
package consulo.ui.ex.action;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.Component;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-29
 */
public final class ActionToolbarContexts {
    private static final Supplier<DataContext> EMPTY = () -> DataContext.EMPTY_CONTEXT;

    private static final Supplier<DataContext> FOCUSED = () -> DataManager.getInstance().getDataContext();

    private ActionToolbarContexts() {
    }

    public static Supplier<DataContext> empty() {
        return EMPTY;
    }

    public static Supplier<DataContext> focused() {
        return FOCUSED;
    }

    public static Supplier<DataContext> forTargetComponent(Component component) {
        return () -> DataManager.getInstance().getDataContext(component);
    }

    public static Supplier<DataContext> forSelectedContent(ContentManager contentManager, Component fallback) {
        return () -> {
            Content content = contentManager.getSelectedContent();
            Component target = content == null ? null : content.getUIPreferredFocusableComponent();
            if (target == null && content != null) {
                target = content.getUIComponent();
            }
            return DataManager.getInstance().getDataContext(target != null ? target : fallback);
        };
    }

    public static Supplier<DataContext> forTargetComponent(javax.swing.JComponent component) {
        return () -> DataManager.getInstance().getDataContext(component);
    }

    public static Supplier<DataContext> forSelectedContent(ContentManager contentManager, javax.swing.JComponent fallback) {
        return () -> {
            Content content = contentManager.getSelectedContent();
            javax.swing.JComponent target = content == null ? null : content.getPreferredFocusableComponent();
            if (target == null && content != null) {
                target = content.getComponent();
            }
            return DataManager.getInstance().getDataContext(target != null ? target : fallback);
        };
    }
}
