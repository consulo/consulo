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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.application.AllIcons;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author Denis Zhdanov
 * @since 8/23/12 11:41 AM
 */
public class ArrangementRemoveConditionAction extends AnAction {

  public ArrangementRemoveConditionAction() {
    getTemplatePresentation().setIcon(AllIcons.Actions.Close);
    getTemplatePresentation().setHoveredIcon(AllIcons.Actions.CloseHovered);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
  }
}
