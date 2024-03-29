/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.ui;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.action.ComboBoxAction;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
class ComboBoxVisibilityGroup extends ComboBoxAction {
  private String myValue;
  private DefaultActionGroup myGroup;
  private Map<String, String> myMap = new HashMap<String, String>();

  protected ComboBoxVisibilityGroup(final String[] options, String[] presentableNames, final Runnable run) {
    final AnAction[] myActions = new AnAction[options.length];
    for (int i = 0; i < options.length; i++) {
      final String value = options[i];
      final String name = presentableNames[i];
      myMap.put(value, name);
      myActions[i] = new AnAction(name) {
        @Override
        public void actionPerformed(AnActionEvent e) {
          setValue(value);
          run.run();
        }
      };
    }
    myGroup = new DefaultActionGroup(myActions);
  }

  public String getValue() {
    return myValue;
  }

  @Nonnull
  @Override
  public DefaultActionGroup createPopupActionGroup(JComponent component) {
    return myGroup;
  }

  public void setValue(String value) {
    getTemplatePresentation().setText(myMap.get(value));
    myValue = value;
  }
}
