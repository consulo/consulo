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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.ide.impl.idea.util.Consumer;
import consulo.language.editor.ui.awt.EditorTextField;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface EditChangelistSupport {

  ExtensionPointName<EditChangelistSupport> EP_NAME = ExtensionPointName.create(EditChangelistSupport.class);

  void installSearch(EditorTextField name, EditorTextField comment);

  Consumer<LocalChangeList> addControls(JPanel bottomPanel, @Nullable LocalChangeList initial);

  void changelistCreated(LocalChangeList changeList);
}
