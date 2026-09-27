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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import consulo.ui.DelayedAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

/**
 * @author VISTALL
 * @since 2026-09-27
 */
@NullMarked
public final class WebDelayedActionImpl implements DelayedAction {
    private static final String AT_POINT = """
        const rect = target.getBoundingClientRect();
        const x = rect.left + $1;
        const y = rect.top + $2;
        """;

    private static final String AT_CENTER = """
        const rect = target.getBoundingClientRect();
        const x = rect.left + rect.width / 2;
        const y = rect.top + rect.height / 2;
        """;

    private static final String AT_CONTEXT_MENU_EVENT = """
        const connector = target.$contextMenuTargetConnector;
        const event = connector ? connector.openEvent : null;
        const source = event && event.detail instanceof Object ? event.detail : event;
        const known = !!source && Number.isFinite(source.x) && Number.isFinite(source.y) && (source.x !== 0 || source.y !== 0);
        const rect = target.getBoundingClientRect();
        const x = known ? source.x : rect.left + rect.width / 2;
        const y = known ? source.y : rect.top + rect.height / 2;
        """;

    private final Component mySpinner;

    private WebDelayedActionImpl(Component spinner) {
        mySpinner = spinner;
    }

    @RequiredUIAccess
    public static DelayedAction startAt(Component target, int relativeX, int relativeY) {
        return start(target, AT_POINT, relativeX, relativeY);
    }

    @RequiredUIAccess
    public static DelayedAction startAtCenter(Component target) {
        return start(target, AT_CENTER, 0, 0);
    }

    @RequiredUIAccess
    public static DelayedAction startAtContextMenuEvent(Component target) {
        return start(target, AT_CONTEXT_MENU_EVENT, 0, 0);
    }

    @RequiredUIAccess
    private static DelayedAction start(Component target, String placement, int relativeX, int relativeY) {
        Optional<UI> maybeUI = target.getUI();
        if (maybeUI.isEmpty()) {
            return () -> {
            };
        }
        UI ui = maybeUI.get();

        Image busy = Image.busy();

        Component spinner = WebImageConverter.getImage(busy);
        spinner.getElement().setAttribute("popover", "manual");
        spinner.getElement().getStyle()
            .set("position", "fixed")
            .set("right", "auto")
            .set("bottom", "auto")
            .set("margin", "0")
            .set("padding", "0")
            .set("border", "none")
            .set("background", "none")
            .set("overflow", "clip")
            .set("color", "inherit")
            .set("pointer-events", "none");

        if (ui.hasModalComponent()) {
            ui.addToModalComponent(spinner);
        }
        else {
            ui.add(spinner);
        }

        spinner.getElement().executeJs(
            """
                const target = $0;
                if (!target) {
                    return;
                }
                """
                + placement
                + """
                this.style.left = (x - $3) + 'px';
                this.style.top = (y - $4) + 'px';
                this.showPopover();
                """,
            target.getElement(),
            relativeX,
            relativeY,
            busy.getWidth() / 2,
            busy.getHeight() / 2
        );

        return new WebDelayedActionImpl(spinner);
    }

    @Override
    @RequiredUIAccess
    public void stop() {
        mySpinner.getElement().removeFromParent();
    }
}
