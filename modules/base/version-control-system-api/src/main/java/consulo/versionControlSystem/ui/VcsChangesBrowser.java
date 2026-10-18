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
package consulo.versionControlSystem.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.versionControlSystem.change.Change;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public interface VcsChangesBrowser {
    Component getComponent();

    List<Change> getChanges();

    @RequiredUIAccess
    void setChanges(List<Change> changes);

    @RequiredUIAccess
    void setEmptyText(LocalizeValue text);

    /**
     * Shows the diff of the selected changes; can be registered on another component to diff from there.
     */
    AnAction getDiffAction();
}
