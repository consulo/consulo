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

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.application.dumb.DumbAware;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

public abstract class FileChooserAction extends AnAction implements DumbAware {
  protected FileChooserAction() {
    setEnabledInModalContext(true);
  }

  protected FileChooserAction(final LocalizeValue text, final LocalizeValue description, final Image icon) {
    super(text, description, icon);
    setEnabledInModalContext(true);
  }

  @Deprecated
  @DeprecationInfo("Use constructor with LocalizeValue")
  protected FileChooserAction(final String text, final String description, final Image icon) {
    super(text, description, icon);
    setEnabledInModalContext(true);
  }

  @Override
  @RequiredUIAccess
  final public void actionPerformed(@Nonnull AnActionEvent e) {
    FileSystemTree tree = e.getRequiredData(FileSystemTree.DATA_KEY);
    actionPerformed(tree, e);
  }

  @Override
  final public void update(@Nonnull AnActionEvent e) {
    FileSystemTree tree = e.getData(FileSystemTree.DATA_KEY);
    if (tree != null) {
      e.getPresentation().setEnabled(true);
      update(tree, e);
    }
    else {
      e.getPresentation().setEnabled(false);
    }
  }

  protected abstract void update(FileSystemTree fileChooser, @Nonnull AnActionEvent e);

  protected abstract void actionPerformed(@Nonnull FileSystemTree fileChooser, @Nonnull AnActionEvent e);
}