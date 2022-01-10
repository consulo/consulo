/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.AnSeparator;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnnotateActionGroup extends ActionGroup {
  private final AnAction[] myActions;

  public AnnotateActionGroup(@Nonnull List<AnnotationFieldGutter> gutters,
                             @Nonnull EditorGutterComponentEx gutterComponent,
                             @Nullable Couple<Map<VcsRevisionNumber, ColorValue>> bgColorMap) {
    super("View", true);
    final List<AnAction> actions = new ArrayList<>();
    for (AnnotationFieldGutter g : gutters) {
      if (g.getID() != null) {
        actions.add(new ShowHideAspectAction(g, gutterComponent));
      }
    }
    actions.add(AnSeparator.getInstance());
    if (bgColorMap != null) {
      actions.add(new ShowAnnotationColorsAction(gutterComponent));
    }
    actions.add(new ShowShortenNames(gutterComponent));
    myActions = actions.toArray(new AnAction[actions.size()]);
  }

  @Nonnull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return myActions;
  }
}
