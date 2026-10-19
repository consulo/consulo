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
package consulo.desktop.awt.internal.diff.bridge;

import consulo.codeEditor.Editor;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.codeEditor.internal.bridge.EditorBridgeConnector;
import consulo.codeEditor.internal.bridge.EditorBridgeFoldConnector;
import consulo.codeEditor.internal.bridge.EditorScrollMap;
import consulo.desktop.awt.internal.diff.util.DiffContentPanel;
import consulo.desktop.awt.internal.diff.util.DiffDividerDrawUtil;
import consulo.desktop.awt.internal.diff.util.DiffSplitter;
import consulo.desktop.awt.internal.diff.util.SyncScrollSupport;
import consulo.desktop.awt.internal.diff.util.ThreeDiffSplitter;
import consulo.diff.impl.internal.util.DiffDrawUtil;
import consulo.diff.util.Side;
import consulo.diff.util.ThreeSide;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class DesktopEditorBridge implements EditorBridge {
    private final List<Editor> myEditors;
    private final List<DiffContentPanel> myPanels;
    private final JComponent myComponent;
    private final @Nullable DiffSplitter mySplitter;
    private final @Nullable ThreeDiffSplitter myThreeSplitter;
    private final SyncScrollSupport.Support mySyncScrollSupport;

    private EditorScrollMap myScrollMap;
    private boolean mySyncScrollEnabled = true;
    private boolean myCurved = true;
    private final List<List<EditorBridgeConnector>> myConnectors = new ArrayList<>();
    private final List<List<EditorBridgeFoldConnector>> myFoldConnectors = new ArrayList<>();

    @RequiredUIAccess
    public DesktopEditorBridge(List<? extends Editor> editors, Disposable parentDisposable) {
        myEditors = List.copyOf(editors);
        assert myEditors.size() == 2 || myEditors.size() == 3;

        int[] lineCounts = new int[myEditors.size()];
        for (int i = 0; i < lineCounts.length; i++) {
            lineCounts[i] = myEditors.get(i).getDocument().getLineCount();
            if (i > 0) {
                myConnectors.add(List.of());
                myFoldConnectors.add(List.of());
            }
        }
        myScrollMap = new EditorScrollMap(List.of(new int[myEditors.size()], lineCounts));

        myPanels = ContainerUtil.map(myEditors, editor -> new DiffContentPanel(editor.getComponent()));
        DiffContentPanel.syncTitleHeights(myPanels);

        VisibleAreaListener listener;
        if (myEditors.size() == 2) {
            DiffSplitter splitter = new DiffSplitter();
            splitter.setFirstComponent(myPanels.get(0));
            splitter.setSecondComponent(myPanels.get(1));
            splitter.setHonorComponentsMinimumSize(false);
            splitter.setPainter((g, divider) -> paintDivider(0, g, divider));

            SyncScrollSupport.TwosideSyncScrollSupport support = new SyncScrollSupport.TwosideSyncScrollSupport(myEditors, createScrollable(0, 1));

            mySplitter = splitter;
            myThreeSplitter = null;
            myComponent = splitter;
            mySyncScrollSupport = support;
            listener = event -> {
                support.visibleAreaChanged(event);
                repaintDividers();
            };
        }
        else {
            ThreeDiffSplitter splitter = new ThreeDiffSplitter(myPanels);
            splitter.setPainter((g, divider) -> paintDivider(0, g, divider), Side.LEFT);
            splitter.setPainter((g, divider) -> paintDivider(1, g, divider), Side.RIGHT);

            SyncScrollSupport.ThreesideSyncScrollSupport support =
                new SyncScrollSupport.ThreesideSyncScrollSupport(myEditors, createScrollable(0, 1), createScrollable(1, 2));

            mySplitter = null;
            myThreeSplitter = splitter;
            myComponent = splitter;
            mySyncScrollSupport = support;
            listener = event -> {
                support.visibleAreaChanged(event);
                repaintDividers();
            };
        }

        for (Editor editor : myEditors) {
            editor.getScrollingModel().addVisibleAreaListener(listener, parentDisposable);
        }
    }

    private SyncScrollSupport.SyncScrollable createScrollable(int left, int right) {
        return new SyncScrollSupport.SyncScrollable() {
            @RequiredUIAccess
            @Override
            public boolean isSyncScrollEnabled() {
                return mySyncScrollEnabled;
            }

            @RequiredUIAccess
            @Override
            public int transfer(Side baseSide, int line) {
                int from = baseSide.select(left, right);
                int to = baseSide.select(right, left);
                return myScrollMap.transfer(from, to, line);
            }
        };
    }

    private void repaintDividers() {
        if (mySplitter != null) {
            mySplitter.repaintDivider();
        }
        if (myThreeSplitter != null) {
            myThreeSplitter.repaintDividers();
        }
    }

    private void paintDivider(int gap, Graphics g, JComponent divider) {
        Editor editor1 = myEditors.get(gap);
        Editor editor2 = myEditors.get(gap + 1);

        Graphics2D gg = DiffDividerDrawUtil.getDividerGraphics(g, divider, editor1.getComponent());

        gg.setColor(TargetAWT.to(DiffDrawUtil.getDividerColor(editor1)));
        gg.fill(gg.getClipBounds());

        List<EditorBridgeConnector> connectors = myConnectors.get(gap);
        DiffDividerDrawUtil.paintPolygons(gg, divider.getWidth(), true, myCurved, editor1, editor2, handler -> {
            for (EditorBridgeConnector connector : connectors) {
                if (!handler.process(
                    connector.startLine1(),
                    connector.endLine1(),
                    connector.startLine2(),
                    connector.endLine2(),
                    connector.color(),
                    connector.resolved()
                )) {
                    return;
                }
            }
        });

        List<EditorBridgeFoldConnector> foldConnectors = myFoldConnectors.get(gap);
        DiffDividerDrawUtil.paintSeparators(gg, divider.getWidth(), editor1, editor2, handler -> {
            for (EditorBridgeFoldConnector connector : foldConnectors) {
                if (!handler.process(connector.line1(), connector.line2())) {
                    return;
                }
            }
        });

        gg.dispose();
    }

    @Override
    public Component getComponent() {
        return TargetAWT.wrap(myComponent);
    }

    @Override
    public List<Editor> getEditors() {
        return myEditors;
    }

    @RequiredUIAccess
    @Override
    public void setScrollMap(EditorScrollMap scrollMap) {
        myScrollMap = scrollMap;
    }

    @RequiredUIAccess
    @Override
    public void setSyncScrollEnabled(boolean enabled) {
        mySyncScrollEnabled = enabled;
    }

    @Override
    public boolean isSyncScrollEnabled() {
        return mySyncScrollEnabled;
    }

    @RequiredUIAccess
    @Override
    public void setConnectors(int gap, List<EditorBridgeConnector> connectors, List<EditorBridgeFoldConnector> foldConnectors) {
        myConnectors.set(gap, List.copyOf(connectors));
        myFoldConnectors.set(gap, List.copyOf(foldConnectors));
        repaintDividers();
    }

    @RequiredUIAccess
    @Override
    public void setCurvedConnectors(boolean curved) {
        myCurved = curved;
        repaintDividers();
    }

    @RequiredUIAccess
    @Override
    public void setTitles(List<? extends @Nullable Component> titles) {
        for (int i = 0; i < myPanels.size(); i++) {
            Component title = i < titles.size() ? titles.get(i) : null;
            myPanels.get(i).setTitle(title == null ? null : (JComponent) TargetAWT.to(title));
        }
        myComponent.revalidate();
        myComponent.repaint();
    }

    @RequiredUIAccess
    @Override
    public void makeVisible(int masterIndex, int[] startLines, int[] endLines, boolean animated) {
        if (mySyncScrollSupport instanceof SyncScrollSupport.TwosideSyncScrollSupport twoside) {
            twoside.makeVisible(Side.fromIndex(masterIndex), startLines[0], endLines[0], startLines[1], endLines[1], animated);
        }
        else if (mySyncScrollSupport instanceof SyncScrollSupport.ThreesideSyncScrollSupport threeside) {
            threeside.makeVisible(ThreeSide.fromIndex(masterIndex), startLines, endLines, animated);
        }
    }

    @RequiredUIAccess
    @Override
    public void runWithoutSyncScroll(Runnable action) {
        mySyncScrollSupport.enterDisableScrollSection();
        try {
            action.run();
        }
        finally {
            mySyncScrollSupport.exitDisableScrollSection();
        }
    }
}
