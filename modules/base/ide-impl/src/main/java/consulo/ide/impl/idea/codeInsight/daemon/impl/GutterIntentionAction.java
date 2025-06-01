// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorPopupHelper;
import consulo.component.util.Iconable;
import consulo.dataContext.DataContext;
import consulo.language.editor.inspection.PriorityAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
@SuppressWarnings("ComparableType")
class GutterIntentionAction implements Comparable<IntentionAction>, Iconable, ShortcutProvider, PriorityAction, SyntheticIntentionAction {
  private final AnAction myAction;
  private final int myOrder;
  private final Image myIcon;
  private String myText;

  GutterIntentionAction(AnAction action, int order, Image icon) {
    myAction = action;
    myOrder = order;
    myIcon = icon;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final RelativePoint relativePoint = EditorPopupHelper.getInstance().guessBestPopupLocation(editor);
    myAction.actionPerformed(new AnActionEvent(relativePoint.toMouseEvent(), ((EditorEx)editor).getDataContext(), ActionPlaces.INTENTION_MENU, new Presentation(), ActionManager.getInstance(), 0));
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return myText != null ? StringUtil.isNotEmpty(myText) : isAvailable(((EditorEx)editor).getDataContext());
  }

  @Nonnull
  @Override
  public Priority getPriority() {
    return myAction instanceof PriorityAction ? ((PriorityAction)myAction).getPriority() : Priority.NORMAL;
  }

  @Nonnull
  static AnActionEvent createActionEvent(@Nonnull DataContext dataContext) {
    return AnActionEvent.createFromDataContext(ActionPlaces.INTENTION_MENU, null, dataContext);
  }

  boolean isAvailable(@Nonnull DataContext dataContext) {
    if (myText == null) {
      AnActionEvent event = createActionEvent(dataContext);
      myAction.update(event);
      if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
        String text = event.getPresentation().getText();
        myText = text != null ? text : StringUtil.notNullize(myAction.getTemplatePresentation().getText());
      }
      else {
        myText = "";
      }
    }
    return StringUtil.isNotEmpty(myText);
  }

  @Override
  @Nonnull
  public String getText() {
    return StringUtil.notNullize(myText);
  }

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
