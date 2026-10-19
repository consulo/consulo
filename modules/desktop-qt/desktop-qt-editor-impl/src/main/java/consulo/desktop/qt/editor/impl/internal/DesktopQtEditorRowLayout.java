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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.ComponentInlayRenderer;
import consulo.codeEditor.Inlay;
import consulo.document.Document;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Where the rows of the editor stand. A row is one line tall, and a component inlay adds the room of its component
 * above or below the row it belongs to - every y the editor maps to a row, or a row to, goes through here.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class DesktopQtEditorRowLayout {
    private final DesktopQtEditorImpl myEditor;

    private @Nullable List<DesktopQtEditorInlayRow> myRows;
    private final Map<Inlay<?>, Integer> myInlayOffsets = new HashMap<>();
    private final Map<Integer, List<Inlay<?>>> myRowInlays = new HashMap<>();

    public DesktopQtEditorRowLayout(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    public void invalidate() {
        myRows = null;
    }

    private List<DesktopQtEditorInlayRow> rows() {
        List<DesktopQtEditorInlayRow> rows = myRows;
        if (rows == null) {
            rows = compute();
            myRows = rows;
        }
        return rows;
    }

    private List<DesktopQtEditorInlayRow> compute() {
        myInlayOffsets.clear();
        myRowInlays.clear();

        Document document = myEditor.getDocument();
        DesktopQtEditorVisualLines visualLines = myEditor.getVisualLines();

        TreeMap<Integer, int[]> rooms = new TreeMap<>();
        for (Inlay<?> inlay : myEditor.getInlayModel().getBlockElementsInRange(0, document.getTextLength())) {
            if (!(inlay.getRenderer() instanceof ComponentInlayRenderer) || !inlay.isValid()) {
                continue;
            }
            int height = inlay.getHeightInPixels();
            if (height <= 0 || myEditor.getFoldingModel().isOffsetCollapsed(inlay.getOffset())) {
                continue;
            }

            int visualLine = visualLines.offsetToVisualLine(inlay.getOffset());
            int[] room = rooms.computeIfAbsent(visualLine, line -> new int[2]);
            myRowInlays.computeIfAbsent(visualLine, line -> new ArrayList<>()).add(inlay);
            if (inlay.getPlacement() == Inlay.Placement.ABOVE_LINE) {
                myInlayOffsets.put(inlay, -room[0] - height);
                room[0] += height;
            }
            else {
                myInlayOffsets.put(inlay, room[1]);
                room[1] += height;
            }
        }

        List<DesktopQtEditorInlayRow> rows = new ArrayList<>(rooms.size());
        for (Map.Entry<Integer, int[]> entry : rooms.entrySet()) {
            rows.add(new DesktopQtEditorInlayRow(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }
        return rows;
    }

    /**
     * The top of the text of a row.
     */
    public int visualLineToY(int visualLine) {
        int lineHeight = myEditor.getLineHeight();
        int y = visualLine * lineHeight;
        for (DesktopQtEditorInlayRow row : rows()) {
            if (row.visualLine() < visualLine) {
                y += row.above() + row.below();
            }
            else if (row.visualLine() == visualLine) {
                y += row.above();
            }
            else {
                break;
            }
        }
        return y;
    }

    /**
     * The top of the room of a row, the inlays above its text included.
     */
    public int visualLineAreaStartY(int visualLine) {
        return visualLineToY(visualLine) - room(visualLine)[0];
    }

    /**
     * The bottom of the room of a row, the inlays below its text included.
     */
    public int visualLineAreaEndY(int visualLine) {
        return visualLineToY(visualLine) + myEditor.getLineHeight() + room(visualLine)[1];
    }

    private int[] room(int visualLine) {
        for (DesktopQtEditorInlayRow row : rows()) {
            if (row.visualLine() == visualLine) {
                return new int[]{row.above(), row.below()};
            }
            if (row.visualLine() > visualLine) {
                break;
            }
        }
        return new int[]{0, 0};
    }

    /**
     * The row a y stands in - the room of an inlay belongs to the row the inlay belongs to. Not clamped to the rows
     * there are.
     */
    public int yToVisualLine(int y) {
        int lineHeight = Math.max(1, myEditor.getLineHeight());
        int consumed = 0;
        int line = 0;
        for (DesktopQtEditorInlayRow row : rows()) {
            int plain = (row.visualLine() - line) * lineHeight;
            if (y < consumed + plain) {
                return line + Math.max(0, y - consumed) / lineHeight;
            }
            consumed += plain;
            line = row.visualLine();

            int roomed = row.above() + lineHeight + row.below();
            if (y < consumed + roomed) {
                return line;
            }
            consumed += roomed;
            line++;
        }
        return line + Math.max(0, y - consumed) / lineHeight;
    }

    /**
     * The height of every row and every inlay room together.
     */
    public int getContentHeight() {
        int height = myEditor.getVisualLines().getVisualLineCount() * myEditor.getLineHeight();
        for (DesktopQtEditorInlayRow row : rows()) {
            height += row.above() + row.below();
        }
        return height;
    }

    public List<Inlay<?>> getInlays(int visualLine) {
        rows();
        return myRowInlays.getOrDefault(visualLine, List.of());
    }

    /**
     * The top of the room of a component inlay, or null while it takes none - folded away or not measured yet.
     */
    public @Nullable Integer inlayY(Inlay<?> inlay) {
        rows();
        Integer offset = myInlayOffsets.get(inlay);
        if (offset == null) {
            return null;
        }

        int visualLine = myEditor.getVisualLines().offsetToVisualLine(inlay.getOffset());
        int top = visualLineToY(visualLine);
        return offset < 0 ? top + offset : top + myEditor.getLineHeight() + offset;
    }
}
