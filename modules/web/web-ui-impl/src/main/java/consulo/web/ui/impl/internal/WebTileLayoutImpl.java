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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Length;
import consulo.ui.Space;
import consulo.ui.TransferHandler;
import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.TileLayout;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;
import consulo.web.ui.impl.internal.vaadin.WebLength;
import consulo.web.ui.impl.internal.vaadin.WebSpace;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class WebTileLayoutImpl extends WebLayoutImpl<WebTileLayoutImpl.Vaadin, LayoutConstraint> implements TileLayout {
    private @Nullable TransferHandler<Component> myTransferHandler;
    private final Set<Component> myDragAndDropChildren = Collections.newSetFromMap(new WeakHashMap<>());

    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebTileLayoutImpl.this;
        }
    }

    public WebTileLayoutImpl(Length minTileWidth, Space gap) {
        VaadinSizeUtil.setWidthFull(this);

        Vaadin vaadin = toVaadinComponent();
        vaadin.addClassName("web-tile-layout");
        if (gap != Space.NONE) {
            vaadin.addClassName(WebSpace.toGapClass(gap));
        }
        vaadin.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(min(" + WebLength.toCss(minTileWidth) + ", 100%), 1fr))");
    }

    @Override
    public Layout<LayoutConstraint> add(Component component, LayoutConstraint constraint) {
        toVaadinComponent().add(TargetVaadin.to(component));
        if (getDragAndDropHandler() != null) {
            installChildDragAndDrop(component);
        }
        return this;
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<Component> handler) {
        myTransferHandler = handler;
        if (getDragAndDropHandler() != null) {
            forEachChild(this::installChildDragAndDrop);
        }
    }

    @Override
    public @Nullable TransferHandler<Component> getTransferHandler() {
        return myTransferHandler;
    }

    private @Nullable DragAndDropTransferHandler<Component> getDragAndDropHandler() {
        return myTransferHandler instanceof DragAndDropTransferHandler<Component> handler ? handler : null;
    }

    private void installChildDragAndDrop(Component child) {
        if (!myDragAndDropChildren.add(child)) {
            return;
        }

        Element element = TargetVaadin.to(child).getElement();
        element.setAttribute("draggable", "true");

        String before = "event.clientX < element.getBoundingClientRect().left + element.getBoundingClientRect().width / 2";

        element.addEventListener("dragstart", event -> onDragStart(child))
            .addEventData("event.dataTransfer.setData('" + WebDragSession.DRAG_TYPE + "', '')")
            .addEventData("event.dataTransfer.effectAllowed = 'copyMove'");

        element.addEventListener("dragend", event -> toVaadinComponent().getUI().ifPresent(ui -> WebDragSession.end(ui, child)));

        element.addEventListener("drop", event -> performDrop(child, event.getEventData().path(before).asBoolean(false)))
            .addEventData(before)
            .addEventData("event.preventDefault()")
            .addEventData("event.stopPropagation()");

        String script = "if (this.__consuloTileDrop) { return; } this.__consuloTileDrop = true;"
            + "const clear = () => { this.removeAttribute('consulo-drop-before'); this.removeAttribute('consulo-drop-after'); };"
            + "this.addEventListener('dragover', e => {"
            + "  if (!e.dataTransfer.types.includes('" + WebDragSession.DRAG_TYPE + "')) { return; }"
            + "  e.preventDefault(); e.stopPropagation();"
            + "  const element = this; const event = e; const before = " + before + ";"
            + "  this.toggleAttribute('consulo-drop-before', before); this.toggleAttribute('consulo-drop-after', !before);"
            + "});"
            + "this.addEventListener('dragleave', clear); this.addEventListener('drop', clear); this.addEventListener('dragend', clear);";

        element.executeJs(script);
        element.addAttachListener(event -> element.executeJs(script));
    }

    private void onDragStart(Component child) {
        DragAndDropTransferHandler<Component> handler = getDragAndDropHandler();
        UI ui = toVaadinComponent().getUI().orElse(null);
        if (handler == null || ui == null) {
            return;
        }

        WebDragSession.start(ui, child, handler.createDragTransfer(this, List.of(child), true));
    }

    private void performDrop(Component target, boolean before) {
        DragAndDropTransferHandler<Component> handler = getDragAndDropHandler();
        UI ui = toVaadinComponent().getUI().orElse(null);
        Object source = ui == null ? null : WebDragSession.getSource(ui);
        if (handler == null || source == null) {
            return;
        }

        List<Component> items = source instanceof Component child && myDragAndDropChildren.contains(child) ? List.of(child) : List.of();
        DragAndDropTransferHandler.DropPosition position = before
            ? DragAndDropTransferHandler.DropPosition.ABOVE
            : DragAndDropTransferHandler.DropPosition.BELOW;

        WebDropContext<Component> context = new WebDropContext<>(target, position, WebDragSession.getTransfer(ui), items, true);
        if (handler.drop(this, context)) {
            handler.drop(this, context.toPerforming());
        }
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}
