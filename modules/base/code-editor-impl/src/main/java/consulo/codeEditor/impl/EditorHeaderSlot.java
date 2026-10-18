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
package consulo.codeEditor.impl;

import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

/**
 * The header area of an editor built from layouts, showing one header at a time.
 * <p>
 * A header stays in the cell it was put in until it is dropped. A header which is no longer shown is dropped, except for
 * the permanent header, whose cell is only hidden: attaching a component again may build it anew and lose its state, and
 * the permanent header comes back after every other header.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class EditorHeaderSlot {
    private final DockLayout myLayout;
    private final DockLayout[] myCells;
    private final @Nullable Component[] myHeaders = new Component[2];

    @RequiredUIAccess
    public EditorHeaderSlot() {
        myCells = new DockLayout[]{DockLayout.create(Space.NONE), DockLayout.create(Space.NONE)};

        myLayout = DockLayout.create(Space.NONE);
        myLayout.top(myCells[0]);
        myLayout.bottom(myCells[1]);
        myLayout.setVisible(false);
    }

    public Component getComponent() {
        return myLayout;
    }

    /**
     * @param header          the header to show, or {@code null} for none
     * @param permanentHeader the permanent header of the editor, kept while another header is shown
     */
    @RequiredUIAccess
    public void show(@Nullable Component header, @Nullable Component permanentHeader) {
        int shownCell = -1;
        for (int i = 0; i < myHeaders.length; i++) {
            Component cellHeader = myHeaders[i];
            if (cellHeader == null) {
                continue;
            }

            if (cellHeader == header) {
                shownCell = i;
            }
            else if (cellHeader != permanentHeader) {
                myCells[i].remove(cellHeader);
                myHeaders[i] = null;
            }
        }

        // at most the permanent header is left besides the shown one, so a cell is free
        if (header != null && shownCell < 0) {
            shownCell = myHeaders[0] == null ? 0 : 1;
            myCells[shownCell].top(header);
            myHeaders[shownCell] = header;
        }

        for (int i = 0; i < myCells.length; i++) {
            myCells[i].setVisible(i == shownCell);
        }
        myLayout.setVisible(shownCell >= 0);
    }
}
