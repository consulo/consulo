/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.gutter;

import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ShortcutSet;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.MouseEvent;

/**
 * @author Dmitry Avdeev
 */
public class NavigateAction<T extends PsiElement> extends AnAction {
  private final LineMarkerInfo<T> myInfo;

  public NavigateAction(@Nonnull String text,
                        @Nonnull LineMarkerInfo<T> info,
                        @Nullable String originalActionId) {
    super(text);
    myInfo = info;
    if (originalActionId != null) {
      ShortcutSet set = ActionManager.getInstance().getAction(originalActionId).getShortcutSet();
      setShortcutSet(set);
    }
  }

  public NavigateAction(@Nonnull LineMarkerInfo<T> info) {
    myInfo = info;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(AnActionEvent e) {
    if (myInfo.getNavigationHandler() != null) {
      MouseEvent mouseEvent = (MouseEvent)e.getInputEvent();
      T element = myInfo.getElement();
      if (element == null || !element.isValid()) return;

      myInfo.getNavigationHandler().navigate(mouseEvent, element);
    }
  }

  @Nonnull
  public static <T extends PsiElement> LineMarkerInfo<T> setNavigateAction(@Nonnull LineMarkerInfo<T> info, @Nonnull String text, @Nullable String originalActionId) {
    NavigateAction<T> action = new NavigateAction<T>(text, info, originalActionId);
    info.setNavigateAction(action);
    return info;
  }
}
