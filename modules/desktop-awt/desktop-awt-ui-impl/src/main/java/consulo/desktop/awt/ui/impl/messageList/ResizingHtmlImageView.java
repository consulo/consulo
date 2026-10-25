// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.impl.messageList;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.impl.messageList.MessageListImages;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.net.URL;

/**
 * Custom image view to be used with an HTML view factory
 * <p>
 * Images are automatically resized to fit the panel width
 * <p>
 * Image loading can be customized via {@link #LOADER_PROPERTY} property in a document
 */
public class ResizingHtmlImageView extends View {
    public static final String LOADER_PROPERTY = "Async.Html.Image.Loader";

    private static Icon loadingIcon() {
        return TargetAWT.to(PlatformIconGroup.processStep_passive());
    }

    private static Icon notLoadedIcon() {
        return TargetAWT.to(PlatformIconGroup.filetypesUnknown());
    }

    private @Nullable ImageLoader myLoader;
    private @Nullable Container myCachedContainer;
    private @Nullable Rectangle myLastPaintedRectangle;

    public ResizingHtmlImageView(Element element) {
        super(element);
    }

    private ImageLoader getLoader() {
        ImageLoader loader = myLoader;
        if (loader == null) {
            loader = createImageLoader();
            myLoader = loader;
        }
        return loader;
    }

    private void resetLoader() {
        ImageLoader loader = myLoader;
        if (loader != null) {
            loader.cancel();
        }
        myLoader = null;
    }

    @Override
    public float getPreferredSpan(int axis) {
        ImageLoader.State state = getLoader().getState();
        return switch (state) {
            case ImageLoader.State.NotLoaded ignored -> getSpan(notLoadedIcon(), axis);
            case ImageLoader.State.Loading loading ->
                loading.dimension() != null ? getSpan(loading.dimension(), axis) : getSpan(loadingIcon(), axis);
            case ImageLoader.State.Loaded loaded -> getSpan(loaded.dimension(), axis);
        };
    }

    /**
     * If the image was loaded we ALWAYS "break" this view to scale the image properly - {@link #breakView}
     */
    @Override
    public int getBreakWeight(int axis, float pos, float len) {
        return axis == X_AXIS && getLoader().getState() instanceof ImageLoader.State.Loaded ? ForcedBreakWeight : BadBreakWeight;
    }

    @Override
    public View breakView(int axis, int offset, float pos, float len) {
        if (axis == X_AXIS && getLoader().getState() instanceof ImageLoader.State.Loaded loaded) {
            preferenceChanged(null, false, true);
            float imageWidth = Math.max(loaded.dimension().width, 1f);
            float imageHeight = Math.max(loaded.dimension().height, 1f);

            float maxHeight = JBUI.scale(MessageListImages.MAX_IMAGE_HEIGHT);
            if (imageWidth <= len && imageHeight <= maxHeight) {
                return new SizedImageView(loaded.image(), imageWidth, imageHeight);
            }

            float scale = Math.min(Math.min(len / imageWidth, maxHeight / imageHeight), 1f);

            float newWidth = Math.max(imageWidth * scale, 1f);
            float newHeight = Math.max(imageHeight * scale, 1f);

            return new SizedImageView(loaded.image(), newWidth, newHeight);
        }
        return this;
    }

    @Override
    public float getAlignment(int axis) {
        return 0f;
    }

    @Override
    public void paint(Graphics g, Shape allocation) {
        Rectangle rect = allocation instanceof Rectangle rectangle ? rectangle : allocation.getBounds();
        ImageLoader currentLoader = getLoader();
        currentLoader.loadImage();
        switch (currentLoader.getState()) {
            case ImageLoader.State.NotLoaded ignored -> notLoadedIcon().paintIcon(null, g, rect.x, rect.y);
            case ImageLoader.State.Loading ignored -> loadingIcon().paintIcon(null, g, rect.x, rect.y);
            case ImageLoader.State.Loaded loaded -> drawImage(g, loaded.image(), rect, myCachedContainer);
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        // ignore if it does not have relevant changes
        if (e == null || e.getChange(getElement()) == null) {
            return;
        }
        super.changedUpdate(e, a, f);
        resetLoader();
        preferenceChanged(null, true, true);
    }

    @Override
    public void setParent(@Nullable View parent) {
        super.setParent(parent);
        myCachedContainer = parent == null ? null : parent.getContainer();
    }

    @Override
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        if (pos < getStartOffset() || pos > getEndOffset()) {
            throw new BadLocationException(getElement().toString(), pos);
        }

        Rectangle bounds = a.getBounds();
        int x = bounds.x + (pos == getEndOffset() ? bounds.width : 0);
        return new Rectangle(x, bounds.y, 0, bounds.height);
    }

    @Override
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle bounds = a.getBounds();
        int right = bounds.x + bounds.width;
        if (x >= right) {
            bias[0] = Position.Bias.Backward;
            return getEndOffset();
        }
        else {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
    }

    private ImageLoader createImageLoader() {
        Object src = getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
        URL base = ((HTMLDocument) getDocument()).getBase();
        Object asyncLoader = getDocument().getProperty(LOADER_PROPERTY);

        return new ImageLoader(base,
            src instanceof String text ? text : null,
            asyncLoader instanceof AsyncHtmlImageLoader loader ? loader : null,
            (old, state) -> {
                if (!old.equals(state)) {
                    preferenceChanged(null, true, true);
                }
                requestRepaint();
            });
    }

    private void requestRepaint() {
        Container container = myCachedContainer;
        if (container == null) {
            return;
        }
        Rectangle rect = myLastPaintedRectangle;
        if (rect == null) {
            container.repaint(100);
        }
        else {
            container.repaint(rect.x, rect.y, rect.width, rect.height);
        }
    }

    private static void drawImage(Graphics g, Image image, Rectangle rect, @Nullable Component component) {
        if (rect.width <= 0 || rect.height <= 0) {
            return;
        }
        TargetAWT.to(ImageEffects.resize(image, rect.width, rect.height)).paintIcon(component, g, rect.x, rect.y);
    }

    private static float getSpan(Dimension dimension, int axis) {
        return axis == X_AXIS ? dimension.width : dimension.height;
    }

    private static float getSpan(Icon icon, int axis) {
        return axis == X_AXIS ? icon.getIconWidth() : icon.getIconHeight();
    }

    // image should be loaded by this point
    private class SizedImageView extends View {
        private final Image myImage;
        private final float myWidth;
        private final float myHeight;

        private SizedImageView(Image image, float width, float height) {
            super(ResizingHtmlImageView.this.getElement());
            myImage = image;
            myWidth = width;
            myHeight = height;
        }

        @Override
        public float getPreferredSpan(int axis) {
            return axis == X_AXIS ? myWidth : myHeight;
        }

        @Override
        public void paint(Graphics g, Shape allocation) {
            Rectangle rect = allocation instanceof Rectangle rectangle ? rectangle : allocation.getBounds();
            drawImage(g, myImage, rect, myCachedContainer);
            myLastPaintedRectangle = rect;
        }

        @Override
        public float getAlignment(int axis) {
            return ResizingHtmlImageView.this.getAlignment(axis);
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            return ResizingHtmlImageView.this.modelToView(pos, a, b);
        }

        @Override
        public int viewToModel(float x, float y, Shape a, Position.Bias[] biasReturn) {
            return ResizingHtmlImageView.this.viewToModel(x, y, a, biasReturn);
        }
    }
}
