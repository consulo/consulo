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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.ui.impl.DesktopSpace;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.RoundedLineBorder;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalLayout;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class DesktopCardImpl extends SwingComponentDelegate<JPanel> implements Card {
    private class MyCardPanel extends JPanel implements FromSwingComponentWrapper {
        MyCardPanel() {
            super(new BorderLayout());
        }

        @Override
        public Component toUIComponent() {
            return DesktopCardImpl.this;
        }
    }

    private static class MyTextLabel extends JBLabel {
        private LocalizeValue myValue = LocalizeValue.empty();

        void setValue(LocalizeValue value) {
            myValue = value;
            setText(value.get());
            setVisible(value != LocalizeValue.empty());
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (myValue != null) {
                setText(myValue.get());
            }
        }
    }

    private final Set<CardStyle> myStyles = EnumSet.noneOf(CardStyle.class);

    private final MyTextLabel myTitleLabel = new MyTextLabel();
    private final MyTextLabel mySubtitleLabel = new MyTextLabel();
    private final JPanel myTitlePanel = new JPanel(new VerticalLayout(0));
    private final JPanel myHeaderPanel = new JPanel(new BorderLayout());
    private final JPanel myContentPanel = new JPanel(new VerticalLayout(DesktopSpace.toPixels(Space.SMALL)));
    private final JPanel myFooterPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, DesktopSpace.toPixels(Space.SMALL), 0));
    private final JPanel myBodyPanel = new JPanel(new VerticalLayout(DesktopSpace.toPixels(Space.MEDIUM)));

    private @Nullable Component myMedia;
    private @Nullable Component myHeader;
    private @Nullable Component myHeaderPrefix;
    private @Nullable Component myHeaderSuffix;
    private final List<Component> myContent = new ArrayList<>();
    private final List<Component> myFooter = new ArrayList<>();

    public DesktopCardImpl() {
        myTitleLabel.setFont(myTitleLabel.getFont().deriveFont(Font.BOLD));
        mySubtitleLabel.setFontColor(UIUtil.FontColor.BRIGHTER);

        for (JPanel panel : List.of(myTitlePanel, myHeaderPanel, myContentPanel, myFooterPanel, myBodyPanel)) {
            panel.setOpaque(false);
        }

        myTitlePanel.add(myTitleLabel);
        myTitlePanel.add(mySubtitleLabel);
        myHeaderPanel.add(myTitlePanel, BorderLayout.CENTER);

        myTitleLabel.setValue(LocalizeValue.empty());
        mySubtitleLabel.setValue(LocalizeValue.empty());
    }

    @Override
    protected JPanel createComponent() {
        JPanel panel = new MyCardPanel();
        panel.setOpaque(false);
        rebuild(panel);
        return panel;
    }

    @Override
    public void addStyle(CardStyle style) {
        myStyles.add(style);
        updateStyle(style);
    }

    @Override
    public void removeStyle(CardStyle style) {
        if (myStyles.remove(style)) {
            updateStyle(style);
        }
    }

    private void updateStyle(CardStyle style) {
        JPanel panel = toAWTComponent();
        if (style == CardStyle.SELECTED) {
            if (panel != null) {
                updateBorder(panel);
                panel.repaint();
            }
            return;
        }
        rebuild(panel);
    }

    private void updateBorder(JPanel panel) {
        // the corners of a card are as round on every frontend - a radius of 10, the arc being its diameter
        panel.setBorder(myStyles.contains(CardStyle.SELECTED)
            ? new RoundedLineBorder(JBCurrentTheme.Focus.focusColor(), JBUI.scale(20))
            : IdeBorderFactory.createRoundedBorder(JBUI.scale(20)));
    }

    @RequiredUIAccess
    @Override
    public Card setMedia(@Nullable Component media) {
        myMedia = media;
        rebuild(toAWTComponent());
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setTitle(LocalizeValue title) {
        myTitleLabel.setValue(title);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setSubtitle(LocalizeValue subtitle) {
        mySubtitleLabel.setValue(subtitle);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeader(@Nullable Component header) {
        myHeader = header;
        rebuildHeader();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeaderPrefix(@Nullable Component prefix) {
        myHeaderPrefix = prefix;
        rebuildHeader();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeaderSuffix(@Nullable Component suffix) {
        myHeaderSuffix = suffix;
        rebuildHeader();
        return this;
    }

    @Override
    public Layout<LayoutConstraint> add(Component component, LayoutConstraint constraint) {
        myContent.add(component);
        myContentPanel.add(DesktopLayoutChildren.toAWT(component));
        rebuild(toAWTComponent());
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card addToFooter(Component component) {
        myFooter.add(component);
        myFooterPanel.add(DesktopLayoutChildren.toAWT(component));
        rebuild(toAWTComponent());
        return this;
    }

    @Override
    public void remove(Component component) {
        java.awt.Component awtComponent = TargetAWT.to(component);
        if (myContent.remove(component)) {
            myContentPanel.remove(awtComponent);
        }
        else if (myFooter.remove(component)) {
            myFooterPanel.remove(awtComponent);
        }
        rebuild(toAWTComponent());
    }

    @RequiredUIAccess
    @Override
    public void removeAll() {
        myContent.clear();
        myContentPanel.removeAll();
        rebuild(toAWTComponent());
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        DesktopLayoutChildren.acceptAll(consumer, myMedia, myHeaderPrefix, myHeader, myHeaderSuffix);
        myContent.forEach(consumer);
        myFooter.forEach(consumer);
    }

    private void rebuildHeader() {
        myHeaderPanel.removeAll();

        if (myHeaderPrefix != null) {
            JComponent prefix = wrap(DesktopLayoutChildren.toAWT(myHeaderPrefix));
            prefix.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, DesktopSpace.toPixels(Space.SMALL)));
            myHeaderPanel.add(prefix, BorderLayout.WEST);
        }

        myHeaderPanel.add(myHeader == null ? myTitlePanel : DesktopLayoutChildren.toAWT(myHeader), BorderLayout.CENTER);

        if (myHeaderSuffix != null) {
            myHeaderPanel.add(DesktopLayoutChildren.toAWT(myHeaderSuffix), BorderLayout.EAST);
        }

        rebuild(toAWTComponent());
    }

    private static JComponent wrap(java.awt.Component component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(component, BorderLayout.NORTH);
        return panel;
    }

    private void rebuild(@Nullable JPanel panel) {
        if (panel == null) {
            return;
        }

        panel.removeAll();
        myBodyPanel.removeAll();

        boolean horizontal = myStyles.contains(CardStyle.HORIZONTAL);
        boolean cover = myStyles.contains(CardStyle.COVER_MEDIA);

        myBodyPanel.add(myHeaderPanel);
        if (!myContent.isEmpty()) {
            myBodyPanel.add(myContentPanel);
        }
        if (!myFooter.isEmpty()) {
            myBodyPanel.add(myFooterPanel);
        }

        int padding = DesktopSpace.toPixels(myStyles.contains(CardStyle.COMPACT) ? Space.MEDIUM : Space.LARGE);
        myBodyPanel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));

        JComponent media = null;
        if (myMedia != null) {
            media = wrap(DesktopLayoutChildren.toAWT(myMedia));
            int mediaPadding = cover ? 0 : padding;
            media.setBorder(BorderFactory.createEmptyBorder(mediaPadding, mediaPadding, horizontal ? mediaPadding : 0, horizontal ? 0 : mediaPadding));
        }

        if (media != null) {
            panel.add(media, horizontal ? BorderLayout.WEST : BorderLayout.NORTH);
        }
        panel.add(myBodyPanel, BorderLayout.CENTER);

        updateBorder(panel);

        panel.revalidate();
        panel.repaint();
    }
}
