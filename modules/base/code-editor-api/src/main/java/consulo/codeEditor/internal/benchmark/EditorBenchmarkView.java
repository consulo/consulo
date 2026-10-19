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
package consulo.codeEditor.internal.benchmark;

import consulo.codeEditor.EditorCustomElementRenderer;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public interface EditorBenchmarkView {
    String getFrontendName();

    boolean isShown();

    int getViewportWidth();

    int getViewportHeight();

    double getDevicePixelRatio();

    EditorCustomElementRenderer createHintRenderer(String text);

    void beginRun();

    void endRun();

    void paintTextOffscreen(int scrollY);

    void repaintTextArea();

    void repaintGutter();

    boolean hasErrorStripe();

    void repaintErrorStripe();

    void resetPaint();

    void flushPaint(long deadlineNanos);

    long getPaintedPixels();
}
