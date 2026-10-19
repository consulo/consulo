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
package consulo.codeEditor.markup;

import consulo.ui.annotation.RequiredUIAccess;

/**
 * Told about a drag of an edge of a {@link LineFramePresentation}. The frame does not move by itself - moving the
 * highlighter it belongs to is up to the listener.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public interface LineFrameResizeListener {
    /**
     * The user grabbed {@code edge}.
     */
    @RequiredUIAccess
    void dragStarted(LineFrameEdge edge);

    /**
     * The pointer of the drag moved over another line.
     *
     * @param line the document line under the pointer
     */
    @RequiredUIAccess
    void dragged(int line);

    /**
     * The user let go of the edge.
     */
    @RequiredUIAccess
    void dragFinished();
}
