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
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.intention.AbstractIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.util.containers.ContainerUtil.ar;

/**
 * @author Dmitry Avdeev
 */
class GutterIntentionAction extends AbstractIntentionAction implements Comparable<IntentionAction>, Iconable, ShortcutProvider {
  private final AnAction myAction;
  private final int myOrder;
  private final Image myIcon;
  private String myText;

  private GutterIntentionAction(AnAction action, int order, Image icon) {
    myAction = action;
    myOrder = order;
    myIcon = icon;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final RelativePoint relativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(editor);
    myAction.actionPerformed(
            new AnActionEvent(relativePoint.toMouseEvent(), ((EditorEx)editor).getDataContext(), myText, new Presentation(),
                              ActionManager.getInstance(), 0));
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    if (myText != null) return StringUtil.isNotEmpty(myText);

    return isAvailable(createActionEvent((EditorEx)editor));
  }

  @Nonnull
  private static AnActionEvent createActionEvent(EditorEx editor) {
    return AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, editor.getDataContext());
  }

  private boolean isAvailable(@Nonnull AnActionEvent event) {
    if (myText == null) {
      myAction.update(event);
      String text = event.getPresentation().getText();
      myText = text != null ? text : StringUtil.notNullize(myAction.getTemplatePresentation().getText());
    }
    return StringUtil.isNotEmpty(myText);
  }

  @Override
  @Nonnull
  public String getText() {
    return StringUtil.notNullize(myText);
  }

  static void addActions(@Nonnull Editor hostEditor,
                         @Nonnull ShowIntentionsPass.IntentionsInfo intentions, Project project, List<RangeHighlighterEx> result) {
    AnActionEvent event = createActionEvent((EditorEx)hostEditor);
    for (RangeHighlighterEx highlighter : result) {
      addActions(project, highlighter, intentions.guttersToShow, event);
    }
  }

  private static void addActions(@Nonnull Project project,
                                 @Nonnull RangeHighlighterEx info,
                                 @Nonnull List<HighlightInfo.IntentionActionDescriptor> descriptors,
                                 @Nonnull AnActionEvent event) {
    final GutterIconRenderer r = info.getGutterIconRenderer();
    if (r == null || DumbService.isDumb(project) && !DumbService.isDumbAware(r)) {
      return;
    }
    List<HighlightInfo.IntentionActionDescriptor> list = new ArrayList<>();
    for (AnAction action : ar(r.getClickAction(), r.getMiddleButtonClickAction(), r.getRightButtonClickAction(), r.getPopupMenuActions())) {
      if (action != null) {
        addActions(action, list, r, 0, event);
      }
    }

    if (list.isEmpty()) return;
    if (list.size() == 1) {
      descriptors.addAll(list);
    }
    else {
      HighlightInfo.IntentionActionDescriptor first = list.get(0);
      List<IntentionAction> options = ContainerUtil.map(list.subList(1, list.size()), HighlightInfo.IntentionActionDescriptor::getAction);
      descriptors.add(new HighlightInfo.IntentionActionDescriptor(first.getAction(), options, null, first.getIcon()));
    }
  }

  private static void addActions(@Nonnull AnAction action,
                                 @Nonnull List<HighlightInfo.IntentionActionDescriptor> descriptors,
                                 @Nonnull GutterIconRenderer renderer,
                                 int order,
                                 @Nonnull AnActionEvent event) {
    if (action instanceof ActionGroup) {
      AnAction[] children = ((ActionGroup)action).getChildren(null);
      for (int i = 0; i < children.length; i++) {
        addActions(children[i], descriptors, renderer, i + order, event);
      }
    }
    Image icon = TargetAWT.from(action.getTemplatePresentation().getIcon());
    if (icon == null) icon = renderer.getIcon();
    if (icon.getWidth() < 16) icon = ImageEffects.resize(icon, 16, 16);
    final GutterIntentionAction gutterAction = new GutterIntentionAction(action, order, icon);
    if (!gutterAction.isAvailable(event)) return;
    descriptors.add(new HighlightInfo.IntentionActionDescriptor(gutterAction, Collections.emptyList(), null, icon) {
      @Nullable
      @Override
      public String getDisplayName() {
        return gutterAction.getText();
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  public int compareTo(@Nonnull IntentionAction o) {
    if (o instanceof GutterIntentionAction) {
      return myOrder - ((GutterIntentionAction)o).myOrder;
    }
    return 0;
  }

  @Override
  public Image getIcon(@IconFlags int flags) {
    return myIcon;
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    return myAction.getShortcutSet();
  }
}
