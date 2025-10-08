// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.internal.intention;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.component.util.Iconable;
import consulo.dataContext.DataContext;
import consulo.language.editor.inspection.PriorityAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class GutterIntentionAction implements Comparable<IntentionAction>, Iconable, ShortcutProvider, PriorityAction, SyntheticIntentionAction {
    private final AnAction myAction;
    private final int myOrder;
    private final Image myIcon;

    private LocalizeValue myTextValue = LocalizeValue.of();

    public GutterIntentionAction(AnAction action, int order, Image icon) {
        myAction = action;
        myOrder = order;
        myIcon = icon;
    }

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        RelativePoint relativePoint = EditorPopupHelper.getInstance().guessBestPopupLocation(editor);
        myAction.actionPerformed(new AnActionEvent(
            relativePoint.toMouseEvent(),
            editor.getDataContext(),
            ActionPlaces.INTENTION_MENU,
            new Presentation(),
            ActionManager.getInstance(),
            0
        ));
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return myTextValue != LocalizeValue.of() || isAvailable(editor.getDataContext());
    }

    @Nonnull
    @Override
    public Priority getPriority() {
        return myAction instanceof PriorityAction priorityAction ? priorityAction.getPriority() : Priority.NORMAL;
    }

    @Nonnull
    static AnActionEvent createActionEvent(@Nonnull DataContext dataContext) {
        return AnActionEvent.createFromDataContext(ActionPlaces.INTENTION_MENU, null, dataContext);
    }

    public boolean isAvailable(@Nonnull DataContext dataContext) {
        if (myTextValue == null) {
            AnActionEvent event = createActionEvent(dataContext);
            myAction.update(event);
            if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
                LocalizeValue text = event.getPresentation().getTextValue();
                myTextValue = text != LocalizeValue.of() ? text : myAction.getTemplatePresentation().getTextValue();
            }
            else {
                myTextValue = LocalizeValue.of();
            }
        }
        return myTextValue != LocalizeValue.of();
    }

    @Override
    @Nonnull
    public LocalizeValue getText() {
        return myTextValue;
    }

    @Override
    public int compareTo(@Nonnull IntentionAction o) {
        if (o instanceof GutterIntentionAction gutterIntentionAction) {
            return myOrder - gutterIntentionAction.myOrder;
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
