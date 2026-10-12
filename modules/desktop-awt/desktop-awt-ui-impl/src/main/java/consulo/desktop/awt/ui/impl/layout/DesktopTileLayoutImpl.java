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

import consulo.desktop.awt.ui.impl.DesktopLength;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.Length;
import consulo.ui.Space;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.TileLayout;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class DesktopTileLayoutImpl extends DesktopLayoutBase<JPanel, LayoutConstraint> implements TileLayout {
    private static final int VISIBLE_ROW_COUNT = 2;

    private class MyTilePanel extends JPanel implements Scrollable, FromSwingComponentWrapper {
        private final DesktopTileLayoutManager myLayoutManager;

        MyTilePanel(DesktopTileLayoutManager layoutManager) {
            super(layoutManager);
            myLayoutManager = layoutManager;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return myLayoutManager.preferredViewportSize(this, VISIBLE_ROW_COUNT);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return DesktopLength.lineHeight(this);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }

        @Override
        public Component toUIComponent() {
            return DesktopTileLayoutImpl.this;
        }
    }

    private final Length myMinTileWidth;
    private final Space myGap;

    public DesktopTileLayoutImpl(Length minTileWidth, Space gap) {
        myMinTileWidth = minTileWidth;
        myGap = gap;
    }

    @Override
    protected JPanel createComponent() {
        return new MyTilePanel(new DesktopTileLayoutManager(myMinTileWidth, myGap));
    }

    @Override
    public void remove(Component component) {
        JPanel panel = toAWTComponent();
        java.awt.Component awtComponent = TargetAWT.to(component);
        if (awtComponent.getParent() == panel) {
            panel.remove(awtComponent);
            panel.revalidate();
            panel.repaint();
        }
    }
}
