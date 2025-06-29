/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.event.JBLoadingPanelListener;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.util.collection.Lists;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author Konstantin Bulenkov
 */
public class JBLoadingPanel extends JPanel {
    private final JPanel myPanel;
    final LoadingDecorator myDecorator;
    private final Collection<JBLoadingPanelListener> myListeners = Lists.newLockFreeCopyOnWriteList();

    public JBLoadingPanel(@Nullable LayoutManager manager, @Nonnull Disposable parent) {
        this(manager, parent, -1);
    }

    public JBLoadingPanel(@Nullable LayoutManager manager, @Nonnull Disposable parent, int startDelayMs) {
        this(
            manager,
            panel -> new LoadingDecorator(panel, parent, startDelayMs) {
                @Override
                protected NonOpaquePanel customizeLoadingLayer(JPanel parent, JLabel text, AsyncProcessIcon icon) {
                    NonOpaquePanel panel = super.customizeLoadingLayer(parent, text, icon);
                    customizeStatusText(text);
                    return panel;
                }
            }
        );
    }

    public JBLoadingPanel(
        @Nullable LayoutManager manager,
        @Nonnull Function<? super JPanel, ? extends LoadingDecorator> createLoadingDecorator
    ) {
        super(new BorderLayout());
        myPanel = manager == null ? new JPanel() : new JPanel(manager);
        myPanel.setOpaque(false);
        myPanel.setFocusable(false);
        myDecorator = createLoadingDecorator.apply(myPanel);
        super.add(myDecorator.getComponent(), BorderLayout.CENTER);
    }

    @Override
    public void setLayout(LayoutManager mgr) {
        if (!(mgr instanceof BorderLayout)) {
            throw new IllegalArgumentException(String.valueOf(mgr));
        }
        super.setLayout(mgr);
        if (myDecorator != null) {
            super.add(myDecorator.getComponent(), BorderLayout.CENTER);
        }
    }

    public static void customizeStatusText(JLabel text) {
        Font font = text.getFont();
        text.setFont(font.deriveFont(font.getStyle(), font.getSize() + 6));
        text.setForeground(ColorUtil.toAlpha(UIUtil.getLabelForeground(), 150));
    }

    public void setLoadingText(LocalizeValue text) {
        myDecorator.setLoadingText(text);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public void setLoadingText(String text) {
        myDecorator.setLoadingText(text);
    }

    public void stopLoading() {
        myDecorator.stopLoading();
        for (JBLoadingPanelListener listener : myListeners) {
            listener.onLoadingFinish();
        }
    }

    public boolean isLoading() {
        return myDecorator.isLoading();
    }

    public void startLoading() {
        myDecorator.startLoading(false);
        for (JBLoadingPanelListener listener : myListeners) {
            listener.onLoadingStart();
        }
    }

    public void addListener(@Nonnull JBLoadingPanelListener listener) {
        myListeners.add(listener);
    }

    public boolean removeListener(@Nonnull JBLoadingPanelListener listener) {
        return myListeners.remove(listener);
    }

    public JPanel getContentPanel() {
        return myPanel;
    }

    @Override
    public Component add(Component comp) {
        return myPanel.add(comp);
    }

    @Override
    public Component add(Component comp, int index) {
        return myPanel.add(comp, index);
    }

    @Override
    public void add(Component comp, Object constraints) {
        myPanel.add(comp, constraints);
    }

    @Override
    public Dimension getPreferredSize() {
        return getContentPanel().getPreferredSize();
    }
}
