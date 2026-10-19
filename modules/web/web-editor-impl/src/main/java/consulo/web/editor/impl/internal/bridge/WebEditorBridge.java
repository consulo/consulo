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
package consulo.web.editor.impl.internal.bridge;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.codeEditor.internal.bridge.EditorBridgeConnector;
import consulo.codeEditor.internal.bridge.EditorBridgeFoldConnector;
import consulo.codeEditor.internal.bridge.EditorScrollMap;
import consulo.colorScheme.EditorColorsScheme;
import consulo.diff.DiffColors;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.web.ui.impl.internal.WebColors;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class WebEditorBridge implements EditorBridge {
    private final List<Editor> myEditors;
    private final WebEditorBridgeComponent myComponent;

    private EditorScrollMap myScrollMap;
    private boolean mySyncScrollEnabled = true;
    private boolean myCurved = true;
    private final List<List<EditorBridgeConnector>> myConnectors = new ArrayList<>();
    private final List<List<EditorBridgeFoldConnector>> myFoldConnectors = new ArrayList<>();

    @RequiredUIAccess
    public WebEditorBridge(List<? extends Editor> editors) {
        myEditors = List.copyOf(editors);

        int[] lineCounts = new int[myEditors.size()];
        List<Component> components = new ArrayList<>();
        for (int i = 0; i < lineCounts.length; i++) {
            lineCounts[i] = myEditors.get(i).getDocument().getLineCount();
            components.add(myEditors.get(i).getUIComponent());
            if (i > 0) {
                myConnectors.add(List.of());
                myFoldConnectors.add(List.of());
            }
        }
        myScrollMap = new EditorScrollMap(List.of(new int[myEditors.size()], lineCounts));
        myComponent = new WebEditorBridgeComponent(components);
        push();
    }

    private void push() {
        for (int gap = 0; gap < myEditors.size() - 1; gap++) {
            push(gap);
        }
    }

    private void push(int gap) {
        EditorColorsScheme scheme = myEditors.get(gap).getColorsScheme();

        StringBuilder data = new StringBuilder("{");
        data.append("\"sync\":").append(mySyncScrollEnabled);
        data.append(",\"curved\":").append(myCurved);
        data.append(",\"background\":").append(json(dividerBackground(scheme)));
        data.append(",\"separatorBackground\":").append(json(scheme.getColor(DiffColors.DIFF_SEPARATORS_BACKGROUND)));
        data.append(",\"separatorTop\":").append(json(scheme.getColor(DiffColors.DIFF_SEPARATORS_TOP_BORDER)));
        data.append(",\"separatorBottom\":").append(json(scheme.getColor(DiffColors.DIFF_SEPARATORS_BOTTOM_BORDER)));

        data.append(",\"anchors\":[");
        boolean first = true;
        for (int[] anchor : myScrollMap.getAnchors()) {
            if (!first) {
                data.append(',');
            }
            first = false;
            data.append('[').append(anchor[gap]).append(',').append(anchor[gap + 1]).append(']');
        }
        data.append(']');

        data.append(",\"connectors\":[");
        first = true;
        for (EditorBridgeConnector connector : myConnectors.get(gap)) {
            if (!first) {
                data.append(',');
            }
            first = false;
            data.append('[').append(connector.startLine1()).append(',').append(connector.endLine1())
                .append(',').append(connector.startLine2()).append(',').append(connector.endLine2())
                .append(',').append(json(connector.color()))
                .append(',').append(connector.resolved()).append(']');
        }
        data.append(']');

        data.append(",\"folds\":[");
        first = true;
        for (EditorBridgeFoldConnector connector : myFoldConnectors.get(gap)) {
            if (!first) {
                data.append(',');
            }
            first = false;
            data.append('[').append(connector.line1()).append(',').append(connector.line2()).append(']');
        }
        data.append("]}");

        myComponent.setData(gap, data.toString());
    }

    private static ColorValue dividerBackground(EditorColorsScheme scheme) {
        ColorValue color = scheme.getColor(EditorColors.GUTTER_BACKGROUND);
        return color != null ? color : EditorColors.GUTTER_BACKGROUND.getDefaultColorValue();
    }

    private static String json(@Nullable ColorValue color) {
        String css = WebColors.toCssColor(color);
        return css == null ? "null" : "\"" + css + "\"";
    }

    @Override
    public Component getComponent() {
        return myComponent;
    }

    @Override
    public List<Editor> getEditors() {
        return myEditors;
    }

    @RequiredUIAccess
    @Override
    public void setScrollMap(EditorScrollMap scrollMap) {
        myScrollMap = scrollMap;
        push();
    }

    @RequiredUIAccess
    @Override
    public void setSyncScrollEnabled(boolean enabled) {
        mySyncScrollEnabled = enabled;
        push();
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
        push(gap);
    }

    @RequiredUIAccess
    @Override
    public void setCurvedConnectors(boolean curved) {
        myCurved = curved;
        push();
    }

    @RequiredUIAccess
    @Override
    public void setTitles(List<? extends @Nullable Component> titles) {
        myComponent.setTitles(titles);
    }

    @RequiredUIAccess
    @Override
    public void makeVisible(int masterIndex, int[] startLines, int[] endLines, boolean animated) {
        myComponent.makeVisible(masterIndex, startLines, endLines);
    }

    @RequiredUIAccess
    @Override
    public void runWithoutSyncScroll(Runnable action) {
        myComponent.suspendSyncScroll();
        action.run();
    }
}
