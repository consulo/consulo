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
package consulo.desktop.awt.editor.impl.internal.benchmark;

import consulo.codeEditor.EditorCustomElementRenderer;
import consulo.codeEditor.internal.benchmark.EditorBenchmarkView;
import consulo.desktop.awt.editor.impl.internal.DesktopEditorImpl;
import consulo.desktop.awt.editor.impl.internal.EditorComponentImpl;
import consulo.ide.impl.idea.codeInsight.hints.HintRenderer;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class DesktopAwtEditorBenchmarkView implements EditorBenchmarkView {
    private final DesktopEditorImpl myEditor;
    private @Nullable BufferedImage myImage;

    public DesktopAwtEditorBenchmarkView(DesktopEditorImpl editor) {
        myEditor = editor;
    }

    @Override
    public String getFrontendName() {
        return "AWT " + Toolkit.getDefaultToolkit().getClass().getSimpleName();
    }

    @Override
    public boolean isShown() {
        return myEditor.getContentComponent().isShowing() && getViewportHeight() > 0;
    }

    @Override
    public int getViewportWidth() {
        return myEditor.getScrollPane().getViewport().getExtentSize().width;
    }

    @Override
    public int getViewportHeight() {
        return myEditor.getScrollPane().getViewport().getExtentSize().height;
    }

    @Override
    public double getDevicePixelRatio() {
        return scale();
    }

    @Override
    public EditorCustomElementRenderer createHintRenderer(String text) {
        return new HintRenderer(text);
    }

    @Override
    public void beginRun() {
    }

    @Override
    public void endRun() {
        myImage = null;
    }

    @Override
    public void paintTextOffscreen(int scrollY) {
        EditorComponentImpl content = myEditor.getContentComponent();
        int width = getViewportWidth();
        int height = getViewportHeight();
        double scale = scale();

        BufferedImage image = myImage;
        int imageWidth = (int) Math.ceil(width * scale);
        int imageHeight = (int) Math.ceil(height * scale);
        if (image == null || image.getWidth() != imageWidth || image.getHeight() != imageHeight) {
            image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            myImage = image;
        }

        Graphics2D graphics = image.createGraphics();
        try {
            graphics.scale(scale, scale);
            graphics.translate(0, -scrollY);
            graphics.setClip(0, scrollY, width, height);
            content.paint(graphics);
        }
        finally {
            graphics.dispose();
        }
    }

    @Override
    public void repaintTextArea() {
        EditorComponentImpl content = myEditor.getContentComponent();
        content.paintImmediately(content.getVisibleRect());
    }

    @Override
    public void repaintGutter() {
        JComponent gutter = myEditor.getGutterComponentEx();
        gutter.paintImmediately(gutter.getVisibleRect());
    }

    @Override
    public boolean hasErrorStripe() {
        JScrollBar scrollBar = myEditor.getVerticalScrollBar();
        return scrollBar.isShowing() && scrollBar.getHeight() > 0;
    }

    @Override
    public void repaintErrorStripe() {
        JScrollBar scrollBar = myEditor.getVerticalScrollBar();
        scrollBar.paintImmediately(scrollBar.getVisibleRect());
    }

    @Override
    public void resetPaint() {
    }

    @Override
    public void flushPaint(long deadlineNanos) {
        RepaintManager.currentManager(myEditor.getContentComponent()).paintDirtyRegions();
    }

    @Override
    public long getPaintedPixels() {
        return -1;
    }

    private double scale() {
        GraphicsConfiguration configuration = myEditor.getContentComponent().getGraphicsConfiguration();
        return configuration == null ? 1 : configuration.getDefaultTransform().getScaleX();
    }
}
