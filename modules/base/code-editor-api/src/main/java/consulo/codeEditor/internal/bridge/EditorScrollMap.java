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

import java.util.List;

/**
 * How the lines of bridged editors correspond: ordered rows of anchor lines, one line per editor. The first row is
 * all zeros and the last one holds the line count of every editor; between two rows lines transfer one to one, up
 * to the end of the shorter run.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class EditorScrollMap {
    private final List<int[]> myAnchors;

    public EditorScrollMap(List<int[]> anchors) {
        myAnchors = List.copyOf(anchors);
    }

    public List<int[]> getAnchors() {
        return myAnchors;
    }

    public int transfer(int fromEditor, int toEditor, int line) {
        int from1 = 0;
        int to1 = 0;
        int from2 = 0;
        int to2 = 0;
        for (int[] anchor : myAnchors) {
            from1 = from2;
            to1 = to2;
            from2 = anchor[fromEditor];
            to2 = anchor[toEditor];
            if (line <= from2) {
                break;
            }
        }

        if (from1 == line) {
            return to1;
        }
        if (from2 == line) {
            return to2;
        }
        if (from2 < line) {
            return line - from2 + to2;
        }
        return Math.min(to1 + (line - from1), to2);
    }
}
