// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.action.toolbar;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.animation.AlphaAnimated;
import consulo.desktop.awt.ui.animation.AlphaAnimationContext;
import consulo.desktop.awt.ui.plaf2.flat.InplaceComponent;
import consulo.ide.impl.idea.openapi.actionSystem.RightAlignedToolbarAction;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SimpleActionToolbarImpl extends JToolBar implements DesktopAWTActionToolbar, QuickActionProvider, AlphaAnimated {
    private static final Logger LOG = Logger.getInstance(SimpleActionToolbarImpl.class);

    protected static final String RIGHT_ALIGN_KEY = "RIGHT_ALIGN";

    @Nonnull
    private final Style myStyle;

    private final Throwable myCreationTrace = new Throwable("toolbar creation trace");

    private JComponent myTargetComponent;

    private boolean myShowSeparatorTitles;

    private final AlphaAnimationContext myAlphaContext = new AlphaAnimationContext(this);

    protected final ActionToolbarEngine myEngine;

    private final DataManager myDataManager;

    public SimpleActionToolbarImpl(@Nonnull String place,
                                   @Nonnull ActionGroup actionGroup,
                                   @Nonnull Style style) {
        super(null);
        myStyle = style;
        myAlphaContext.getAnimator().setVisibleImmediately(true);
        myDataManager = DataManager.getInstance();
        myEngine = new ActionToolbarEngine(place, actionGroup, this, Application.get(), KeymapManager.getInstance(), ActionManager.getInstance(), this) {
            @Override
            protected DataContext getDataContext() {
                return SimpleActionToolbarImpl.this.getDataContext();
            }

            @Override
            protected void fillToolBar(List<? extends AnAction> visibleActions, boolean shouldRebuildUI) {
                SimpleActionToolbarImpl.this.actionsUpdated(visibleActions, shouldRebuildUI);
            }

            @Override
            protected boolean isShowing() {
                return SimpleActionToolbarImpl.this.isShowing();
            }

            @Override
            protected void removeAll() {
                SimpleActionToolbarImpl.this.removeAll();
            }
        };

        setOrientation(style.isHorizontal() ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL);

        // If the panel doesn't handle mouse event then it will be passed to its parent.
        // It means that if the panel is in sliding mode then the focus goes to the editor
        // and panel will be automatically hidden.
        enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
        
        setMiniMode(false);
    }

    @Override
    @Nonnull
    public Style getStyle() {
        return myStyle;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        for (Component component : getComponents()) {
            tweakActionComponentUI(component);
        }
    }

    @Nonnull
    public String getPlace() {
        return myEngine.getPlace();
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

    private boolean isInsideNavBar() {
        return ActionPlaces.NAVIGATION_BAR_TOOLBAR.equals(myEngine.getPlace());
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public int getLayoutPolicy() {
        return AUTO_LAYOUT_POLICY;
    }

    @Override
    public void setLayoutPolicy(int layoutPolicy) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Nonnull
    @Override
    public AlphaAnimationContext getAlphaContext() {
        return myAlphaContext;
    }

    @Override
    public void paint(Graphics g) {
        myAlphaContext.paint(g, () -> super.paint(g));
    }

    private void actionsUpdated(@Nonnull List<? extends AnAction> visibleActions, boolean shouldRebuildUI) {
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
        removeAll();

        boolean isLastElementSeparator = false;
        final List<AnAction> rightAligned = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            AnAction action = actions.get(i);
            if (action instanceof RightAlignedToolbarAction) {
                rightAligned.add(action);
                continue;
            }

            if (action instanceof AnSeparator separator) {
                if (isLastElementSeparator) {
                    continue;
                }
                if (i > 0 && i < actions.size() - 1) {
                    add(
                        SEPARATOR_CONSTRAINT,
                        new ActionToolbarSeparator(this, myShowSeparatorTitles ? separator.getTextValue() : LocalizeValue.empty())
                    );
                    isLastElementSeparator = true;
                    continue;
                }
            }
            else if (action instanceof CustomComponentAction) {
                add(CUSTOM_COMPONENT_CONSTRAINT, getCustomComponent(action));
            }
            else {
                add(ACTION_BUTTON_CONSTRAINT, createToolbarButton(action).getComponent());
            }
            isLastElementSeparator = false;
        }

        for (AnAction action : rightAligned) {
            JComponent button = action instanceof CustomComponentAction ? getCustomComponent(action) : createToolbarButton(action).getComponent();
            if (!isInsideNavBar()) {
                button.putClientProperty(RIGHT_ALIGN_KEY, Boolean.TRUE);
            }
            add(button);
        }
    }

    @Nonnull
    private JComponent getCustomComponent(@Nonnull AnAction action) {
        Presentation presentation = myEngine.getPresentation(action);
        JComponent customComponent = presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY);
        if (customComponent == null) {
            customComponent = ((CustomComponentAction) action).createCustomComponent(presentation, getPlace());
            presentation.putClientProperty(CustomComponentAction.COMPONENT_KEY, customComponent);
            UIUtil.putClientProperty(customComponent, CustomComponentAction.ACTION_KEY, action);
        }
        tweakActionComponentUI(customComponent);

        return customComponent;
    }

    protected void tweakActionComponentUI(@Nonnull Component actionComponent) {
        if (ActionPlaces.EDITOR_TOOLBAR.equals(getPlace())) {
            // tweak font & color for editor toolbar to match editor tabs style
            actionComponent.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
            actionComponent.setForeground(ColorUtil.dimmer(JBColor.BLACK));
        }

        if (myStyle == Style.INPLACE && actionComponent instanceof JComponent jComponent) {
            InplaceComponent.prepareLeadingOrTrailingComponent(jComponent);
        }
    }

    @Nonnull
    protected ActionButton createToolbarButton(@Nonnull AnAction action,
                                               @Nonnull String place,
                                               @Nonnull Presentation presentation) {
        ActionButton actionButton;
        if (action instanceof Toggleable toggleable) {
            actionButton = new ActionToggleToolbarButtonImpl(action, presentation, place, false) {
                @Override
                protected DataContext getDataContext() {
                    return getToolbarDataContext();
                }
            };
        }
        else {
            actionButton = new ActionToolbarButtonImpl(action, presentation, place, false) {
                @Override
                protected DataContext getDataContext() {
                    return getToolbarDataContext();
                }
            };
        }

        tweakActionComponentUI(actionButton.getComponent());

        return actionButton;
    }

    @Nonnull
    private ActionButton createToolbarButton(@Nonnull AnAction action) {
        return createToolbarButton(action, getPlace(), myEngine.getPresentation(action));
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
    @Override
    public DataContext getToolbarDataContext() {
        return getDataContext();
    }

    @Override
    public void setShowSeparatorTitles(boolean showSeparatorTitles) {
        myShowSeparatorTitles = showSeparatorTitles;
    }

    @Nonnull
    protected DataContext getDataContext() {
        if (myTargetComponent == null && getClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING) == null) {
            putClientProperty(SUPPRESS_TARGET_COMPONENT_WARNING, true);
            LOG.warn("'" + getPlace() + "' toolbar by default uses any focused component to update its actions. " +
                "Toolbar actions that need local UI context would be incorrectly disabled. " +
                "Please call toolbar.setTargetComponent() explicitly.", myCreationTrace);
        }
        Component target = myTargetComponent != null ? myTargetComponent : IJSwingUtilities.getFocusedComponentInWindowOrSelf(this);
        return myDataManager.getDataContext(target);
    }

    @Nonnull
    @Override
    public List<AnAction> getActions(boolean originalProvider) {
        return getActions();
    }

    @Nonnull
    @Override
    public List<AnAction> getActions() {
        return myEngine.getActions();
    }

    public Presentation getPresentation(AnAction action) {
        return myEngine.getPresentation(action);
    }

    /**
     * Clear internal caches.
     * <p>
     * This method can be called after updating {@link SimpleActionToolbarImpl#myActionGroup}
     * to make sure toolbar does not reference old {@link AnAction} instances.
     */
    @Override
    @RequiredUIAccess
    public void reset() {
        myEngine.reset();
    }
}
