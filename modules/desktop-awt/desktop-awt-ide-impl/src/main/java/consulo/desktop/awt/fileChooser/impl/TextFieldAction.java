/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.action.ActionManager;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.LinkListener;

/**
 * User: anna
 */
public abstract class TextFieldAction extends LinkLabel implements LinkListener {
  public TextFieldAction() {
    super("", null);
    setListener(this, null);
    update();
  }

  protected void onSetActive(final boolean active) {
    final String tooltip = KeymapUtil
      .createTooltipText(ActionsBundle.message("action.FileChooser.TogglePathShowing.text"),
                         ActionManager.getInstance().getAction("FileChooser.TogglePathShowing"));
    setToolTipText(tooltip);
  }

  protected String getStatusBarText() {
    return ActionsBundle.message("action.FileChooser.TogglePathShowing.text");
  }

  public void update() {
    setVisible(true);
    setText(PropertiesComponent.getInstance().getBoolean(FileChooserDialogImpl.FILE_CHOOSER_SHOW_PATH_PROPERTY, true) ? IdeBundle.message("file.chooser.hide.path") : IdeBundle.message("file.chooser.show.path"));
  }
}
