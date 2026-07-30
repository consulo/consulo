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
package consulo.desktop.awt.editor.impl.gutter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.execution.coverage.CoveragePresentation;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionImpl
public class CoveragePresentationPainter implements AwtLineMarkerPresentationPainter<CoveragePresentation> {
    @Override
    public Class<CoveragePresentation> getPresentationType() {
        return CoveragePresentation.class;
    }

    @Override
    public void paint(CoveragePresentation presentation, Editor editor, Graphics2D g, Rectangle bounds) {
        if (presentation.color() != null) {
            g.setColor(TargetAWT.to(presentation.color()));
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        if (presentation.icon() != null) {
            Icon icon = TargetAWT.to(presentation.icon());
            icon.paintIcon(editor.getComponent(), g, bounds.x, bounds.y);
        }
    }
}
