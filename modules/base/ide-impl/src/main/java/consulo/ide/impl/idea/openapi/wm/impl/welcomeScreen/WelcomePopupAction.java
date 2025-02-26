/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ui.popup.PopupFactoryImpl;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * @author Vladislav.Kaznacheev
 */
public abstract class WelcomePopupAction extends AnAction implements DumbAware {

    protected abstract void fillActions(DefaultActionGroup group);

    @Nonnull
    protected abstract LocalizeValue getTextForEmpty();

    /**
     * When there is only one option to choose from, this method is called to determine whether
     * the popup should still be shown or that the option should be chosen silently.
     *
     * @return true to choose single option silently
     * false otherwise
     */
    protected abstract boolean isSilentlyChooseSingleOption();

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull final AnActionEvent e) {
        final DefaultActionGroup group = new DefaultActionGroup();
        fillActions(group);

        if (group.getChildrenCount() == 1 && isSilentlyChooseSingleOption()) {
            final AnAction[] children = group.getChildren(null);
            children[0].actionPerformed(e);
            return;
        }


        if (group.getChildrenCount() == 0) {
            group.add(new AnAction(getTextForEmpty()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    group.setPopup(false);
                }
            });
        }

        final DataContext context = e.getDataContext();
        final ListPopup popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(null,
                group,
                context,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                true);

        InputDetails inputDetails = Objects.requireNonNull(e.getInputDetails());

        Component component = e.getRequiredData(PlatformDataKeys.CONTEXT_UI_COMPONENT);

        popup.showBy(component, inputDetails);
    }
}
