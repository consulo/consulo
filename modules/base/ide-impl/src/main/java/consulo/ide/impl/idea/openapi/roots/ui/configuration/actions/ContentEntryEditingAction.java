/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.roots.ui.configuration.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ContentEntryTreeEditor;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.internal.AnActionWithUIUpdate;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Eugene Zhuravlev
 * @since 2003-10-14
 */
public abstract class ContentEntryEditingAction extends ToggleAction implements DumbAware, AnActionWithUIUpdate {
    protected final ContentEntryTreeEditor myTreeEditor;

    protected ContentEntryEditingAction(ContentEntryTreeEditor treeEditor, LocalizeValue text, @Nullable Image image) {
        super(text, text, image);
        myTreeEditor = treeEditor;
    }

    @Override
    @RequiredUIAccess
    public void updateAtUI(AnActionEvent e) {
        update(e);
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(myTreeEditor.getContentEntryEditor() != null && !getSelectedFiles().isEmpty());
    }

    @RequiredUIAccess
    protected final List<VirtualFile> getSelectedFiles() {
        return myTreeEditor.getSelectedFiles();
    }
}
