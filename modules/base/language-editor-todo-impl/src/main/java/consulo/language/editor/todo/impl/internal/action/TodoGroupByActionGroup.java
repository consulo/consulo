// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.todo.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.language.editor.todo.impl.internal.localize.LanguageTodoLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2025-09-17
 */
@ActionImpl(
    id = "TodoViewGroupByGroup",
    children = {
        @ActionRef(type = TodoGroupByModulesAction.class),
        @ActionRef(type = TodoGroupByPackagesAction.class),
        @ActionRef(type = TodoGroupByFlattenPackages.class)
    }
)
public class TodoGroupByActionGroup extends DefaultActionGroup {
    public TodoGroupByActionGroup() {
        super(LanguageTodoLocalize.groupGroupBy(), LocalizeValue.empty(), PlatformIconGroup.actionsGroupby());
        setPopup(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            this,
            e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        ).showUnderneathOf(e.getInputEvent().getComponent());
    }
}