/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.ui.EditorBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface EditChangelistSupport {
    ExtensionPointName<EditChangelistSupport> EP_NAME = ExtensionPointName.create(EditChangelistSupport.class);

    /**
     * Customizes change list name and comment components.
     * E.g., could be used to install completion to these components.
     *
     * @param name    change list name component
     * @param comment change list comment component
     */
    @RequiredUIAccess
    void installSearch(EditorBox name, EditorBox comment);

    /**
     * Allows adding custom components to create / edit change list UI.
     * And reacting to successful change list creation or edition.
     *
     * @param bottomPanel panel to add custom components
     * @param initial     change list being edited or {@code null} if a new change list is created
     * @return callback to call after a change list is successfully created or edited
     */
    @RequiredUIAccess
    @Nullable Consumer<LocalChangeList> addControls(HorizontalLayout bottomPanel, @Nullable LocalChangeList initial);

    @Deprecated
    @DeprecationInfo("Use return value of addControls(HorizontalLayout, LocalChangeList) instead")
    default void changelistCreated(LocalChangeList changeList) {
    }
}
