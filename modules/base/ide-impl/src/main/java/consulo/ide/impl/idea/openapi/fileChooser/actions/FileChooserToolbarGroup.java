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
package consulo.ide.impl.idea.openapi.fileChooser.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-09-14
 */
@ActionImpl(
    id = "FileChooserToolbar",
    children = {
        @ActionRef(type = GotoHomeAction.class),
        @ActionRef(type = GotoDesktopDirAction.class),
        @ActionRef(type = GotoProjectDirectory.class),
        @ActionRef(type = GotoModuleDirectory.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = NewFileAction.class),
        @ActionRef(type = NewFolderAction.class),
        @ActionRef(type = FileDeleteAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = RefreshFileChooserAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ShowHiddensAction.class)
    }
)
public class FileChooserToolbarGroup extends DefaultActionGroup implements DumbAware {
    public FileChooserToolbarGroup() {
        super(LocalizeValue.absent(), false);
    }
}
