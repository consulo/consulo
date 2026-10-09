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
package consulo.desktop.qt.ui.impl.htmlView;

import consulo.desktop.qt.ui.impl.DesktopQtCurrentInput;
import consulo.desktop.qt.ui.impl.DesktopQtInputDetails;
import consulo.desktop.qt.ui.impl.DesktopQtStyleApplier;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.logging.Logger;
import consulo.ui.HtmlView;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HtmlViewDoubleClickEvent;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import consulo.ui.impl.HtmlDocuments;
import consulo.util.io.StreamUtil;
import io.qt.core.QSize;
import io.qt.core.QUrl;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QDesktopServices;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPalette;
import io.qt.gui.QPixmap;
import io.qt.gui.QTextDocument;
import io.qt.widgets.QFrame;
import io.qt.widgets.QStyle;
import io.qt.widgets.QTextBrowser;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The document is rendered by the rich text engine of qt rather than by a browser - {@link HtmlView} is documented
 * as html4 without css3 and without scripting, which is exactly what that engine covers.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtHtmlViewImpl extends QtComponentDelegate<QTextBrowser> implements HtmlView {
    private static final Logger LOG = Logger.getInstance(DesktopQtHtmlViewImpl.class);

    /**
     * An {@code img} of the document naming an image of the platform is fetched through here rather than being
     * rewritten into the markup: the rich text engine asks for every resource it cannot resolve itself, so the id
     * stays an id and nothing has to be turned into a url first.
     */
    private class QtHtmlView extends QTextBrowser {
        QtHtmlView(QWidget parent) {
            super(parent);
        }

        @Override
        public Object loadResource(int type, QUrl name) {
            if (type == QTextDocument.ResourceType.ImageResource.value()) {
                QPixmap pixmap = resolveImage(name.toString());
                if (pixmap != null) {
                    return pixmap;
                }
            }

            return super.loadResource(type, name);
        }

        @Override
        protected void mouseReleaseEvent(QMouseEvent event) {
            myClickedAnchor = anchorAt(event.position().toPoint());
            try {
                super.mouseReleaseEvent(event);
            }
            finally {
                myClickedAnchor = null;
            }
        }

        @Override
        public QSize sizeHint() {
            if (!mySizeToContent) {
                return super.sizeHint();
            }

            QTextDocument layout = document().clone();
            try {
                layout.setTextWidth(CONTENT_WIDTH);
                double width = Math.min(CONTENT_WIDTH, Math.ceil(layout.idealWidth() + layout.documentMargin() * 2));
                layout.setTextWidth(width);

                int frame = frameWidth() * 2;
                int height = (int) Math.ceil(layout.size().height()) + frame;
                int scrollBar = 0;
                if (height > CONTENT_HEIGHT) {
                    height = CONTENT_HEIGHT;
                    scrollBar = style().pixelMetric(QStyle.PixelMetric.PM_ScrollBarExtent);
                }
                return new QSize((int) width + frame + scrollBar, height);
            }
            finally {
                layout.dispose();
            }
        }

        @Override
        protected void mouseDoubleClickEvent(QMouseEvent event) {
            super.mouseDoubleClickEvent(event);

            if (event.button() == Qt.MouseButton.LeftButton) {
                getListenerDispatcher(HtmlViewDoubleClickEvent.class).onEvent(
                    new HtmlViewDoubleClickEvent(DesktopQtHtmlViewImpl.this, DesktopQtInputDetails.mouse(this, event))
                );
            }
        }
    }

    private static final int CONTENT_WIDTH = 500;
    private static final int CONTENT_HEIGHT = 500;

    private volatile @Nullable Function<String, Image> myImageResolver;

    private @Nullable String myPendingHtml;

    private @Nullable String myClickedAnchor;

    private boolean mySizeToContent;

    @Override
    protected QTextBrowser createQt(QWidget parent) {
        return new QtHtmlView(parent);
    }

    @Override
    protected void initialize(QTextBrowser component) {
        component.setReadOnly(true);

        // the view carries no frame of its own - whatever holds it draws the border, the way the awt one is put
        // inside a scroll pane which owns the frame
        component.setFrameShape(QFrame.Shape.NoFrame);

        // a link is a choice the platform makes, not a page to walk to: the browser must neither navigate nor hand
        // the url to the desktop, only report it
        component.setOpenLinks(false);
        component.setOpenExternalLinks(false);

        component.anchorClicked.connect(url -> onLink(component, url));

        QPalette palette = component.palette();
        QColor separator = DesktopQtStyleApplier.themeColor(ComponentColors.SEPARATOR, palette.color(QPalette.ColorRole.Mid));
        palette.setColor(QPalette.ColorGroup.Active, QPalette.ColorRole.WindowText, separator);
        palette.setColor(QPalette.ColorGroup.Inactive, QPalette.ColorRole.WindowText, separator);
        component.setPalette(palette);

        String pending = myPendingHtml;
        if (pending != null) {
            myPendingHtml = null;

            setHtml(component, pending);
        }
    }

    private void onLink(QTextBrowser component, QUrl url) {
        String href = linkHref(component, url);

        if (myDataObject.hasListeners(HyperlinkEvent.class)) {
            getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, href, DesktopQtCurrentInput.current(component)));
            return;
        }

        if (href.startsWith("#")) {
            component.scrollToAnchor(href.substring(1));
        }
        else if (!url.isRelative()) {
            QDesktopServices.openUrl(url);
        }
    }

    private String linkHref(QTextBrowser component, QUrl url) {
        String clicked = myClickedAnchor;
        if (clicked != null && !clicked.isEmpty()) {
            return clicked;
        }

        String focused = component.textCursor().charFormat().anchorHref();
        if (focused != null && !focused.isEmpty()) {
            return focused;
        }
        return url.toString();
    }

    private void setHtml(QTextBrowser component, String html) {
        component.setHtml(html);

        if (mySizeToContent) {
            component.updateGeometry();
        }
    }

    @Override
    @RequiredUIAccess
    public void scrollToFragment(String fragment) {
        QTextBrowser component = myComponent;
        if (component != null && !fragment.isEmpty()) {
            component.scrollToAnchor(fragment);
        }
    }

    @Override
    @RequiredUIAccess
    public void setSizeToContent(boolean sizeToContent) {
        mySizeToContent = sizeToContent;

        QTextBrowser component = myComponent;
        if (component != null) {
            component.updateGeometry();
        }
    }

    private @Nullable QPixmap resolveImage(String source) {
        if (!source.startsWith(IMAGE_SRC_PREFIX)) {
            return null;
        }

        Function<String, Image> resolver = myImageResolver;
        if (resolver == null) {
            return null;
        }

        String id = source.substring(IMAGE_SRC_PREFIX.length());

        try {
            Image image = resolver.apply(id);

            return image instanceof DesktopQtImage qtImage ? qtImage.toQPixmap() : null;
        }
        catch (Exception e) {
            LOG.warn("Failed to resolve the image " + id + " of an html view", e);
            return null;
        }
    }

    @Override
    public void setImageResolver(@Nullable Function<String, Image> imageResolver) {
        myImageResolver = imageResolver;
    }

    @Override
    public CompletableFuture<?> render(RenderData renderData) {
        String document = buildDocument(renderData);

        QTextBrowser component = myComponent;
        if (component == null) {
            // the widget of a component only exists once it is bound, and a caller renders into the view it just
            // built rather than waiting for it to be shown
            myPendingHtml = document;
            return CompletableFuture.completedFuture(null);
        }

        if (UIAccess.isUIThread()) {
            setHtml(component, document);
            return CompletableFuture.completedFuture(null);
        }

        return getUIAccess().giveAsync(() -> {
            QTextBrowser current = myComponent;
            if (current != null) {
                setHtml(current, document);
            }
            return null;
        });
    }

    /**
     * Every stylesheet is folded into the document rather than linked. The urls a caller hands over point into the
     * running platform - a {@code jar:file:} inside a plugin - which the rich text engine cannot fetch.
     */
    private static String buildDocument(RenderData renderData) {
        StringBuilder head = new StringBuilder();

        for (URL css : renderData.externalCsses()) {
            if (css == null) {
                continue;
            }

            try (InputStream stream = css.openStream()) {
                head.append("<style>\n").append(StreamUtil.readText(stream, StandardCharsets.UTF_8)).append("\n</style>\n");
            }
            catch (IOException e) {
                LOG.warn("Failed to read the stylesheet " + css + " of an html view", e);
            }
        }

        String inlineCss = renderData.inlineCss();
        if (!inlineCss.isEmpty()) {
            head.append("<style>\n").append(inlineCss).append("\n</style>\n");
        }

        return HtmlDocuments.withHead(renderData.html(), head.toString());
    }

    /**
     * The rich text engine keeps no dom and drops the attributes of the source, so there is nothing left to look a
     * markdown offset up by - only the browser and the cobra dom of the awt frontend can answer this.
     */
    @Override
    public void scrollToMarkdownSrcOffset(int offset) {
    }
}
