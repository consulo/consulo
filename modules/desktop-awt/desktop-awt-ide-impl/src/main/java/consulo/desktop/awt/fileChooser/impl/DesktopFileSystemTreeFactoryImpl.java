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
import consulo.ide.impl.idea.ide.actions.SynchronizeAction;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTreeFactory;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileSystemTreeImpl;
import consulo.project.Project;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

@Singleton
@ServiceImpl
public class DesktopFileSystemTreeFactoryImpl extends FileSystemTreeFactory {
  @Override
  @Nonnull
  public FileSystemTree createFileSystemTree(Project project, FileChooserDescriptor fileChooserDescriptor) {
    return new FileSystemTreeImpl(project, fileChooserDescriptor);
  }

  @Override
  @Nonnull
  public DefaultActionGroup createDefaultFileSystemActions(FileSystemTree fileSystemTree) {
    DefaultActionGroup group = new DefaultActionGroup();
    final ActionManager actionManager = ActionManager.getInstance();
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
