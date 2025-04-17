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
package consulo.ui.ex.awt.action;

import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Predicate;

public abstract class ComboBoxAction extends AnAction implements CustomComponentAction {
    private String myPopupTitle;

    protected ComboBoxAction() {
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ComboBoxButton button = (ComboBoxButton)e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY);
        if (button == null) {
            Component contextComponent = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
            JRootPane rootPane = UIUtil.getParentOfType(JRootPane.class, contextComponent);
            if (rootPane != null) {
                button = (ComboBoxButton)UIUtil.uiTraverser()
                    .withRoot(rootPane)
                    .bfsTraversal()
                    .filter(
                        component -> component instanceof ComboBoxButton comboBoxButton
                            && comboBoxButton.getComboBoxAction() == ComboBoxAction.this
                    )
                    .first();
            }
            if (button == null) {
                return;
            }
        }

        button.showPopup();
    }

    @Nonnull
    @Override
    public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
        NonOpaquePanel panel = new NonOpaquePanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(0, 4));
        ComboBoxButton button = createComboBoxButton(presentation);
        panel.add(button.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    @Nonnull
    public JBPopup createPopup(
        @Nonnull JComponent component,
        @Nonnull DataContext context,
        @Nonnull Presentation presentation,
        @Nonnull Runnable onDispose
    ) {
        return createPopup(component, context, onDispose);
    }

    @Nonnull
    public JBPopup createPopup(@Nonnull JComponent component, @Nonnull DataContext context, @Nonnull Runnable onDispose) {
        ActionGroup group = createPopupActionGroup(component, context);

        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            getPopupTitle(),
            group,
            context,
            false,
            shouldShowDisabledActions(),
            false,
            onDispose,
            getMaxRows(),
            getPreselectCondition()
        );
        popup.setMinimumSize(new Dimension(getMinWidth(), getMinHeight()));
        return popup;
    }

    @Nonnull
    protected ComboBoxButton createComboBoxButton(Presentation presentation) {
        return new ComboBoxButtonImpl(this, presentation);
    }

    @Nullable
    public String getTooltipText(@Nonnull ComboBoxButton button) {
        return null;
    }

    public void setPopupTitle(String popupTitle) {
        myPopupTitle = popupTitle;
    }

    public String getPopupTitle() {
        return myPopupTitle;
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
    }

    public boolean shouldShowDisabledActions() {
        return false;
    }

    @Nonnull
    protected abstract ActionGroup createPopupActionGroup(JComponent button);

    @Nonnull
    protected ActionGroup createPopupActionGroup(JComponent button, @Nonnull DataContext dataContext) {
        return createPopupActionGroup(button);
    }

    public int getMaxRows() {
        return 30;
    }

    public int getMinHeight() {
        return 1;
    }

    public int getMinWidth() {
        return 1;
    }

    public Predicate<AnAction> getPreselectCondition() {
        return null;
    }
}
