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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.dom.Element;

import java.util.Optional;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class WebGridSelectionColumn {
    private static final String TAG = "vaadin-grid-flow-selection-column";

    private WebGridSelectionColumn() {
    }

    public static void hide(Grid<?> grid) {
        if (!hideInserted(grid)) {
            grid.getElement().getNode().runWhenAttached(ui -> hideInserted(grid));
        }
    }

    private static boolean hideInserted(Grid<?> grid) {
        Optional<Element> column = grid.getElement().getChildren().filter(child -> TAG.equals(child.getTag())).findFirst();
        column.ifPresent(element -> element.setProperty("hidden", true));
        return column.isPresent();
    }
}
