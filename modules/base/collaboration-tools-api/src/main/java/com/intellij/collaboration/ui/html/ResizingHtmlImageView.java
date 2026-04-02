// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.html;

import consulo.application.AllIcons;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.JBUI;
import kotlinx.coroutines.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.net.URL;
import java.util.Base64;
import java.util.function.BiConsumer;

/**
 * Custom image view to be used with {@link com.intellij.util.ui.ExtendableHTMLViewFactory}.
 * <p>
 * Images are automatically resized to fit the panel width.
 * Image loading can be customized via {@link AsyncHtmlImageLoader#KEY} property in a document.
 */
public final class ResizingHtmlImageView extends View {
    private static final int MAX_IMAGE_HEIGHT = 500;

    private ImageLoader loader;
    private Container cachedContainer;
    private Rectangle lastPaintedRectangle;

    public ResizingHtmlImageView(@Nonnull Element element) {
        super(element);
    }

    private @Nonnull ImageLoader getOrCreateLoader() {
        if (loader == null) {
            loader = createImageLoader();
        }
        return loader;
    }

    private void cancelLoader() {
        if (loader != null) {
            loader.cancel();
            loader = null;
        }
    }

    @Override
    public float getPreferredSpan(int axis) {
        ImageLoader.State state = getOrCreateLoader().state;
        if (state instanceof ImageLoader.NotLoaded) {
            return getIconSpan(AllIcons.FileTypes.Image, axis);
        }
        else if (state instanceof ImageLoader.Loading loading) {
            return loading.dimension != null
                ? getDimensionSpan(loading.dimension, axis)
                : getIconSpan(PlatformIconGroup.processStep_passive(), axis);
        }
        else if (state instanceof ImageLoader.Loaded loaded) {
            return getDimensionSpan(loaded.dimension, axis);
        }
        return 0;
    }

    @Override
    public int getBreakWeight(int axis, float pos, float len) {
        if (axis == X_AXIS && getOrCreateLoader().state instanceof ImageLoader.Loaded) {
            return ForcedBreakWeight;
        }
        return BadBreakWeight;
    }

    @Override
    public View breakView(int axis, int offset, float pos, float len) {
        ImageLoader.State state = getOrCreateLoader().state;
        if (axis == X_AXIS && state instanceof ImageLoader.Loaded loaded) {
            preferenceChanged(null, false, true);
            float imageWidth = Math.max(loaded.dimension.width, 1f);
            float imageHeight = Math.max(loaded.dimension.height, 1f);

            float maxHeight = JBUI.scale(MAX_IMAGE_HEIGHT);
            if (imageWidth <= len && imageHeight <= maxHeight) {
                return new SizedImageView(loaded.image, imageWidth, imageHeight);
            }

            float scale = Math.min(Math.min(len / imageWidth, maxHeight / imageHeight), 1f);
            float newWidth = Math.max(imageWidth * scale, 1f);
            float newHeight = Math.max(imageHeight * scale, 1f);
            return new SizedImageView(loaded.image, newWidth, newHeight);
        }
        return this;
    }

    @Override
    public float getAlignment(int axis) {
        return 0f;
    }

    @Override
    public void paint(@Nonnull Graphics g, @Nonnull Shape allocation) {
        Rectangle rect = allocation instanceof Rectangle r ? r : allocation.getBounds();
        ImageLoader currentLoader = getOrCreateLoader();
        currentLoader.loadImage();
        ImageLoader.State state = currentLoader.state;
        if (state instanceof ImageLoader.NotLoaded) {
            AllIcons.FileTypes.Image.paintIcon(null, g, rect.x, rect.y);
        }
        else if (state instanceof ImageLoader.Loading) {
            PlatformIconGroup.processStep_passive().paintIcon(null, g, rect.x, rect.y);
        }
        else if (state instanceof ImageLoader.Loaded loaded) {
            StartupUiUtil.drawImage(g, loaded.image, rect, null);
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        if (e.getChange(getElement()) == null) {
            return;
        }
        super.changedUpdate(e, a, f);
        cancelLoader();
        preferenceChanged(null, true, true);
    }

    @Override
    public void setParent(View parent) {
        super.setParent(parent);
        cachedContainer = parent != null ? parent.getContainer() : null;
    }

    @Override
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        if (pos < getStartOffset() || pos > getEndOffset()) {
            throw new BadLocationException(getElement().toString(), pos);
        }
        int x = a.getBounds().x + (pos == getEndOffset() ? a.getBounds().width : 0);
        return new Rectangle(x, a.getBounds().y, 0, a.getBounds().height);
    }

    @Override
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        int right = a.getBounds().x + a.getBounds().width;
        if (x >= right) {
            bias[0] = Position.Bias.Backward;
            return getEndOffset();
        }
        else {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
    }

    private @Nonnull ImageLoader createImageLoader() {
        Object srcAttr = getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
        String src = srcAttr instanceof String s ? s : null;
        URL base = ((HTMLDocument) getDocument()).getBase();
        Object loaderProp = getDocument().getProperty(AsyncHtmlImageLoader.KEY);
        AsyncHtmlImageLoader asyncLoader = loaderProp instanceof AsyncHtmlImageLoader al ? al : null;

        return new ImageLoader(base, src, asyncLoader, (oldState, newState) -> {
            if (oldState != newState) {
                preferenceChanged(null, true, true);
            }
            requestRepaint();
        });
    }

    private void requestRepaint() {
        Rectangle rect = lastPaintedRectangle;
        if (rect == null) {
            if (cachedContainer != null) {
                cachedContainer.repaint(100);
            }
        }
        else {
            if (cachedContainer != null) {
                cachedContainer.repaint(rect.x, rect.y, rect.width, rect.height);
            }
        }
    }

