/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.ui.awt;

import consulo.ui.ex.action.AnAction;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
public interface ChangesBrowserTree<T> {
    void setEmptyText(@Nonnull String emptyText);

    void setScrollPaneBorder(Border border);

    void excludeChanges(Collection<T> changes);

    void includeChanges(Collection<T> changes);

    Collection<T> getIncludedChanges();

    void setIncludedChanges(Collection<T> changes);

    default void setChangesToDisplay(List<T> changes) {
        setChangesToDisplay(changes, null);
    }

    void setChangesToDisplay(List<T> changes, @Nullable VirtualFile toSelect);

    @Nonnull
    AnAction[] getTreeActions();

    @Nonnull
    default JComponent getComponent() {
        return (JComponent) this;
    }
}
