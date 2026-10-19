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
package consulo.codeEditor.internal.bridge;

import consulo.codeEditor.Editor;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A connection between editors standing side by side: the editors laid out with a strip in each gap between two
 * neighbours, the strip drawing which lines of one correspond to which lines of the other, and the editors scrolled
 * together along a {@link EditorScrollMap}. Every frontend draws the strip and links the scrolling itself.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface EditorBridge {
    Component getComponent();

    List<Editor> getEditors();

    @RequiredUIAccess
    void setScrollMap(EditorScrollMap scrollMap);

    @RequiredUIAccess
    void setSyncScrollEnabled(boolean enabled);

    boolean isSyncScrollEnabled();

    /**
     * @param gap the gap between editor {@code gap} and editor {@code gap + 1}
     */
    @RequiredUIAccess
    void setConnectors(int gap, List<EditorBridgeConnector> connectors, List<EditorBridgeFoldConnector> foldConnectors);

    @RequiredUIAccess
    void setCurvedConnectors(boolean curved);

    /**
     * A title above each editor, in the order of {@link #getEditors()}. The titles of all editors take the height of
     * the highest one, so the editors start at the same height.
     */
    @RequiredUIAccess
    void setTitles(List<? extends @Nullable Component> titles);

    /**
     * Scrolls every editor so its range of lines is in sight, the ranges standing level with each other where the
     * editors allow it.
     *
     * @param masterIndex the editor the others follow
     */
    @RequiredUIAccess
    void makeVisible(int masterIndex, int[] startLines, int[] endLines, boolean animated);

    /**
     * Runs the action with the scrolling of the editors unlinked, so a change of one editor's layout does not move
     * the others.
     */
    @RequiredUIAccess
    void runWithoutSyncScroll(Runnable action);
}
