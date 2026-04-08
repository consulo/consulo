// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import com.intellij.collaboration.ui.html.AsyncHtmlImageLoader;
import com.intellij.collaboration.ui.html.ResizingHtmlImageView;
import consulo.application.localize.ApplicationLocalize;
import consulo.ui.AntialiasingType;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.GraphicsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.*;
import java.awt.*;
import java.net.URL;
import java.util.function.Consumer;

public final class HtmlEditorPaneUtil {
    private HtmlEditorPaneUtil() {
    }

    /**
     * Show tooltip from HTML title attribute.
     * Syntax is {@code <{CONTENT_TAG} title="{text}">}
     */
    public static final ExtendableHTMLViewFactory.Extension CONTENT_TOOLTIP = (elem, defaultView) -> {
        if (!(defaultView instanceof InlineView)) {
            return null;
        }
        return new InlineView(elem) {
            @Override
            public String getToolTipText(float x, float y, Shape allocation) {
                Object title = getElement().getAttributes().getAttribute(HTML.Attribute.TITLE);
                if (title instanceof String s && !s.isEmpty()) {
                    return s;
                }
                return super.getToolTipText(x, y, allocation);
            }
        };
    };

    /**
     * Handles image loading and scaling
     */
    public static final ExtendableHTMLViewFactory.Extension IMAGES_EXTENSION = (elem, view) -> {
        if (view instanceof ImageView) {
            return new ResizingHtmlImageView(elem);
        }
        return view;
    };

    /**
     * Show an icon inlined with the text.
     * Syntax is {@code <icon-inline src="..."/>}
     *
     * @deprecated Use {@link #inlineIconExtension(Class)}
     */
    @Deprecated
    public static final ExtendableHTMLViewFactory.Extension INLINE_ICON_EXTENSION = inlineIconExtension(HtmlEditorPaneUtil.class);

    /**
     * Show an icon inlined with the text.
     * Syntax is {@code <icon-inline src="..."/>}
     *
     * @param aClass Class used for its classloader to find reflexive icons on the classpath.
     */
    public static @Nonnull ExtendableHTMLViewFactory.Extension inlineIconExtension(@Nonnull Class<?> aClass) {
        return (elem, view) -> {
            if ("icon-inline".equals(elem.getName())) {
                Object srcAttr = elem.getAttributes().getAttribute(HTML.Attribute.SRC);
                if (srcAttr instanceof String path) {
                    Icon icon = IconLoader.findIcon(path, aClass, true, false);
                    if (icon != null) {
                        return new InlineView(elem) {
                            @Override
                            public float getPreferredSpan(int axis) {
                                if (axis == X_AXIS) {
                                    return icon.getIconWidth() + super.getPreferredSpan(axis);
                                }
                                return super.getPreferredSpan(axis);
                            }

                            @Override
                            public void paint(Graphics g, Shape allocation) {
                                super.paint(g, allocation);
                                icon.paintIcon(null, g, allocation.getBounds().x, allocation.getBounds().y);
                            }
                        };
                    }
                }
            }
            return view;
        };
    }

    /**
     * Read-only editor pane intended to display simple HTML snippet
     */
    public static @Nonnull JEditorPane createSimpleHtmlPane() {
        return createSimpleHtmlPane(null, true, null, null, HtmlEditorPaneUtil.class);
    }

    /**
     * Read-only editor pane intended to display simple HTML snippet
     */
    public static @Nonnull JEditorPane createSimpleHtmlPane(@Language("HTML") @Nonnull String body) {
        JEditorPane pane = createSimpleHtmlPane();
        setHtmlBody(pane, body);
        return pane;
    }

    /**
     * Read-only editor pane intended to display simple HTML snippet
     */
    public static @Nonnull JEditorPane createSimpleHtmlPane(
        @Nullable StyleSheet additionalStyleSheet,
        boolean addBrowserListener,
        @Nullable AsyncHtmlImageLoader customImageLoader,
        @Nullable URL baseUrl,
        @Nonnull Class<?> aClass
    ) {
        JEditorPane pane = new JBHtmlPane(
            JBHtmlPaneStyleConfiguration.builder().build(),
            JBHtmlPaneConfiguration.builder()
                .customStyleSheet(
                    "p {\n    padding: 0 0 0 0;\n}\np.custom_image {\n    padding: 4px 0 4px 0;\n}"
                )
                .customStyleSheetProvider(c -> additionalStyleSheet != null ? additionalStyleSheet : new StyleSheet())
                .extensions(
                    ExtendableHTMLViewFactory.Extensions.WORD_WRAP,
                    CONTENT_TOOLTIP,
                    inlineIconExtension(aClass),
                    IMAGES_EXTENSION
                )
                .build()
        );
        pane.setEditable(false);
        pane.setOpaque(false);
        if (addBrowserListener) {
            pane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
        }
        pane.setMargin(JBInsets.emptyInsets());
        GraphicsUtil.setAntialiasingType(pane, AntialiasingType.getAATextInfoForSwingComponent());
        ((DefaultCaret) pane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        if (customImageLoader != null) {
            pane.getDocument().putProperty(AsyncHtmlImageLoader.KEY, customImageLoader);
        }
        if (baseUrl != null) {
            ((HTMLDocument) pane.getDocument()).setBase(baseUrl);
        }
        pane.setName("Simple HTML Pane");
        return pane;
    }

    public static void setHtmlBody(@Nonnull JEditorPane pane, @Language("HTML") @Nonnull String body) {
        if (body.isEmpty()) {
            pane.setText("");
        }
        else {
            pane.setText("<html><body>" + body + "</body></html>");
        }
        // JDK bug JBR-2256 - need to force height recalculation
        if (pane.getHeight() == 0) {
            pane.setSize(Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2);
        }
    }

    public static void onHyperlinkActivated(@Nonnull JEditorPane pane, @Nonnull Consumer<HyperlinkEvent> listener) {
        pane.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(HyperlinkEvent e) {
                listener.accept(e);
            }
        });
    }

    /**
     * Loading label with animated icon
     */
    public static @Nonnull JLabel createLoadingLabel() {
        return createLoadingLabel(null);
    }

    /**
     * Loading label with animated icon
     */
    public static @Nonnull JLabel createLoadingLabel(@Nullable @NlsContexts.Label String labelText) {
        JLabel label = new JLabel(CollaborationToolsUIUtil.getAnimatedLoadingIcon());
        label.setName("Animated loading label");
        label.setText(labelText);
        return label;
    }

    /**
     * Loading label with a text
     */
    public static @Nonnull JLabel createLoadingTextLabel() {
        return createLoadingTextLabel(ApplicationLocalize.labelLoadingPagePleaseWait().get());
    }

    /**
     * Loading label with a text
     */
    public static @Nonnull JLabel createLoadingTextLabel(@Nls @Nonnull String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UIUtil.getContextHelpForeground());
        label.setName("Textual loading label");
        return label;
    }

    /**
     * Scrollpane without background and borders
     */
    public static @Nonnull JScrollPane createTransparentScrollPane(@Nonnull JComponent content) {
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(content, true);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        return scrollPane;
    }

    /**
     * A simple stub that can be used to transfer focus to an empty panel
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public static @Nonnull JComponent createFocusableStub() {
        JLabel label = new JLabel();
        label.setFocusable(true);
        return label;
    }
}
