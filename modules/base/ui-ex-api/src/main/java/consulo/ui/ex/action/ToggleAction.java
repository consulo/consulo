// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.action;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

/**
 * An action which has a selected state, and which toggles its selected state when performed.
 * Can be used to represent a menu item with a checkbox, or a toolbar button which keeps its pressed state.
 */
public abstract class ToggleAction extends AnAction implements Toggleable {
    public ToggleAction() {
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ToggleAction(@Nullable  String text) {
        super(text);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ToggleAction(
        @Nullable  String text,
        @Nullable  String description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    protected ToggleAction(LocalizeValue text) {
        super(text);
    }

    public ToggleAction(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected ToggleAction(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    @Override
    @RequiredUIAccess
    public final void actionPerformed(AnActionEvent e) {
        boolean state = !isSelected(e);
        setSelected(e, state);
        Presentation presentation = e.getPresentation();
        Toggleable.setSelected(presentation, state);
    }

    /**
     * Returns the selected (checked, pressed) state of the action.
     *
     * @param e the action event representing the place and context in which the selected state is queried.
     * @return true if the action is selected, false otherwise
     */
    public abstract boolean isSelected(AnActionEvent e);

    /**
     * Sets the selected state of the action to the specified value.
     *
     * @param e     the action event which caused the state change.
     * @param state the new selected state of the action.
     */
    @RequiredUIAccess
    public abstract void setSelected(AnActionEvent e, boolean state);

    @Override
    public void update(AnActionEvent e) {
        boolean selected = isSelected(e);
        Presentation presentation = e.getPresentation();

        Toggleable.setSelected(presentation, selected);

        if (e.isFromContextMenu()) {
            //force to show check marks instead of toggled icons in context menu
            presentation.setIcon(null);
        }
    }
}
