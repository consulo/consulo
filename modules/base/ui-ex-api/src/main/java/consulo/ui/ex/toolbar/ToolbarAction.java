/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.ex.toolbar;

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.internal.AnActionWithUIUpdate;
import consulo.ui.ex.internal.ToolbarExecutor;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public abstract sealed class ToolbarAction<E> extends AnAction implements DumbAware, AnActionWithUIUpdate
    permits AddAction, RemoveAction, EditAction, UpMoveAction, DownMoveAction {
    protected ToolbarAction(LocalizeValue text, Image icon) {
        super(text, LocalizeValue.empty(), icon);
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public void updateAtUI(AnActionEvent e) {
        ToolbarExecutor<E> executor = e.getData(ToolbarExecutor.KEY);
        if (executor == null) {
            return;
        }

        e.getPresentation().setEnabled(isContextComponentShowingAndEnabled(executor) && isContextComponentStateAllowingAction(executor));
    }

    abstract boolean isEnabled(int size, int min, int max);

    private static boolean isContextComponentShowingAndEnabled(ToolbarExecutor<?> executor) {
        Component component = executor.getComponent();
        return component.isVisible() && component.isEnabled();
    }

    @RequiredUIAccess
    private boolean isContextComponentStateAllowingAction(ToolbarExecutor<E> executor) {
        return isEnabled(executor.getSize(), executor.getMinSelectionIndex(), executor.getMaxSelectionIndex());
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    protected final @Nullable E getSelectedValue(AnActionEvent e) {
        ToolbarExecutor<E> executor = e.getData(ToolbarExecutor.KEY);
        return executor == null ? null : executor.getSelectedValue();
    }

    /**
     * The whole selection, for an action which acts on more than the lead row.
     */
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    protected final List<E> getSelectedValues(AnActionEvent e) {
        ToolbarExecutor<E> executor = e.getData(ToolbarExecutor.KEY);
        return executor == null ? List.of() : executor.getSelectedValues();
    }
}
