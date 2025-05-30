/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.InlayActionData;
import consulo.project.Project;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2025-05-30
 */
public class IconInlayPresentationEntry extends InlayPresentationEntry {
    private final Image myImage;
    private final byte myParentIndexToSwitch;

    public IconInlayPresentationEntry(Image myImage, byte parentIndexToSwitch, InlayMouseArea clickArea) {
        super(clickArea);
        this.myImage = myImage;
        this.myParentIndexToSwitch = parentIndexToSwitch;
    }

    @Override
    public void handleClick(EditorMouseEvent e, InlayPresentationList list, boolean controlDown) {
        Editor editor = e.getEditor();
        Project project = editor.getProject();
        if (clickArea != null && project != null) {
            InlayActionData actionData = clickArea.getActionData();
            if (controlDown) {
                ApplicationManager.getApplication()
                    .getService(DeclarativeInlayActionService.class)
                    .invokeActionHandler(actionData, e);
            }
        }

        if (myParentIndexToSwitch != (byte) -1) {
            list.toggleTreeState(myParentIndexToSwitch);
        }
    }

    @Override
    public void render(Graphics2D graphics,
                       InlayTextMetrics metrics,
                       TextAttributes attributes,
                       boolean isDisabled,
                       int yOffset,
                       int rectHeight,
                       Editor editor) {
        int centerImage = computeHeight(metrics) / 2;
        int centerRect = rectHeight / 2;

        TargetAWT.to(myImage).paintIcon(editor.getComponent(), graphics, 0, centerRect - centerImage);
    }

    @Override
    public int computeWidth(InlayTextMetrics metrics) {
        return myImage.getWidth();
    }

    @Override
    public int computeHeight(InlayTextMetrics metrics) {
        return myImage.getWidth();
    }
}
