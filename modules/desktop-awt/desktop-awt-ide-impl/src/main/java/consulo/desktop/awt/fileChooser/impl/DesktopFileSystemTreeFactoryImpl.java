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
package consulo.desktop.awt.fileChooser.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileSystemTree;
import consulo.fileChooser.FileSystemTreeFactory;
import consulo.ide.impl.idea.ide.actions.SynchronizeAction;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileSystemTreeImpl;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.tree.Tree;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.util.function.Function;

@Singleton
@ServiceImpl
public class DesktopFileSystemTreeFactoryImpl implements FileSystemTreeFactory {
    @Override
    @Nonnull
    public FileSystemTree createFileSystemTree(ComponentManager project, FileChooserDescriptor fileChooserDescriptor) {
        return new FileSystemTreeImpl((Project) project, fileChooserDescriptor);
    }

    @Nonnull
    @Override
    public FileSystemTree createFileSystemTree(@Nullable ComponentManager project, FileChooserDescriptor descriptor, Object tree, @Nullable TreeCellRenderer renderer, @Nullable Function<? super TreePath, String> speedSearchConverter) {
        return new FileSystemTreeImpl((Project) project, descriptor, (Tree) tree, renderer, speedSearchConverter);
    }

    @Override
    @Nonnull
    public ActionGroup createDefaultFileSystemActions(FileSystemTree fileSystemTree) {
        DefaultActionGroup group = new DefaultActionGroup();
        ActionManager actionManager = ActionManager.getInstance();
        group.add(actionManager.getAction("FileChooser.GotoHome"));
        group.add(actionManager.getAction("FileChooser.GotoProject"));
        group.addSeparator();
        group.add(actionManager.getAction("FileChooser.NewFolder"));
        group.add(actionManager.getAction("FileChooser.Delete"));
        group.addSeparator();
        SynchronizeAction action1 = new SynchronizeAction();
        AnAction original = actionManager.getAction(IdeActions.ACTION_SYNCHRONIZE);
        action1.copyFrom(original);
        action1.registerCustomShortcutSet(original.getShortcutSet(), fileSystemTree.getTree());
        group.add(action1);
        group.addSeparator();
        group.add(actionManager.getAction("FileChooser.ShowHiddens"));

        return group;
    }
}
