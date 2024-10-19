/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.annotation.DeprecationInfo;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.Toggleable;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.AnActionButton;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public abstract class ToggleActionButton extends AnActionButton implements Toggleable {
    public ToggleActionButton(@Nonnull LocalizeValue text, Image icon) {
        super(text, LocalizeValue.empty(), icon);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public ToggleActionButton(String text, Image icon) {
        super(text, null, icon);
    }

    protected ToggleActionButton(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected ToggleActionButton(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected ToggleActionButton(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
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
    public abstract void setSelected(AnActionEvent e, boolean state);

    @Override
    public final void actionPerformed(AnActionEvent e) {
        final boolean state = !isSelected(e);
        setSelected(e, state);
        final Boolean selected = state ? Boolean.TRUE : Boolean.FALSE;
        final Presentation presentation = e.getPresentation();
        presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, selected);
    }

    @Override
    public final void updateButton(AnActionEvent e) {
        final Boolean selected = isSelected(e) ? Boolean.TRUE : Boolean.FALSE;
        final Presentation presentation = e.getPresentation();
        presentation.putClientProperty(Toggleable.SELECTED_PROPERTY, selected);
    }
}
