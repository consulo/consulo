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

import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.LinkListener;

/**
 * @author anna
 */
public abstract class TextFieldAction extends LinkLabel implements LinkListener {
  public TextFieldAction() {
    super("", null);
    setListener(this, null);
    update();
  }

  protected void onSetActive(final boolean active) {
    final String tooltip = KeymapUtil.createTooltipText(
      ActionLocalize.actionFilechooserTogglepathshowingText().get(),
      ActionManager.getInstance().getAction("FileChooser.TogglePathShowing")
    );
    setToolTipText(tooltip);
  }

  protected String getStatusBarText() {
    return ActionLocalize.actionFilechooserTogglepathshowingText().get();
  }

  public void update() {
    setVisible(true);
    setText(
      PropertiesComponent.getInstance().getBoolean(FileChooserDialogImpl.FILE_CHOOSER_SHOW_PATH_PROPERTY, true)
        ? IdeLocalize.fileChooserHidePath().get() : IdeLocalize.fileChooserShowPath().get()
    );
  }
}