    private float getDimensionSpan(@Nonnull Dimension dim, int axis) {
        return (axis == X_AXIS ? dim.width : dim.height);
    }

    private float getIconSpan(@Nonnull Icon icon, int axis) {
        return (axis == X_AXIS ? icon.getIconWidth() : icon.getIconHeight());
    }

    private final class SizedImageView extends View {
        private final Image image;
        private final float width;
        private final float height;

        SizedImageView(@Nonnull Image image, float width, float height) {
            super(ResizingHtmlImageView.this.getElement());
            this.image = image;
            this.width = width;
            this.height = height;
        }

        @Override
        public float getPreferredSpan(int axis) {
            return axis == X_AXIS ? width : height;
        }

        @Override
        public void paint(Graphics g, Shape allocation) {
            Rectangle rect = allocation instanceof Rectangle r ? r : allocation.getBounds();
            StartupUiUtil.drawImage(g, image, rect, null);
            lastPaintedRectangle = rect;
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

    private static final class ImageLoader implements ImageObserver {
        private final @Nullable URL baseUrl;
        private final @Nullable String src;
        private final @Nullable AsyncHtmlImageLoader asyncLoader;
        private final BiConsumer<State, State> onStateChange;
        private Job loadingJob;
        private final Dimension dimension = new Dimension(-1, -1);
        volatile State state = new NotLoaded();

        ImageLoader(
            @Nullable URL baseUrl, @Nullable String src,
            @Nullable AsyncHtmlImageLoader asyncLoader,
            @Nonnull BiConsumer<State, State> onStateChange
        ) {
            this.baseUrl = baseUrl;
            this.src = src;
            this.asyncLoader = asyncLoader;
            this.onStateChange = onStateChange;
        }

        void setState(@Nonnull State newState) {
            State old = this.state;
            this.state = newState;
            onStateChange.accept(old, newState);
        }

        void loadImage() {
            if (loadingJob == null) {
                loadingJob = requestImage();
            }
        }

        private @Nonnull Job requestImage() {
            if (src == null) {
                Job job = JobKt.Job(null);
                job.cancel(null);
                return job;
            }
            try {
                if (src.startsWith("data:image") && src.contains("base64")) {
                    Job result = JobKt.Job(null);
                    Image base64Image = tryCreateBase64Image(src);
                    if (base64Image != null) {
                        JBImageToolkit.prepareImage(base64Image, -1, -1, this);
                        ((CompletableJob) result).complete();
                    }
                    else {
                        result.cancel(null);
                    }
                    return result;
                }
                if (asyncLoader != null) {
                    setState(new Loading(null));
                    // Async loading - simplified for Java conversion
                    Job result = JobKt.Job(null);
                    try {
                        Image image = asyncLoader.load(baseUrl, src);
                        if (image instanceof BufferedImage bi) {
                            setState(new Loaded(image, new Dimension(bi.getWidth(), bi.getHeight())));
                        }
                        else if (image != null) {
                            JBImageToolkit.prepareImage(image, -1, -1, this);
                        }
                        ((CompletableJob) result).complete();
                    }
                    catch (Exception e) {
                        setState(new NotLoaded());
                        result.cancel(null);
                    }
                    return result;
                }
                else {
                    URL url = baseUrl != null ? new URL(baseUrl, src) : new URL(src);
                    Image image = JBImageToolkit.createImage(url);
                    JBImageToolkit.prepareImage(image, -1, -1, this);
                    Job job = JobKt.Job(null);
                    ((CompletableJob) job).complete();
                    return job;
                }
            }
            catch (Exception e) {
                Job job = JobKt.Job(null);
                ((CompletableJob) job).completeExceptionally(e);
                return job;
            }
        }

        @Override
        public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
            if (loadingJob != null && loadingJob.isCancelled()) {
                return false;
            }
            return doUpdate(img, flags, width, height);
        }

        private boolean doUpdate(Image img, int flags, int width, int height) {
            if ((flags & (ImageObserver.ABORT | ImageObserver.ERROR)) != 0) {
                setState(new NotLoaded());
                return false;
            }
            if ((flags & ImageObserver.WIDTH) != 0) {
                dimension.width = width;
            }
            if ((flags & ImageObserver.HEIGHT) != 0) {
                dimension.height = height;
            }
            if ((flags & (ImageObserver.FRAMEBITS | ImageObserver.ALLBITS | ImageObserver.SOMEBITS)) != 0) {
                Dimension dim = new Dimension(img.getWidth(null), img.getHeight(null));
                setState(new Loaded(img, dim));
            }
            else if (!(state instanceof Loaded)) {
                setState(new Loading(width >= 0 && height >= 0 ? new Dimension(dimension) : null));
            }
            return (flags & ImageObserver.ALLBITS) == 0;
        }

        void cancel() {
            if (loadingJob != null) {
                loadingJob.cancel(null);
            }
        }

        sealed interface State {
        }

        record NotLoaded() implements State {
        }

        record Loading(@Nullable Dimension dimension) implements State {
        }

        record Loaded(@Nonnull Image image, @Nonnull Dimension dimension) implements State {
        }
    }

    private static @Nullable Image tryCreateBase64Image(@Nonnull String src) {
        String[] parts = src.split(",");
        if (parts.length != 2) {
            return null;
        }
        byte[] decodedImage = Base64.getDecoder().decode(parts[1]);
        return JBImageToolkit.createImage(decodedImage);
    }
}
