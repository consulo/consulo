/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ui.ex.action.util;

import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionManagerEx;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionGroupUtil {
  private static Presentation getPresentation(AnAction action, Map<AnAction, Presentation> action2presentation) {
    return action2presentation.computeIfAbsent(action, k -> action.getTemplatePresentation().clone());
  }

  public static boolean isGroupEmpty(@Nonnull ActionGroup actionGroup, @Nonnull AnActionEvent e) {
    return isGroupEmpty(actionGroup, e, new HashMap<>());
  }

  @Deprecated
  public static boolean isGroupEmpty(@Nonnull ActionGroup actionGroup, @Nonnull AnActionEvent e, boolean unused) {
    return isGroupEmpty(actionGroup, e, new HashMap<>());
  }

  private static boolean isGroupEmpty(@Nonnull ActionGroup actionGroup, @Nonnull AnActionEvent e, @Nonnull Map<AnAction, Presentation> action2presentation) {
    AnAction[] actions = actionGroup.getChildren(e);
    for (AnAction action : actions) {
      if (action instanceof AnSeparator) continue;
      if (isActionEnabledAndVisible(e, action2presentation, action)) {
        if (action instanceof ActionGroup) {
          if (!isGroupEmpty((ActionGroup)action, e, action2presentation)) {
            return false;
          }
          // else continue for-loop
        }
        else {
          return false;
        }
      }
    }
    return true;
  }

  @Nullable
  public static AnAction getSingleActiveAction(@Nonnull ActionGroup actionGroup, @Nonnull AnActionEvent e, boolean isInModalContext) {
    List<AnAction> children = getEnabledChildren(actionGroup, e, new HashMap<>());
    if (children.size() == 1) {
      return children.get(0);
    }
    return null;
  }

  private static List<AnAction> getEnabledChildren(@Nonnull ActionGroup actionGroup, @Nonnull AnActionEvent e, @Nonnull Map<AnAction, Presentation> action2presentation) {
    List<AnAction> result = new ArrayList<>();
    AnAction[] actions = actionGroup.getChildren(e);
    for (AnAction action : actions) {
      if (action instanceof ActionGroup) {
        if (isActionEnabledAndVisible(e, action2presentation, action)) {
          result.addAll(getEnabledChildren((ActionGroup)action, e, action2presentation));
        }
      }
      else if (!(action instanceof AnSeparator)) {
        if (isActionEnabledAndVisible(e, action2presentation, action)) {
          result.add(action);
        }
      }
    }
    return result;
  }

  private static boolean isActionEnabledAndVisible(@Nonnull final AnActionEvent e, @Nonnull final Map<AnAction, Presentation> action2presentation, @Nonnull final AnAction action) {
    Presentation presentation = getPresentation(action, action2presentation);
    AnActionEvent event = new AnActionEvent(e.getInputEvent(), e.getDataContext(), ActionPlaces.UNKNOWN, presentation, ActionManager.getInstance(), e.getModifiers());
    event.setInjectedContext(action.isInInjectedContext());

    ((ActionManagerEx)ActionManager.getInstance()).performDumbAwareUpdate(action, event, false);

    return presentation.isEnabled() && presentation.isVisible();
  }
}
