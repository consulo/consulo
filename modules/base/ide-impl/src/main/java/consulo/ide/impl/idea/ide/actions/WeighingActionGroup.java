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
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.impl.idea.openapi.actionSystem.impl.BasePresentationFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author peter
 */
abstract class WeighingActionGroup extends ActionGroup {
  private final BasePresentationFactory myPresentationFactory = new BasePresentationFactory();

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    getDelegate().update(e);
  }

  protected abstract ActionGroup getDelegate();

  @Override
  @Nonnull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return getDelegate().getChildren(e);
  }

  @Nonnull
  @Override
  public List<AnAction> postProcessVisibleChildren(@Nonnull List<AnAction> visibleActions) {
    LinkedHashSet<AnAction> heaviest = null;
    double maxWeight = Presentation.DEFAULT_WEIGHT;
    for (AnAction action : visibleActions) {
      Presentation presentation = myPresentationFactory.getPresentation(action);
      if (presentation.isEnabled() && presentation.isVisible()) {
        if (presentation.getWeight() > maxWeight) {
          maxWeight = presentation.getWeight();
          heaviest = new LinkedHashSet<>();
        }
        if (presentation.getWeight() == maxWeight && heaviest != null) {
          heaviest.add(action);
        }
      }
    }

    if (heaviest == null) {
      return visibleActions;
    }

    final ActionGroup.Builder chosen = ActionGroup.newImmutableBuilder();
    boolean prevSeparator = true;
    for (AnAction action : visibleActions) {
      final boolean separator = action instanceof AnSeparator;
      if (separator && !prevSeparator) {
        chosen.add(action);
      }
      prevSeparator = separator;

      if (shouldBeChosenAnyway(action)) {
        heaviest.add(action);
      }

      if (heaviest.contains(action)) {
        chosen.add(action);
      }
    }

    ActionGroup other = new ExcludingActionGroup(getDelegate(), heaviest);
    other.setPopup(true);
    other.getTemplatePresentation().setText("Other...");
    return List.of(chosen.build(), new AnSeparator(), other);
  }

  protected boolean shouldBeChosenAnyway(AnAction action) {
    return false;
  }
}
