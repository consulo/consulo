// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.impl.content;

import consulo.application.ui.UISettings;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.ex.awt.internal.IdeTooltip;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.UIAccess;
import consulo.ide.impl.idea.util.ui.BaseButtonBehavior;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.project.ui.impl.internal.wm.action.CloseContentTabAction;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.TimedDeadzone;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.util.collection.SmartList;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class ContentTabLabel extends BaseLabel {
    private static final int MAX_WIDTH = JBUI.scale(300);
    private static final int DEFAULT_HORIZONTAL_INSET = JBUI.scale(12);
    protected static final int ICONS_GAP = JBUI.scale(3);

    private final Content myContent;
    private final TabContentLayout myLayout;

    private final List<AdditionalIcon> myAdditionalIcons = new SmartList<>();
    private final ActionToolbar myCloseToolbar;
    private final JComponent myCloseToolbarComponent;
    private int myCloseToolbarX;
    private String myText = null;
    private int myIconWithInsetsWidth;

    private CurrentTooltip currentIconTooltip;

    BaseButtonBehavior behavior = new BaseButtonBehavior(this) {
        @Override
        protected void execute(MouseEvent e) {
            Optional<Runnable> first = myAdditionalIcons.stream()
                .filter(icon -> mouseOverIcon(icon))
                .map(AdditionalIcon::getAction)
                .findFirst();

            if (first.isPresent()) {
                first.get().run();
                return;
            }

            selectContent();

            if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1 && !myLayout.myDoubleClickActions.isEmpty()) {
                DataContext dataContext = DataManager.getInstance().getDataContext(ContentTabLabel.this);
                UIAccess uiAccess = UIAccess.current();
                for (AnAction action : myLayout.myDoubleClickActions) {
                    AnActionEvent event = AnActionEvent.createFromInputEvent(e, ActionPlaces.UNKNOWN, null, dataContext);
                    ActionRunnerAsync.lastUpdateAndCheckDumbAsync(action, event, false).whenCompleteAsync((enabled, throwable) -> {
                        if (Boolean.TRUE.equals(enabled)) {
                            ActionManagerEx.getInstanceEx().fireBeforeActionPerformed(action, dataContext, event);
                            ActionImplUtil.performActionDumbAware(action, event);
                        }
                    }, uiAccess);
                }
            }
        }
    };

    private void showTooltip(AdditionalIcon icon) {

        if (icon != null) {
            if (currentIconTooltip != null) {
                if (currentIconTooltip.icon == icon) {
                    IdeTooltipManagerImpl.getInstanceImpl().show(currentIconTooltip.currentTooltip, false, false);
                    return;
                }

                hideCurrentTooltip();
            }

            String toolText = icon.getTooltip();

            if (toolText != null && !toolText.isEmpty()) {
                IdeTooltip tooltip = new IdeTooltip(this, icon.getCenterPoint(), new JLabel(toolText));
                currentIconTooltip = new CurrentTooltip(IdeTooltipManagerImpl.getInstanceImpl().show(tooltip, false, false), icon);
                return;
            }
        }

        hideCurrentTooltip();
        if (myText != null && !myText.equals(getText())) {
            IdeTooltip tooltip = new IdeTooltip(this, getMousePosition(), new JLabel(myText));
            currentIconTooltip = new CurrentTooltip(IdeTooltipManagerImpl.getInstanceImpl().show(tooltip, false, false), null);
        }
    }

    private void hideCurrentTooltip() {
        if (currentIconTooltip == null) {
            return;
        }

        currentIconTooltip.currentTooltip.hide();
        currentIconTooltip = null;
    }

    @RequiredUIAccess
    private void updateCloseToolbar() {
        myCloseToolbar.updateActionsAsync().whenCompleteAsync((actions, throwable) -> relayoutTabs(), UIAccess.current());
    }

    private void relayoutTabs() {
        myLayout.myLastLayout = null;

        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    @Override
    public void setText(String text) {
        myText = text;
        updateText();
    }

    private void updateText() {
        FontMetrics fm = getFontMetrics(getFont());
        float textWidth = BasicGraphicsUtils.getStringWidth(this, fm, myText);
        float prefWidth = myIconWithInsetsWidth + textWidth;

        int maxWidth = getMaximumSize().width;

        if (prefWidth > maxWidth) {
            int offset = maxWidth - myIconWithInsetsWidth;
            String s = BasicGraphicsUtils.getClippedString(this, fm, myText, offset);
            super.setText(s);
            return;
        }

        super.setText(myText);
    }

    protected final boolean mouseOverIcon(AdditionalIcon icon) {
        if (!isHovered() || !icon.getAvailable()) {
            return false;
        }

        PointerInfo info = MouseInfo.getPointerInfo();
        if (info == null) {
            return false;
        }
        Point point = info.getLocation();
        SwingUtilities.convertPointFromScreen(point, this);
        return icon.contains(point);
    }

    ContentTabLabel(Content content, TabContentLayout layout) {
        super(layout.myUi);
        myLayout = layout;
        myContent = content;

        fillIcons(myAdditionalIcons);

        myCloseToolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            ActionPlaces.TOOLWINDOW_TITLE,
            ActionGroup.newImmutableBuilder().add(new CloseContentTabAction(contentManager(), content)).build(),
            ActionToolbar.Style.INPLACE
        );
        myCloseToolbar.setTargetComponent(this);
        myCloseToolbarComponent = myCloseToolbar.getComponent();
        myCloseToolbarComponent.setOpaque(false);
        add(myCloseToolbarComponent);

        behavior.setActionTrigger(MouseEvent.MOUSE_RELEASED);
        behavior.setMouseDeadzone(TimedDeadzone.NULL);

        myContent.addPropertyChangeListener(event -> {
            String property = event.getPropertyName();
            if (Content.IS_CLOSABLE.equals(property) || Content.PROP_PINNED.equals(property)) {
                UIUtil.invokeLaterIfNeeded(this::updateCloseToolbar);
            }
        });

        setMaximumSize(new Dimension(MAX_WIDTH, getMaximumSize().height));
    }

    protected void fillIcons(List<AdditionalIcon> icons) {
    }

    private boolean isCloseButtonAfterText() {
        UISettings settings = UISettings.getShadowInstance();
        return settings.getCloseTabButtonOnTheRight() || !settings.getShowCloseButton();
    }

    private int getCloseToolbarWidth() {
        return canBeClosed() ? myCloseToolbarComponent.getPreferredSize().width : 0;
    }

    @Override
    public void doLayout() {
        super.doLayout();

        boolean visible = canBeClosed();
        myCloseToolbarComponent.setVisible(visible);
        if (visible) {
            Dimension size = myCloseToolbarComponent.getPreferredSize();
            myCloseToolbarComponent.setBounds(myCloseToolbarX, (getHeight() - size.height) / 2, size.width, size.height);
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        super.processMouseMotionEvent(event);

        boolean hovered = isHovered();

        if (hovered) {
            if (invalid()) {
                repaint();
            }

            Optional<AdditionalIcon> first = myAdditionalIcons.stream().filter(this::mouseOverIcon).findFirst();

            if (first.isPresent()) {
                showTooltip(first.get());
                return;
            }
        }

        showTooltip(null);
    }

    protected boolean invalid() {
        return myAdditionalIcons.stream().anyMatch(AdditionalIcon::getAvailable);
    }

    public final boolean canBeClosed() {
        return myContent.isCloseable() && myUi.myWindow.canCloseContents();
    }

    protected void selectContent() {
        ContentManager mgr = contentManager();
        if (mgr.getIndexOfContent(myContent) >= 0) {
            mgr.setSelectedContent(myContent, true);
        }
    }

    public void update() {
        setHorizontalAlignment(SwingConstants.LEFT);
        if (!myLayout.isToDrawTabs()) {
            setBorder(null);
        }

        updateTextAndIcon(myContent);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int iconWidth = 0;
        Map<Boolean, List<AdditionalIcon>> map = new HashMap<>();
        for (AdditionalIcon myAdditionalIcon : myAdditionalIcons) {
            if (myAdditionalIcon.getAvailable()) {
                map.computeIfAbsent(myAdditionalIcon.getAfterText(), k -> new SmartList<>()).add(myAdditionalIcon);
            }
        }

        int closeWidth = getCloseToolbarWidth();
        boolean closeAfterText = isCloseButtonAfterText();

        int right = DEFAULT_HORIZONTAL_INSET;
        int left = DEFAULT_HORIZONTAL_INSET;

        if (map.get(false) != null || closeWidth > 0 && !closeAfterText) {
            iconWidth = ICONS_GAP;

            if (closeWidth > 0 && !closeAfterText) {
                myCloseToolbarX = iconWidth;
                iconWidth += closeWidth + ICONS_GAP;
            }

            for (AdditionalIcon icon : map.getOrDefault(false, List.of())) {
                icon.setX(iconWidth);
                iconWidth += icon.getIconWidth() + ICONS_GAP;
            }

            left = iconWidth;
            iconWidth = 0;
        }

        if (map.get(true) != null || closeWidth > 0 && closeAfterText) {
            right = ICONS_GAP + JBUI.scale(4);

            for (AdditionalIcon icon : map.getOrDefault(true, List.of())) {
                icon.setX(iconWidth + size.width + ICONS_GAP - right);
                iconWidth += icon.getIconWidth() + ICONS_GAP;
            }

            if (closeWidth > 0 && closeAfterText) {
                myCloseToolbarX = iconWidth + size.width + ICONS_GAP - right;
                iconWidth += closeWidth + ICONS_GAP;
            }
        }

        setBorder(new EmptyBorder(0, left, 0, right));
        myIconWithInsetsWidth = iconWidth + right + left;

        return new Dimension(iconWidth + size.width, size.height);
    }

    private void paintIcons(Graphics g) {
        for (AdditionalIcon icon : myAdditionalIcons) {
            if (icon.getAvailable()) {
                icon.paintIcon(this, g);
            }
        }
    }

    @Override
    @RequiredUIAccess
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintIcons(g);
    }

    public boolean isSelected() {
        return contentManager().isSelected(myContent);
    }

    public boolean isHovered() {
        return behavior.isHovered();
    }

    private ContentManager contentManager() {
        return myUi.myWindow.getContentManager();
    }

    @Override
    public Content getContent() {
        return myContent;
    }

    private static class CurrentTooltip {
        final IdeTooltip currentTooltip;
        final AdditionalIcon icon;

        CurrentTooltip(IdeTooltip currentTooltip, AdditionalIcon icon) {
            this.currentTooltip = currentTooltip;
            this.icon = icon;
        }
    }
}
