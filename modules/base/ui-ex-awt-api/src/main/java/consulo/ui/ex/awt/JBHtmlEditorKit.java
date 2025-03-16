// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.logging.Logger;
import consulo.ui.ex.awt.html.ColoredHRuleView;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

public class JBHtmlEditorKit extends HTMLEditorKit {

    public static JBHtmlEditorKit create() {
        return new JBHtmlEditorKit();
    }

    public static JBHtmlEditorKit create(boolean noGapsBetweenParagraphs) {
        return new JBHtmlEditorKit(noGapsBetweenParagraphs);
    }

    private static final Logger LOG = Logger.getInstance(JBHtmlEditorKit.class);

    private final ViewFactory myViewFactory = new JBHtmlFactory();

    private Function<String, Image> myImageResolver = null;

    private boolean myNoGapsBetweenParagraphs;

    @Override
    public Cursor getDefaultCursor() {
        return null;
    }

    private final HyperlinkListener myHyperlinkListener;

    public JBHtmlEditorKit() {
        this(true);
    }

    public JBHtmlEditorKit(boolean noGapsBetweenParagraphs) {
        myNoGapsBetweenParagraphs = noGapsBetweenParagraphs;
        myHyperlinkListener = new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                Element element = e.getSourceElement();
                if (element == null) {
                    return;
                }
                if (element.getName().equals("img")) {
                    return;
                }

                if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    setUnderlined(true, element);
                }
                else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
                    setUnderlined(false, element);
                }
            }

            private void setUnderlined(boolean underlined, @Nonnull Element element) {
                AttributeSet attributes = element.getAttributes();
                Object attribute = attributes.getAttribute(HTML.Tag.A);
                if (attribute instanceof MutableAttributeSet) {
                    MutableAttributeSet a = (MutableAttributeSet) attribute;
                    a.addAttribute(CSS.Attribute.TEXT_DECORATION, underlined ? "underline" : "none");
                    ((StyledDocument) element.getDocument()).setCharacterAttributes(element.getStartOffset(), element.getEndOffset() - element.getStartOffset(), a, false);
                }
            }
        };
    }

    public void setImageResolver(Function<String, Image> imageResolver) {
        myImageResolver = imageResolver;
    }

    @Override
    public Document createDefaultDocument() {
        StyleSheet style = getStyleSheet();

        updateStyle(style);
        
        if (myNoGapsBetweenParagraphs) {
            style.addRule("p { margin-top: 0; }");
        }

        // static class instead anonymous for exclude $this [memory leak]
        StyleSheet ss = new StyleSheetCompressionThreshold();
        ss.addStyleSheet(style);

        HTMLDocument doc = new HTMLDocument(ss);
        doc.setParser(getParser());
        doc.setAsynchronousLoadPriority(4);
        doc.setTokenThreshold(100);
        return doc;
    }

    private static StyleSheet updateStyle(StyleSheet style) {
        style.addRule("code { font-size: 100%; }"); // small by Swing's default
        style.addRule("small { font-size: small; }"); // x-small by Swing's default
        style.addRule("a { text-decoration: none;}");
        // override too large default margin "ul {margin-left-ltr: 50; margin-right-rtl: 50}" from javax/swing/text/html/default.css
        style.addRule("ul { margin-left-ltr: 10; margin-right-rtl: 10; }");
        // override too large default margin "ol {margin-left-ltr: 50; margin-right-rtl: 50}" from javax/swing/text/html/default.css
        // Select ol margin to have the same indentation as "ul li" and "ol li" elements (seems value 22 suites well)
        style.addRule("ol { margin-left-ltr: 22; margin-right-rtl: 22; }");
        return style;
    }

    @Override
    public void install(final JEditorPane pane) {
        super.install(pane);
        // JEditorPane.HONOR_DISPLAY_PROPERTIES must be set after HTMLEditorKit is completely installed
        pane.addPropertyChangeListener("editorKit", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                // In case JBUI user scale factor changes, the font will be auto-updated by BasicTextUI.installUI()
                // with a font of the properly scaled size. And is then propagated to CSS, making HTML text scale dynamically.

                // The default JEditorPane's font is the label font, seems there's no need to reset it here.
                // If the default font is overridden, more so we should not reset it.
                // However, if the new font is not UIResource - it won't be auto-scaled.
                // [tav] dodo: remove the next two lines in case there're no regressions
                //Font font = getLabelFont();
                //pane.setFont(font);

                // let CSS font properties inherit from the pane's font
                pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                pane.removePropertyChangeListener(this);
            }
        });
        pane.addHyperlinkListener(myHyperlinkListener);

        List<LinkController> listeners1 = filterLinkControllerListeners(pane.getMouseListeners());
        List<LinkController> listeners2 = filterLinkControllerListeners(pane.getMouseMotionListeners());
        // replace just the original listener
        if (listeners1.size() == 1 && listeners1.equals(listeners2)) {
            LinkController oldLinkController = listeners1.get(0);
            pane.removeMouseListener(oldLinkController);
            pane.removeMouseMotionListener(oldLinkController);
            MouseExitSupportLinkController newLinkController = new MouseExitSupportLinkController();
            pane.addMouseListener(newLinkController);
            pane.addMouseMotionListener(newLinkController);
        }
    }

    @Override
    public ViewFactory getViewFactory() {
        return myViewFactory;
    }

    @Nonnull
    private static List<LinkController> filterLinkControllerListeners(@Nonnull Object[] listeners) {
        return ContainerUtil.mapNotNull(listeners, o -> ObjectUtil.tryCast(o, LinkController.class));
    }

    @Override
    public void deinstall(@Nonnull JEditorPane c) {
        c.removeHyperlinkListener(myHyperlinkListener);
        super.deinstall(c);
    }

    private static class StyleSheetCompressionThreshold extends StyleSheet {
        @Override
        protected int getCompressionThreshold() {
            return -1;
        }
    }

    // Workaround for https://bugs.openjdk.java.net/browse/JDK-8202529
    private static class MouseExitSupportLinkController extends LinkController {
        @Override
        public void mouseExited(@Nonnull MouseEvent e) {
            mouseMoved(new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiersEx(), -1, -1, e.getClickCount(), e.isPopupTrigger(), e.getButton()));
        }
    }

    public class JBHtmlFactory extends HTMLFactory {
        @Override
        public View create(Element elem) {
            AttributeSet attrs = elem.getAttributes();
            if ("hr".equals(elem.getName())) {
                return new ColoredHRuleView(elem);
            }
            else if ("icon".equals(elem.getName())) {
                String src = (String) attrs.getAttribute(HTML.Attribute.SRC);
                if (src != null) {
                    if (src.contains("@")) {
                        int width = Image.DEFAULT_ICON_SIZE;
                        int height = Image.DEFAULT_ICON_SIZE;
                        Object widthValue = attrs.getAttribute(HTML.Attribute.WIDTH);
                        if (widthValue != null) {
                            width = StringUtil.parseInt(widthValue.toString(), width);
                        }
                        Object heightValue = attrs.getAttribute(HTML.Attribute.HEIGHT);
                        if (heightValue != null) {
                            height = StringUtil.parseInt(heightValue.toString(), height);
                        }
                        ImageKey imageKey = ImageKey.fromString(src, width, height);
                        return new MyIconView(elem, imageKey);
                    }
                    else {
                        try {
                            Image image = Image.fromUrl(new URL(src));
                            return new MyIconView(elem, image);
                        }
                        catch (IOException e) {
                            LOG.warn(e);
                        }
                    }
                }
            }
            else if ("img".equals(elem.getName())) {
                String src = (String) attrs.getAttribute(HTML.Attribute.SRC);
                // example: "data:image/png;base64,ENCODED_IMAGE_HERE"
                if (src != null && src.startsWith("data:image") && src.contains("base64")) {
                    String[] split = src.split(",");
                    if (split.length == 2) {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(split[1]))) {
                            BufferedImage image = ImageIO.read(bis);
                            if (image != null) {
                                return new MyBufferedImageView(elem, image);
                            }
                        }
                        catch (IllegalArgumentException | IOException e) {
                            LOG.debug(e);
                        }
                    }
                }

                if (src != null && myImageResolver != null) {
                    Image icon = myImageResolver.apply(src);
                    if (icon != null) {
                        return new MyIconView(elem, icon);
                    }
                }
            }
            return super.create(elem);
        }
    }


    private static class MyIconView extends View {
        private final Image myViewIcon;

        private MyIconView(Element elem, Image viewIcon) {
            super(elem);
            myViewIcon = viewIcon;
        }

        @Override
        public float getPreferredSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return myViewIcon.getWidth();
                case View.Y_AXIS:
                    return myViewIcon.getHeight();
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }

        @Override
        public String getToolTipText(float x, float y, Shape allocation) {
            return (String) super.getElement().getAttributes().getAttribute(HTML.Attribute.ALT);
        }

        @Override
        public void paint(Graphics g, Shape allocation) {
            TargetAWT.to(myViewIcon).paintIcon(null, g, allocation.getBounds().x, allocation.getBounds().y);
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            int p0 = getStartOffset();
            int p1 = getEndOffset();
            if ((pos >= p0) && (pos <= p1)) {
                Rectangle r = a.getBounds();
                if (pos == p1) {
                    r.x += r.width;
                }
                r.width = 0;
                return r;
            }
            throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
        }

        @Override
        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            Rectangle alloc = (Rectangle) a;
            if (x < alloc.x + (alloc.width / 2f)) {
                bias[0] = Position.Bias.Forward;
                return getStartOffset();
            }
            bias[0] = Position.Bias.Backward;
            return getEndOffset();
        }
    }

    private static class MyBufferedImageView extends View {
        private static final int DEFAULT_BORDER = 0;
        private final BufferedImage myBufferedImage;
        private final int width;
        private final int height;
        private final int border;
        private final float vAlign;

        private MyBufferedImageView(Element elem, BufferedImage myBufferedImage) {
            super(elem);
            this.myBufferedImage = myBufferedImage;
            int width = getIntAttr(HTML.Attribute.WIDTH, -1);
            int height = getIntAttr(HTML.Attribute.HEIGHT, -1);
            if (width < 0 && height < 0) {
                this.width = myBufferedImage.getWidth();
                this.height = myBufferedImage.getHeight();
            }
            else if (width < 0) {
                this.width = height * getAspectRatio();
                this.height = height;
            }
            else if (height < 0) {
                this.width = width;
                this.height = width / getAspectRatio();
            }
            else {
                this.width = width;
                this.height = height;
            }
            this.border = getIntAttr(HTML.Attribute.BORDER, DEFAULT_BORDER);
            Object alignment = elem.getAttributes().getAttribute(HTML.Attribute.ALIGN);
            float vAlign = 1.0f;
            if (alignment != null) {
                alignment = alignment.toString();
                if ("top".equals(alignment)) {
                    vAlign = 0f;
                }
                else if ("middle".equals(alignment)) {
                    vAlign = .5f;
                }
            }
            this.vAlign = vAlign;
        }

        private int getAspectRatio() {
            return myBufferedImage.getWidth() / myBufferedImage.getHeight();
        }

        private int getIntAttr(HTML.Attribute name, int defaultValue) {
            AttributeSet attr = getElement().getAttributes();
            if (attr.isDefined(name)) {
                String val = (String) attr.getAttribute(name);
                if (val == null) {
                    return defaultValue;
                }
                else {
                    try {
                        return Math.max(0, Integer.parseInt(val));
                    }
                    catch (NumberFormatException x) {
                        return defaultValue;
                    }
                }
            }
            else {
                return defaultValue;
            }
        }

        @Override
        public float getPreferredSpan(int axis) {
            switch (axis) {
                case View.X_AXIS:
                    return width + 2 * border;
                case View.Y_AXIS:
                    return height + 2 * border;
                default:
                    throw new IllegalArgumentException("Invalid axis: " + axis);
            }
        }

        @Override
        public String getToolTipText(float x, float y, Shape allocation) {
            return (String) super.getElement().getAttributes().getAttribute(HTML.Attribute.ALT);
        }

        @Override
        public void paint(Graphics g, Shape a) {
            Rectangle bounds = a.getBounds();
            g.drawImage(myBufferedImage, bounds.x + border, bounds.y + border, width, height, null);
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) {
            int p0 = getStartOffset();
            int p1 = getEndOffset();
            if ((pos >= p0) && (pos <= p1)) {
                Rectangle r = a.getBounds();
                if (pos == p1) {
                    r.x += r.width;
                }
                r.width = 0;
                return r;
            }
            return null;
        }

        @Override
        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            Rectangle alloc = (Rectangle) a;
            if (x < alloc.x + alloc.width) {
                bias[0] = Position.Bias.Forward;
                return getStartOffset();
            }
            bias[0] = Position.Bias.Backward;
            return getEndOffset();
        }

        @Override
        public float getAlignment(int axis) {
            if (axis == View.Y_AXIS) {
                return vAlign;
            }
            return super.getAlignment(axis);
        }
    }
}
