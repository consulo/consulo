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

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public final class WebGridNavigation {
    public static final String TREE_KEYS = String.join("\n",
        "38", "40", "37", "39", "36", "35", "33", "34",
        "SHIFT+38", "SHIFT+40", "SHIFT+36", "SHIFT+35", "SHIFT+33", "SHIFT+34"
    );

    public static final String TABLE_KEYS = String.join("\n",
        "38", "40", "33", "34", "CTRL+36", "CTRL+35",
        "SHIFT+38", "SHIFT+40", "SHIFT+33", "SHIFT+34", "CTRL+SHIFT+36", "CTRL+SHIFT+35"
    );

    private static final String OWN_KEYS_ATTRIBUTE = "consulo-own-keys";
    private static final String EVENT = "consulo-grid-navigate";
    private static final String KEY_DATA = "event.detail.key";
    private static final String SHIFT_DATA = "event.detail.shift";
    private static final String PAGE_DATA = "event.detail.page";

    private static final String INSTALL = """
        const grid = this;
        grid.$consuloNavigationKeys = new Set($0.split('\\n'));
        if (grid.$consuloNavigation) {
            return;
        }
        grid.$consuloNavigation = true;

        const comboOf = event => {
            const parts = [];
            if (event.altKey) {
                parts.push('ALT');
            }
            if (event.ctrlKey) {
                parts.push('CTRL');
            }
            if (event.metaKey) {
                parts.push('META');
            }
            if (event.shiftKey) {
                parts.push('SHIFT');
            }
            parts.push(event.keyCode);
            return parts.join('+');
        };

        const isEditable = node => !!node && (node.tagName === 'TEXTAREA'
            || node.isContentEditable === true
            || (node.tagName === 'INPUT' && !['checkbox', 'radio', 'button'].includes(node.type)));

        grid.addEventListener('keydown', event => {
            if (event.isComposing || isEditable(event.composedPath()[0])) {
                return;
            }

            const speedSearch = grid.$consuloSpeedSearch;
            if (speedSearch && speedSearch.consumes(event)) {
                return;
            }

            if (!grid.$consuloNavigationKeys.has(comboOf(event))) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();

            const row = [...grid.$.items.children].find(child => !child.hidden && child.offsetHeight > 0);
            const rowHeight = row ? row.offsetHeight : 0;
            const viewport = grid.$.table.clientHeight - grid.$.header.offsetHeight - grid.$.footer.offsetHeight;
            const page = rowHeight > 0 ? Math.max(1, Math.floor(viewport / rowHeight) - 1) : 1;

            grid.dispatchEvent(new CustomEvent('consulo-grid-navigate', {
                detail: { key: event.key, shift: event.shiftKey, page: page }
            }));
        }, true);
        """;

    private static final String FOCUS_ROW = """
        const grid = this;
        const key = $0;
        if (!grid.matches(':focus-within')) {
            return;
        }

        const active = grid.shadowRoot.activeElement;
        const column = active && active.parentElement ? [...active.parentElement.children].indexOf(active) : -1;
        let tries = 0;
        const run = () => {
            const row = [...grid.$.items.children].find(child => child._item && child._item.key === key);
            if (!row) {
                if (tries++ < 60) {
                    requestAnimationFrame(run);
                }
                return;
            }

            const cell = row.children[column] || [...row.children].find(child => !child.hidden);
            if (cell && grid.matches(':focus-within')) {
                cell.focus({ preventScroll: true });
            }
        };
        requestAnimationFrame(run);
        """;

    private WebGridNavigation() {
    }

    public static void install(Grid<?> grid, String keys, WebGridKeyHandler handler) {
        Element element = grid.getElement();
        element.setAttribute(OWN_KEYS_ATTRIBUTE, keys);

        element.addEventListener(EVENT, event -> handler.handle(
                event.getEventData().path(KEY_DATA).asString(""),
                event.getEventData().path(SHIFT_DATA).asBoolean(false),
                Math.max(1, event.getEventData().path(PAGE_DATA).asInt(1))
            ))
            .addEventData(KEY_DATA)
            .addEventData(SHIFT_DATA)
            .addEventData(PAGE_DATA);

        element.addAttachListener(event -> element.executeJs(INSTALL, keys));
        element.executeJs(INSTALL, keys);
    }

    public static int targetIndex(String key, int current, int size, int page) {
        if (size <= 0) {
            return -1;
        }

        int last = size - 1;
        if (current < 0 || current > last) {
            return switch (key) {
                case "ArrowUp", "ArrowDown", "PageUp", "PageDown", "Home" -> 0;
                case "End" -> last;
                default -> -1;
            };
        }

        return switch (key) {
            case "ArrowUp" -> Math.max(0, current - 1);
            case "ArrowDown" -> Math.min(last, current + 1);
            case "PageUp" -> Math.max(0, current - page);
            case "PageDown" -> Math.min(last, current + page);
            case "Home" -> 0;
            case "End" -> last;
            default -> -1;
        };
    }

    public static <T> void focusRow(Grid<T> grid, T item) {
        grid.getElement().executeJs(FOCUS_ROW, grid.getDataCommunicator().getKeyMapper().key(item));
    }
}
