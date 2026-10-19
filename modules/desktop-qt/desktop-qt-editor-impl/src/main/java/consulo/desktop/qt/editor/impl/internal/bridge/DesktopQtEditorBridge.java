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
package consulo.desktop.qt.editor.impl.internal.bridge;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.codeEditor.internal.bridge.EditorBridgeConnector;
import consulo.codeEditor.internal.bridge.EditorBridgeFoldConnector;
import consulo.codeEditor.internal.bridge.EditorBridgeScrollUtil;
import consulo.codeEditor.internal.bridge.EditorScrollMap;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.WrappedLayout;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class DesktopQtEditorBridge implements EditorBridge {
    private final List<Editor> myEditors;
    private final DesktopQtEditorBridgeLayout myLayout;
    private final List<WrappedLayout> myTitles;

    private EditorScrollMap myScrollMap;
    private boolean mySyncScrollEnabled = true;
    private boolean myCurved = true;
    private final List<List<EditorBridgeConnector>> myConnectors = new ArrayList<>();
    private final List<List<EditorBridgeFoldConnector>> myFoldConnectors = new ArrayList<>();

    private boolean myDuringSyncScroll;
    private int myDisableSyncScrollDepth;

    @RequiredUIAccess
    public DesktopQtEditorBridge(List<? extends Editor> editors, Disposable parentDisposable) {
        myEditors = List.copyOf(editors);

        int[] lineCounts = new int[myEditors.size()];
        List<WrappedLayout> titles = new ArrayList<>();
        for (int i = 0; i < lineCounts.length; i++) {
            lineCounts[i] = myEditors.get(i).getDocument().getLineCount();
            titles.add(WrappedLayout.create());
            if (i > 0) {
                myConnectors.add(List.of());
                myFoldConnectors.add(List.of());
            }
        }
        myScrollMap = new EditorScrollMap(List.of(new int[myEditors.size()], lineCounts));
        myTitles = List.copyOf(titles);

        List<Component> panes = new ArrayList<>();
        for (int i = 0; i < myEditors.size(); i++) {
            panes.add(createPane(i));
        }
        myLayout = new DesktopQtEditorBridgeLayout(this);
        myLayout.setComponents(panes);

        for (int index = 0; index < myEditors.size(); index++) {
            int master = index;
            myEditors.get(index).getScrollingModel().addVisibleAreaListener(event -> visibleAreaChanged(master, event), parentDisposable);
        }
    }

    @RequiredUIAccess
    private Component createPane(int index) {
        DockLayout pane = DockLayout.create(Space.NONE);
        pane.top(myTitles.get(index));
        pane.center(myEditors.get(index).getUIComponent());
        return pane;
    }

    private void visibleAreaChanged(int master, VisibleAreaEvent event) {
        Rectangle oldRectangle = event.getOldRectangle();
        Rectangle newRectangle = event.getNewRectangle();
        if (mySyncScrollEnabled && !myDuringSyncScroll && myDisableSyncScrollDepth == 0 && oldRectangle != null) {
            myDuringSyncScroll = true;
            try {
                for (int slave = 0; slave < myEditors.size(); slave++) {
                    if (slave == master) {
                        continue;
                    }
                    if (newRectangle.y != oldRectangle.y) {
                        syncVerticalScroll(myEditors.get(master), myEditors.get(slave), master, slave);
                    }
                    if (newRectangle.x != oldRectangle.x) {
                        syncHorizontalScroll(myEditors.get(master), myEditors.get(slave));
                    }
                }
            }
            finally {
                myDuringSyncScroll = false;
            }
        }
        myLayout.repaintDivider();
    }

    private void syncVerticalScroll(Editor master, Editor slave, int masterIndex, int slaveIndex) {
        if (master.getDocument().getTextLength() == 0) {
            return;
        }

        Rectangle viewRect = master.getScrollingModel().getVisibleArea();
        int middleY = viewRect.height / 3;

        LogicalPosition masterPos = master.xyToLogicalPosition(new Point(viewRect.x, viewRect.y + middleY));
        int masterCenterLine = masterPos.line;
        int convertedCenterLine = myScrollMap.transfer(masterIndex, slaveIndex, masterCenterLine);

        Point point = slave.logicalPositionToXY(new LogicalPosition(convertedCenterLine, masterPos.column));
        int correction = (viewRect.y + middleY) % master.getLineHeight();
        int offset = point.y - middleY + correction;

        ScrollingModel model = slave.getScrollingModel();
        model.disableAnimation();
        model.scrollVertically(offset);
        model.enableAnimation();
    }

    private static void syncHorizontalScroll(Editor master, Editor slave) {
        int offset = master.getScrollingModel().getVisibleArea().x;

        ScrollingModel model = slave.getScrollingModel();
        model.disableAnimation();
        model.scrollHorizontally(offset);
        model.enableAnimation();
    }

    List<EditorBridgeConnector> getConnectors(int gap) {
        return myConnectors.get(gap);
    }

    List<EditorBridgeFoldConnector> getFoldConnectors(int gap) {
        return myFoldConnectors.get(gap);
    }

    boolean isCurved() {
        return myCurved;
    }

    @Override
    public Component getComponent() {
        return myLayout;
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
        myLayout.repaintDivider();
    }

    @RequiredUIAccess
    @Override
    public void setCurvedConnectors(boolean curved) {
        myCurved = curved;
        myLayout.repaintDivider();
    }

    @RequiredUIAccess
    @Override
    public void setTitles(List<? extends @Nullable Component> titles) {
        for (int i = 0; i < myTitles.size(); i++) {
            myTitles.get(i).set(i < titles.size() ? titles.get(i) : null);
        }
    }

    @RequiredUIAccess
    @Override
    public void makeVisible(int masterIndex, int[] startLines, int[] endLines, boolean animated) {
        int[] offsets = EditorBridgeScrollUtil.getTargetOffsets(
            myEditors,
            startLines,
            endLines,
            -1,
            EditorBridgeScrollUtil::getDocumentMaximumOffset
        );

        runWithoutSyncScroll(() -> {
            EditorBridgeScrollUtil.scrollHorizontally(myEditors.get(masterIndex), 0, false);
            for (int i = 0; i < myEditors.size(); i++) {
                EditorBridgeScrollUtil.scrollVertically(myEditors.get(i), offsets[i], false);
            }
        });
        myLayout.repaintDivider();
    }

    @RequiredUIAccess
    @Override
    public void runWithoutSyncScroll(Runnable action) {
        myDisableSyncScrollDepth++;
        try {
            action.run();
        }
        finally {
            myDisableSyncScrollDepth--;
        }
    }
}
