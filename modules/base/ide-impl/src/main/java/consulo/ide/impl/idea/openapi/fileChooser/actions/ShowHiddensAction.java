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
package consulo.ide.impl.idea.openapi.fileChooser.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nullable;

/**
 * @author Vladimir Kondratyev
 */
public final class ShowHiddensAction extends ToggleAction implements DumbAware {
    @Override
    public boolean isSelected(AnActionEvent e) {
        final FileSystemTree fileSystemTree = e.getData(FileSystemTree.DATA_KEY);
        return fileSystemTree != null && fileSystemTree.areHiddensShown();
    }

    @Override
    @RequiredUIAccess
    public void setSelected(AnActionEvent e, boolean state) {
        final FileSystemTree fileSystemTree = e.getData(FileSystemTree.DATA_KEY);
        if (fileSystemTree != null) {
            fileSystemTree.showHiddens(state);
        }
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.actionsTogglevisibility();
    }
}