// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.dataContext.DataContext;
import consulo.ui.ex.localize.UILocalize;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ide.impl.idea.util.LineSeparator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class LineSeparatorPanel extends EditorBasedStatusBarPopup {
    public LineSeparatorPanel(@Nonnull Project project, @Nonnull StatusBarWidgetFactory factory) {
        super(project, factory, true);
    }

    @Nonnull
    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile file) {
        if (file == null) {
            return WidgetState.HIDDEN;
        }
        String lineSeparator = LoadTextUtil.detectLineSeparator(file, true);
        if (lineSeparator == null) {
            return WidgetState.HIDDEN;
        }
        LocalizeValue toolTipText = IdeLocalize.tooltipLineSeparator(StringUtil.escapeLineBreak(lineSeparator));
        LocalizeValue panelText = LocalizeValue.of(LineSeparator.fromString(lineSeparator).toString());
        return new WidgetState(toolTipText, panelText, true);
    }

    @Nullable
    @Override
    protected ListPopup createPopup(DataContext context) {
        AnAction group = ActionManager.getInstance().getAction("ChangeLineSeparators");
        if (!(group instanceof ActionGroup actionGroup)) {
            return null;
        }

        return JBPopupFactory.getInstance()
            .createActionGroupPopup(UILocalize.statusBarLineSeparatorWidgetName().get(), actionGroup, context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }

    @Nonnull
    @Override
    protected StatusBarWidget createInstance(@Nonnull Project project) {
        return new LineSeparatorPanel(project, myFactory);
    }
}
