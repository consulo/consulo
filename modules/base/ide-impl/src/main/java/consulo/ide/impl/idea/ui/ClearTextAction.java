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
package consulo.ide.impl.idea.ui;

import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIExAWTDataKey;
import jakarta.annotation.Nonnull;

import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * @author Anna.Kozlova
 */
public class ClearTextAction extends AnAction implements DumbAware {

  public ClearTextAction() {
    setEnabledInModalContext(true);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Component component = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
    if (component instanceof JTextComponent) {
      final JTextComponent textComponent = (JTextComponent)component;
      textComponent.setText("");
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    final Component component = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
    if (component instanceof JTextComponent) {
      final JTextComponent textComponent = (JTextComponent)component;
      e.getPresentation().setEnabled(textComponent.getText().length() > 0 && ((JTextComponent)component).isEditable());
    }
    else {
      e.getPresentation().setEnabled(false);
    }
  }
}