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
package consulo.web.ui.impl.internal.image.viewer;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.image.viewer.ImageViewer;
import consulo.ui.image.viewer.ImageViewerChessboard;
import consulo.ui.image.viewer.ImageViewerGrid;
import consulo.ui.image.viewer.ImageViewerZoom;
import consulo.ui.impl.image.viewer.ImageViewerState;
import consulo.web.ui.impl.internal.WebColors;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageUrl;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * The browser does the drawing: an img in a box of the size of the image at its zoom, which a scrolling div centres
 * while it is smaller. The chessboard is a background of the box and the grid a background of a layer over the image,
 * both plain css - nothing is drawn on the server, and the server only hears of the pointer and the wheel.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
public class WebImageViewerImpl extends VaadinComponentDelegate<WebImageViewerImpl.Vaadin> implements ImageViewer, ImageViewerState.Host {
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebImageViewerImpl.this;
        }
    }

    /**
     * Control and the wheel zoom instead of scrolling, which only the browser can stop before it happens - so it
     * asks the server with an event of its own. The size of the viewport is watched the same way.
     * <p/>
     * The pointer is told where it is over the canvas ($0) when it moves, and when a scroll or a new size of the
     * canvas moves the canvas under it - the browser tells nothing then. The scroll offset is kept as it was before
     * the canvas got smaller, which the browser cuts to the new range before {@link #ANCHOR_SCRIPT} runs.
     */
    private static final String INSTALL_SCRIPT = """
        const viewport = this;
        const canvas = $0;
        if (viewport.$consuloImageViewer) {
            return;
        }
        viewport.$consuloImageViewer = true;
        viewport.$consuloScroll = {left: viewport.scrollLeft, top: viewport.scrollTop};

        let pointer = null;
        let told = {over: false, x: -1, y: -1};
        const tellPointer = () => {
            const rect = canvas.getBoundingClientRect();
            const over = pointer !== null && rect.width > 0 && rect.height > 0
                && pointer.x >= rect.left && pointer.y >= rect.top && pointer.x < rect.right && pointer.y < rect.bottom;
            const x = over ? pointer.x - rect.left : -1;
            const y = over ? pointer.y - rect.top : -1;
            if (over === told.over && x === told.x && y === told.y) {
                return;
            }
            told = {over: over, x: x, y: y};
            viewport.dispatchEvent(new CustomEvent('consulo-image-pointer', {detail: told}));
        };

        viewport.addEventListener('mousemove', event => {
            pointer = {x: event.clientX, y: event.clientY};
            tellPointer();
        });
        viewport.addEventListener('mouseleave', () => {
            pointer = null;
            tellPointer();
        });
        viewport.addEventListener('scroll', () => {
            viewport.$consuloScroll = {left: viewport.scrollLeft, top: viewport.scrollTop};
            tellPointer();
        }, {passive: true});
        new ResizeObserver(tellPointer).observe(canvas);

        viewport.addEventListener('wheel', event => {
            if (!event.ctrlKey || event.deltaY === 0 || viewport.wheelZoom === false) {
                return;
            }
            event.preventDefault();
            const rect = viewport.getBoundingClientRect();
            viewport.dispatchEvent(new CustomEvent('consulo-image-zoom', {
                detail: {up: event.deltaY < 0, x: event.clientX - rect.left, y: event.clientY - rect.top}
            }));
        }, {passive: false});
        // the whole size counts for the zoom, as the scroll bars come and go with it - the size within them for
        // where the canvas is centred; each one may change without the other
        const tellViewport = () => viewport.dispatchEvent(new CustomEvent('consulo-image-viewport', {
            detail: {
                width: viewport.offsetWidth,
                height: viewport.offsetHeight,
                clientWidth: viewport.clientWidth,
                clientHeight: viewport.clientHeight
            }
        }));
        new ResizeObserver(tellViewport).observe(viewport);
        new ResizeObserver(tellViewport).observe(viewport, {box: 'border-box'});
        """;

    /**
     * Scrolls to {@code scroll * $0 + $1} and {@code scroll * $0 + $2} - the new size of the canvas is in place by
     * the time this runs.
     */
    private static final String ANCHOR_SCRIPT = """
        const scroll = this.$consuloScroll || {left: this.scrollLeft, top: this.scrollTop};
        this.scrollLeft = Math.max(0, Math.round(scroll.left * $0 + $1));
        this.scrollTop = Math.max(0, Math.round(scroll.top * $0 + $2));
        this.$consuloScroll = {left: this.scrollLeft, top: this.scrollTop};
        """;

    private final ImageViewerState myState = new ImageViewerState(this, this);

    private final Div myCanvas = new Div();
    private final Element myImageElement = new Element("img");
    private final Div myGridLayer = new Div();

    private @Nullable Size2D myViewportSize;
    private @Nullable Size2D myClientSize;
    private Size2D myCanvasSize = Size2D.ZERO;

    public WebImageViewerImpl() {
        Vaadin viewport = getVaadinComponent();
        viewport.setSizeFull();
        viewport.getStyle()
            .set("display", "flex")
            .set("overflow", "auto")
            .set("min-width", "0")
            .set("min-height", "0")
            .set("box-sizing", "border-box");

        String insets = ImageViewerState.INSETS + "px";
        myCanvas.getStyle()
            // margin auto centres it while smaller, and unlike centring by flex leaves nothing out of reach once bigger
            .set("margin", "auto")
            .set("flex", "none")
            .set("position", "relative")
            .set("box-sizing", "border-box")
            .set("padding", insets)
            .set("background-clip", "content-box")
            .set("background-origin", "content-box");

        myImageElement.setAttribute("draggable", "false");
        myImageElement.getStyle()
            .set("display", "block")
            .set("width", "100%")
            .set("height", "100%")
            .set("user-select", "none");

        myGridLayer.getStyle()
            .set("position", "absolute")
            .set("left", insets)
            .set("top", insets)
            .set("right", insets)
            .set("bottom", insets)
            // the lines are between the pixels - every tile starts with one, so the first column and row are cut
            // away; unlike a clip-path, the background clip is snapped to pixels together with the tiles
            .set("box-sizing", "border-box")
            .set("padding", "1px 0 0 1px")
            .set("background-origin", "padding-box")
            .set("background-clip", "content-box")
            .set("pointer-events", "none");

        myCanvas.getElement().appendChild(myImageElement, myGridLayer.getElement());
        viewport.add(myCanvas);

        Element viewportElement = viewport.getElement();
        viewportElement.addEventListener("consulo-image-zoom", event -> {
            JsonNode data = event.getEventData();
            Point2D anchor = new Point2D(
                (int) Math.round(data.path("event.detail.x").asDouble(0)),
                (int) Math.round(data.path("event.detail.y").asDouble(0))
            );
            myState.wheelZoom(data.path("event.detail.up").asBoolean(false), anchor);
        }).addEventData("event.detail.up").addEventData("event.detail.x").addEventData("event.detail.y");

        viewportElement.addEventListener("consulo-image-viewport", event -> {
            JsonNode data = event.getEventData();
            myViewportSize = new Size2D(data.path("event.detail.width").asInt(0), data.path("event.detail.height").asInt(0));
            myClientSize = new Size2D(data.path("event.detail.clientWidth").asInt(0), data.path("event.detail.clientHeight").asInt(0));
            myState.viewportResized();
        })
            .addEventData("event.detail.width")
            .addEventData("event.detail.height")
            .addEventData("event.detail.clientWidth")
            .addEventData("event.detail.clientHeight");

        viewportElement.addEventListener("consulo-image-pointer", event -> {
            JsonNode data = event.getEventData();
            if (data.path("event.detail.over").asBoolean(false)) {
                myState.pointerMoved(data.path("event.detail.x").asDouble(0), data.path("event.detail.y").asDouble(0));
            }
            else {
                myState.pointerExited();
            }
        })
            .addEventData("event.detail.over")
            .addEventData("event.detail.x")
            .addEventData("event.detail.y")
            // unlike a throttle, tells where the pointer stopped
            .debounce(100, DebouncePhase.LEADING, DebouncePhase.INTERMEDIATE, DebouncePhase.TRAILING);

        viewport.addAttachListener(event -> viewportElement.executeJs(INSTALL_SCRIPT, myCanvas.getElement()));

        updateCanvas();
        updateChessboard();
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public @Nullable Image getImage() {
        return myState.getImage();
    }

    @RequiredUIAccess
    @Override
    public void setImage(@Nullable Image image) {
        myState.setImage(image);
    }

    @Override
    public ImageViewerZoom getZoom() {
        return myState;
    }

    @Override
    public @Nullable ImageViewerChessboard getChessboard() {
        return myState.getChessboard();
    }

    @RequiredUIAccess
    @Override
    public void setChessboard(@Nullable ImageViewerChessboard chessboard) {
        myState.setChessboard(chessboard);
    }

    @Override
    public @Nullable ImageViewerGrid getGrid() {
        return myState.getGrid();
    }

    @RequiredUIAccess
    @Override
    public void setGrid(@Nullable ImageViewerGrid grid) {
        myState.setGrid(grid);
    }

    @Override
    public boolean isWheelZoomEnabled() {
        return myState.isWheelZoomEnabled();
    }

    @RequiredUIAccess
    @Override
    public void setWheelZoomEnabled(boolean enabled) {
        myState.setWheelZoomEnabled(enabled);
        getVaadinComponent().getElement().setProperty("wheelZoom", enabled);
    }

    @Override
    public @Nullable Size2D getViewportSize() {
        return myViewportSize;
    }

    @RequiredUIAccess
    @Override
    public void imageChanged() {
        Image image = myState.getImage();
        String url = image == null ? null : WebImageUrl.toURL(image);
        if (url == null) {
            myImageElement.removeAttribute("src");
        }
        else {
            myImageElement.setAttribute("src", url);
        }

        updateCanvas();
    }

    @RequiredUIAccess
    @Override
    public void zoomChanged(double oldZoomFactor, @Nullable Point2D anchor) {
        Size2D oldCanvasSize = myCanvasSize;
        updateCanvas();

        // the canvas is centred within the scroll bars
        Size2D viewport = myClientSize;
        if (oldZoomFactor <= 0 || viewport == null) {
            return;
        }

        // ImageViewerState#anchoredScroll, but with the scroll offset only the browser knows
        double ratio = myState.getZoomFactor() / oldZoomFactor;
        Point2D point = anchor != null ? anchor : new Point2D(viewport.width() / 2, viewport.height() / 2);
        getVaadinComponent().getElement().executeJs(
            ANCHOR_SCRIPT,
            ratio,
            anchorOffset(ratio, point.x(), viewport.width(), oldCanvasSize.width()),
            anchorOffset(ratio, point.y(), viewport.height(), oldCanvasSize.height())
        );
    }

    /**
     * @return what is added to the old scroll offset times {@code ratio} to get the new one - the canvas was centred
     * with a gap before it while smaller than the viewport, and the insets keep their size at every zoom
     */
    private static double anchorOffset(double ratio, int anchor, int viewportSize, int oldCanvasSize) {
        double gap = Math.max(0, (viewportSize - oldCanvasSize) / 2d);
        return (anchor - gap - ImageViewerState.INSETS) * ratio + ImageViewerState.INSETS - anchor;
    }

    @RequiredUIAccess
    @Override
    public void appearanceChanged() {
        updateChessboard();
        updateGrid();
    }

    private void updateCanvas() {
        Size2D size = myState.getCanvasSize();
        myCanvasSize = size;
        myCanvas.setVisible(!size.isEmpty());
        myCanvas.getStyle()
            .set("width", size.width() + "px")
            .set("height", size.height() + "px");

        // no source pixel is mixed with another when zoomed in - each one shows as a square
        myImageElement.getStyle().set("image-rendering", myState.isSmoothScaling() ? "auto" : "pixelated");

        updateGrid();
    }

    private void updateChessboard() {
        Style style = myCanvas.getStyle();
        ImageViewerChessboard chessboard = myState.getChessboard();
        if (chessboard == null) {
            style.remove("background-color");
            style.remove("background-image");
            style.remove("background-size");
            return;
        }

        int tile = Math.max(1, chessboard.cellSize()) * 2;
        style.set("background-color", css(chessboard.lightColor()));
        // starting at three o'clock the dark quarters are the bottom right and the top left one of each tile
        style.set("background-image", "repeating-conic-gradient(from 90deg, " + css(chessboard.darkColor()) + " 0% 25%, transparent 0% 50%)");
        style.set("background-size", tile + "px " + tile + "px");
    }

    private void updateGrid() {
        Style style = myGridLayer.getStyle();
        ImageViewerGrid grid = myState.getVisibleGrid();
        Image image = myState.getImage();
        Size2D area = myState.getImageAreaSize();
        if (grid == null || image == null || area.isEmpty() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            style.remove("background-image");
            style.remove("background-size");
            return;
        }

        String mainColor = css(grid.lineColor().withAlpha(0x4D / 255f));
        String auxColor = css(grid.lineColor().withAlpha(0x26 / 255f));
        // the size a pixel is drawn at - the image is stretched over whole pixels of the screen
        double pixelX = (double) area.width() / image.getWidth();
        double pixelY = (double) area.height() / image.getHeight();
        int lineSpan = Math.max(1, grid.lineSpan());
        String span = pixelX * lineSpan + "px " + pixelY * lineSpan + "px";
        String pixel = pixelX + "px " + pixelY + "px";

        style.set("background-image", String.join(", ",
            "linear-gradient(to right, " + mainColor + " 1px, transparent 1px)",
            "linear-gradient(to bottom, " + mainColor + " 1px, transparent 1px)",
            "linear-gradient(to right, " + auxColor + " 1px, transparent 1px)",
            "linear-gradient(to bottom, " + auxColor + " 1px, transparent 1px)"
        ));
        style.set("background-size", String.join(", ", span, span, pixel, pixel));
    }

    private static String css(ColorValue color) {
        String css = WebColors.toCssColor(color);
        return css == null ? "transparent" : css;
    }
}
