/*
 * Copyright 2013-2020 consulo.io
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

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.Element;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.ScrollableLayoutInternal;
import consulo.ui.internal.ScrollableLayoutViewportEvent;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.layout.VerticalScrollBarPosition;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2020-05-10
 */
public class WebScrollLayoutImpl extends VaadinComponentDelegate<WebScrollLayoutImpl.Vaadin> implements ScrollableLayoutInternal {
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebScrollLayoutImpl.this;
        }
    }

    private static final String TRACK_ATTRIBUTE = "data-consulo-viewport-id";
    private static final int VIEWPORT_THROTTLE_MS = 50;

    private static final AtomicInteger ourTrackId = new AtomicInteger();

    private static final String INSTALL_SCRIPT =
        "const scroller = this;" +
            "if (!scroller.__consuloViewport) {" +
            "  let pending = false;" +
            "  const resize = new ResizeObserver(() => scroller.__consuloViewport.report());" +
            "  const report = () => {" +
            "    if (pending) { return; }" +
            "    pending = true;" +
            "    requestAnimationFrame(() => {" +
            "      pending = false;" +
            "      if (!scroller.isConnected) { return; }" +
            "      const base = scroller.getBoundingClientRect();" +
            "      const parts = [];" +
            "      scroller.querySelectorAll('[" + TRACK_ATTRIBUTE + "]').forEach(e => {" +
            "        const r = e.getBoundingClientRect();" +
            "        parts.push(e.getAttribute('" + TRACK_ATTRIBUTE + "') + ':' +" +
            "          Math.round(r.left - base.left - scroller.clientLeft + scroller.scrollLeft) + ':' +" +
            "          Math.round(r.top - base.top - scroller.clientTop + scroller.scrollTop) + ':' +" +
            "          Math.round(r.width) + ':' + Math.round(r.height));" +
            "      });" +
            "      scroller.dispatchEvent(new CustomEvent('consulo-viewport', { detail: {" +
            "        x: Math.round(scroller.scrollLeft), y: Math.round(scroller.scrollTop)," +
            "        width: scroller.clientWidth, height: scroller.clientHeight, bounds: parts.join(';') } }));" +
            "    });" +
            "  };" +
            "  const track = () => {" +
            "    resize.disconnect();" +
            "    resize.observe(scroller);" +
            "    if (scroller.lastElementChild) { resize.observe(scroller.lastElementChild); }" +
            "    scroller.querySelectorAll('[" + TRACK_ATTRIBUTE + "]').forEach(e => resize.observe(e));" +
            "    report();" +
            "  };" +
            "  scroller.addEventListener('scroll', report, { passive: true });" +
            "  scroller.__consuloViewport = { report: report, track: track };" +
            "}" +
            "scroller.__consuloViewport.track();";

    private final Map<Component, String> myTracked = new HashMap<>();
    private final Map<String, Rectangle2D> myTrackedBounds = new HashMap<>();
    private Rectangle2D myViewRect = new Rectangle2D(0, 0);
    private boolean myTrackPending;
    private @Nullable Element myOverlayHolder;

    public WebScrollLayoutImpl(Component component, ScrollableLayoutOptions options) {
        HasSize content = (HasSize) TargetVaadin.to(component);

        // full width but its own height: pinning the height to the box would make the content exactly as tall
        // as what it is scrolled inside, so there would never be anything to scroll to
        content.setWidthFull();
        content.setMinHeight("100%");

        Vaadin vaadin = getVaadinComponent();
        vaadin.setSizeFull();
        vaadin.getStyle()
            .set("overflow-x", toCssOverflow(options.getHorizontalScrollPolicy()))
            .set("overflow-y", toCssOverflow(options.getVerticalScrollPolicy()));

        if (options.getVerticalScrollBarPosition() == VerticalScrollBarPosition.LEFT) {
            vaadin.getStyle().set("direction", "rtl");
            ((com.vaadin.flow.component.Component) content).getElement().getStyle().set("direction", "ltr");
        }

        if (content instanceof Grid<?>) {
            vaadin.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column");
        }

        vaadin.add((com.vaadin.flow.component.Component) content);

        vaadin.getElement().addEventListener("consulo-viewport", event -> {
                int x = event.getEventData().path("event.detail.x").asInt(0);
                int y = event.getEventData().path("event.detail.y").asInt(0);
                int width = event.getEventData().path("event.detail.width").asInt(0);
                int height = event.getEventData().path("event.detail.height").asInt(0);
                String bounds = event.getEventData().path("event.detail.bounds").asText("");
                onViewportReported(new Rectangle2D(x, y, width, height), bounds);
            })
            .addEventData("event.detail.x")
            .addEventData("event.detail.y")
            .addEventData("event.detail.width")
            .addEventData("event.detail.height")
            .addEventData("event.detail.bounds")
            .debounce(VIEWPORT_THROTTLE_MS, DebouncePhase.LEADING, DebouncePhase.INTERMEDIATE, DebouncePhase.TRAILING);

        vaadin.getElement().addAttachListener(event -> installViewportScript());
        installViewportScript();
    }

    private void installViewportScript() {
        getVaadinComponent().getElement().executeJs(INSTALL_SCRIPT);
    }

    @RequiredUIAccess
    private void onViewportReported(Rectangle2D viewRect, String bounds) {
        myViewRect = viewRect;
        myTrackedBounds.clear();
        if (!bounds.isEmpty()) {
            for (String part : bounds.split(";")) {
                String[] values = part.split(":");
                if (values.length != 5) {
                    continue;
                }

                myTrackedBounds.put(values[0], new Rectangle2D(
                    Integer.parseInt(values[1]),
                    Integer.parseInt(values[2]),
                    Integer.parseInt(values[3]),
                    Integer.parseInt(values[4])
                ));
            }
        }

        getListenerDispatcher(ScrollableLayoutViewportEvent.class).onEvent(new ScrollableLayoutViewportEvent(this, viewRect));
    }

    @RequiredUIAccess
    @Override
    public Rectangle2D getViewRect() {
        return myViewRect;
    }

    @RequiredUIAccess
    @Override
    public void scrollTo(Point2D position) {
        myViewRect = new Rectangle2D(new Point2D(Math.max(0, position.x()), Math.max(0, position.y())), myViewRect.size());
        getVaadinComponent().getElement().executeJs("this.scrollTo($0, $1)", position.x(), position.y());
    }

    @RequiredUIAccess
    @Override
    public void setBoundsTracked(Component descendant, boolean tracked) {
        com.vaadin.flow.component.Component vaadinComponent = TargetVaadin.to(descendant);
        if (tracked) {
            if (myTracked.containsKey(descendant)) {
                return;
            }

            String id = String.valueOf(ourTrackId.incrementAndGet());
            myTracked.put(descendant, id);
            vaadinComponent.getElement().setAttribute(TRACK_ATTRIBUTE, id);
        }
        else {
            String id = myTracked.remove(descendant);
            if (id == null) {
                return;
            }

            myTrackedBounds.remove(id);
            vaadinComponent.getElement().removeAttribute(TRACK_ATTRIBUTE);
        }
        scheduleTrack();
    }

    private void scheduleTrack() {
        if (myTrackPending) {
            return;
        }

        myTrackPending = true;
        Vaadin vaadin = getVaadinComponent();
        vaadin.getElement().getNode().runWhenAttached(ui -> ui.beforeClientResponse(vaadin, context -> {
            myTrackPending = false;
            installViewportScript();
        }));
    }

    @RequiredUIAccess
    @Override
    public void setTopOverlay(@Nullable Component overlay) {
        Vaadin vaadin = getVaadinComponent();
        if (myOverlayHolder != null) {
            vaadin.getElement().removeChild(myOverlayHolder);
            myOverlayHolder = null;
        }
        if (overlay == null) {
            return;
        }

        Element holder = new Element("div");
        holder.getClassList().add("web-scroll-layout-overlay");
        holder.appendChild(TargetVaadin.to(overlay).getElement());
        vaadin.getElement().insertChild(0, holder);
        myOverlayHolder = holder;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Rectangle2D getBounds(Component descendant) {
        String id = myTracked.get(descendant);
        return id == null ? null : myTrackedBounds.get(id);
    }

    private static String toCssOverflow(ScrollableLayoutOptions.ScrollPolicy policy) {
        return switch (policy) {
            case ALWAYS -> "scroll";
            case NEVER -> "hidden";
            default -> "auto";
        };
    }

    @Override
    @RequiredUIAccess
    public void remove(Component component) {
        com.vaadin.flow.component.Component vaadinComponent = TargetVaadin.to(component);
        if (vaadinComponent == getFirstChild()) {
            removeAll();
        }
        else {
            throw new IllegalArgumentException("Not content");
        }
    }

    private com.vaadin.flow.component.@Nullable Component getFirstChild() {
        int componentCount = toVaadinComponent().getComponentCount();
        if (componentCount == 1) {
            return toVaadinComponent().getComponentAt(0);
        }
        return null;
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        WebLayoutChildren.accept(getFirstChild(), consumer);
    }

    @Override
    @RequiredUIAccess
    public void removeAll() {
        getVaadinComponent().removeAll();
    }

    @Override
    public WebScrollLayoutImpl.Vaadin createVaadinComponent() {
        return new Vaadin();
    }
}