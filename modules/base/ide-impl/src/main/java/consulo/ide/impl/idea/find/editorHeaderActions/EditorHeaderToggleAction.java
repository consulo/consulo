// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.annotation.DeprecationInfo;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.find.SearchSession;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CheckboxAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class EditorHeaderToggleAction extends CheckboxAction implements DumbAware {
    protected EditorHeaderToggleAction(@Nonnull LocalizeValue text) {
        this(text, null, null, null);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    protected EditorHeaderToggleAction(@Nonnull String text) {
        this(text, null, null, null);
    }

    protected EditorHeaderToggleAction(
        @Nonnull LocalizeValue text,
        @Nullable Image icon,
        @Nullable Image hoveredIcon,
        @Nullable Image selectedIcon
    ) {
        super(text);
        getTemplatePresentation().setIcon(icon);
        getTemplatePresentation().setHoveredIcon(hoveredIcon);
        getTemplatePresentation().setSelectedIcon(selectedIcon);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    protected EditorHeaderToggleAction(
        @Nonnull String text,
        @Nullable Image icon,
        @Nullable Image hoveredIcon,
        @Nullable Image selectedIcon
    ) {
        this(LocalizeValue.of(text), icon, hoveredIcon, selectedIcon);
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public CheckBox createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
        CheckBox customComponent = super.createCustomComponent(presentation, place);
        customComponent.setFocusable(false);
        return customComponent;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        SearchSession search = e.getData(SearchSession.KEY);
        return search != null && isSelected(search);
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean selected) {
        SearchSession search = e.getData(SearchSession.KEY);
        if (search != null) {
            setSelected(search, selected);
        }
    }

    protected abstract boolean isSelected(@Nonnull SearchSession session);

    protected abstract void setSelected(@Nonnull SearchSession session, boolean selected);
}
