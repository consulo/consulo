/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.action.toolbar;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.HorizontalLayout;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2024-12-31
 */
public class ActionButtonToolbarImpl extends JPanel implements DesktopAWTActionToolbar {
    private static final Logger LOG = Logger.getInstance(ActionButtonToolbarImpl.class);

    private final ActionToolbarEngine myEngine;
    @Nonnull
    private final DataManager myDataManager;

    private JComponent myTargetComponent;

    private final Throwable myCreationTrace = new Throwable("toolbar creation trace");

    public ActionButtonToolbarImpl(@Nonnull String place,
                                   @Nonnull ActionGroup actionGroup,
                                   @Nonnull Application application,
                                   @Nonnull KeymapManager keymapManager,
                                   @Nonnull ActionManager actionManager,
                                   @Nonnull DataManager dataManager) {
        super(new HorizontalLayout(4));
        myDataManager = dataManager;
        myEngine = new ActionToolbarEngine(place, actionGroup, this, application, keymapManager, actionManager, this) {
            @Override
            protected DataContext getDataContext() {
                return ActionButtonToolbarImpl.this.getDataContext();
            }

            @Override
            protected void fillToolBar(List<? extends AnAction> visibleActions, boolean shouldRebuildUI) {
                refill(visibleActions, shouldRebuildUI);
            }

            @Override
            protected boolean isShowing() {
                return ActionButtonToolbarImpl.this.isShowing();
            }

            @Override
            protected void removeAll() {
                ActionButtonToolbarImpl.this.removeAll();
            }
        };
    }

    private void refill(List<? extends AnAction> visibleActions, boolean shouldRebuildUI) {
        Dimension oldSize = getPreferredSize();

        removeAll();

        fillToolBar(visibleActions);

        Dimension newSize = getPreferredSize();

        ((WindowManagerEx) WindowManager.getInstance()).adjustContainerWindow(this, oldSize, newSize);

        if (shouldRebuildUI) {
            revalidate();
        }
        else {
            Container parent = getParent();
            if (parent != null) {
                parent.invalidate();
                parent.validate();
            }
        }

        repaint();
    }

    private void fillToolBar(@Nonnull final List<? extends AnAction> actions) {
        boolean isLastElementSeparator = false;
        for (int i = 0; i < actions.size(); i++) {
            AnAction action = actions.get(i);

            if (action instanceof AnSeparator separator) {
                if (isLastElementSeparator) {
                    continue;
                }
                if (i > 0 && i < actions.size() - 1) {
                    add(
                        SEPARATOR_CONSTRAINT,
                        new ActionToolbarSeparator(this, LocalizeValue.empty())
                    );
                    isLastElementSeparator = true;
                    continue;
                }
            }
            else if (action instanceof CustomComponentAction) {
                add(getCustomComponent(action));
            }
            else {
                add(createToolbarButton(action).getComponent());
            }
            isLastElementSeparator = false;
        }
    }

    @Nonnull
    @RequiredUIAccess
    private JComponent getCustomComponent(@Nonnull AnAction action) {
        Presentation presentation = myEngine.getPresentation(action);
        JComponent customComponent = presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY);
        if (customComponent == null) {
            customComponent = ((CustomComponentAction) action).createCustomComponent(presentation, myEngine.getPlace());
            presentation.putClientProperty(CustomComponentAction.COMPONENT_KEY, customComponent);
            UIUtil.putClientProperty(customComponent, CustomComponentAction.ACTION_KEY, action);
        }

        tweakActionComponentUI(action, customComponent);

        return customComponent;
    }

    @Nonnull
    protected ActionButton createToolbarButton(@Nonnull AnAction action,
                                               @Nonnull String place,
                                               @Nonnull Presentation presentation) {

        ActionButton actionButton;
        if (action instanceof Toggleable toggleable) {
            actionButton = new ActionToggleToolbarButtonImpl(action, presentation, place, true) {
                @Override
                protected DataContext getDataContext() {
                    return getToolbarDataContext();
                }
            };
        }
        else {
            actionButton = new ActionToolbarButtonImpl(action, presentation, place, true) {
                @Override
                protected DataContext getDataContext() {
                    return getToolbarDataContext();
                }
            };
        }

        tweakActionComponentUI(action, actionButton.getComponent());

        return actionButton;
    }

    @Nonnull
    private ActionButton createToolbarButton(@Nonnull AnAction action) {
        return createToolbarButton(action, myEngine.getPlace(), myEngine.getPresentation(action));
    }

    protected void tweakActionComponentUI(@Nonnull AnAction action, Component component) {
    }

    @Override
    @RequiredUIAccess
    public void addNotify() {
        super.addNotify();

        myEngine.addNotify();
    }

    @Override
    @RequiredUIAccess
    public void removeNotify() {
        super.removeNotify();

        myEngine.removeNotify();
    }

    @Override
    public void reset() {
        myEngine.reset();
    }

    @Override
    public int getLayoutPolicy() {
        return AUTO_LAYOUT_POLICY;
    }

    @Override
    public void setLayoutPolicy(int layoutPolicy) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public void updateActionsImmediately() {
        myEngine.updateActionsImmediately();
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public CompletableFuture<?> updateActionsAsync() {
        return myEngine.updateActionsAsync();
    }

    @Override
    public boolean hasVisibleActions() {
        return myEngine.hasVisibleActions();
    }

    @Override
    public DataContext getToolbarDataContext() {
        return getDataContext();
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Nonnull
    @Override
    public consulo.ui.Component getUIComponent() {
        return TargetAWT.wrap(this);
    }

    @Override
    public void setTargetComponent(final JComponent component) {
        if (myTargetComponent == null) {
            putClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING, true);
        }

        myTargetComponent = component;

        if (myTargetComponent != component) {
            myTargetComponent = component;
            if (isShowing()) {
                updateActionsImmediately();
            }
        }
    }

    @Nonnull
    protected DataContext getDataContext() {
        if (myTargetComponent == null && getClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING) == null) {
            putClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING, true);
            LOG.warn("'" + myEngine.getPlace() + "' toolbar by default uses any focused component to update its actions. " +
                "Toolbar actions that need local UI context would be incorrectly disabled. " +
                "Please call toolbar.setTargetComponent() explicitly.", myCreationTrace);
        }
        Component target = myTargetComponent != null ? myTargetComponent : IJSwingUtilities.getFocusedComponentInWindowOrSelf(this);
        return myDataManager.getDataContext(target);
    }

    @Nonnull
    @Override
    public List<AnAction> getActions() {
        return myEngine.getActions();
    }

    @Override
    public Style getStyle() {
        return Style.BUTTON;
    }
}
