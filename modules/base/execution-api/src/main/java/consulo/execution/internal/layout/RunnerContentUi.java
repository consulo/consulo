/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.internal.layout;

import consulo.disposer.Disposable;
import consulo.project.ui.wm.dock.DockContainer;
import consulo.ui.ex.action.QuickActionProvider;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentUI;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12.05.2024
 */
public interface RunnerContentUi extends ContentUI, CellTransform.Facade, ViewContext, Disposable, PropertyChangeListener, QuickActionProvider, DockContainer, DockContainer.Dialog {
    Key<RunnerContentUi> KEY = Key.create("DebuggerContentUI");

    @Override
    default JComponent getComponent() {
        throw new AbstractMethodError();
    }

    @Nullable
    Content findContent(String key);

    void restore(@Nonnull Content content);

    @Nullable
    Content findOrRestoreContentIfNeeded(@Nonnull String key);

    boolean isHorizontalToolbar();

    void setHorizontalToolbar(boolean state);

    void updateTabsUI(boolean validateNow);

    RunnerLayout getLayoutSettings();
}
