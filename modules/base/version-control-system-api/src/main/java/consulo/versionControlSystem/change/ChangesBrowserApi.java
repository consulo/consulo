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
package consulo.versionControlSystem.change;

import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.internal.ChangesBrowserTree;

import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 21-Jul-24
 */
public interface ChangesBrowserApi<T> {
    public enum MyUseCase {
        LOCAL_CHANGES,
        COMMITTED_CHANGES
    }

    Key<ChangesBrowserApi> DATA_KEY = Key.create(ChangesBrowserApi.class);

    void rebuildList();

    void setDataIsDirty(boolean dataIsDirty);

    List<Change> getCurrentDisplayedChanges();

    void setChangesToDisplay(List<T> changes);

    JComponent getPreferredFocusedComponent();

    ChangesBrowserTree getViewer();

    JScrollPane getViewerScrollPane();

    AnAction getDiffAction();

    default JComponent getComponent() {
        return (JComponent) this;
    }
}
