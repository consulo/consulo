/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.Presentation;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.event.EditorMouseEventArea;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * @author msk
 */
public class PasteFromX11Action extends EditorAction {
    private static final Logger LOG = Logger.getInstance(PasteFromX11Action.class);

    public PasteFromX11Action() {
        super(new Handler());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Editor editor = e.getData(Editor.KEY);
        if (editor == null || !Platform.current().os().isXWindow()) {
            presentation.setEnabled(false);
        }
        else {
            boolean rightPlace = true;
            InputEvent inputEvent = e.getInputEvent();
            if (inputEvent instanceof MouseEvent me) {
                rightPlace = false;
                if (editor.getMouseEventArea(me) == EditorMouseEventArea.EDITING_AREA) {
                    Component component = SwingUtilities.getDeepestComponentAt(me.getComponent(), me.getX(), me.getY());
                    rightPlace = !(component instanceof JScrollBar);
                }
            }
            presentation.setEnabled(rightPlace);
        }
    }

    public static class Handler extends BasePasteHandler {
        @Override
        protected Transferable getContentsToPaste(Editor editor, DataContext dataContext) {
            Clipboard clip = editor.getComponent().getToolkit().getSystemSelection();
            if (clip == null) {
                return null;
            }

            try {
                return clip.getContents(null);
            }
            catch (Exception e) {
                LOG.info(e);
                return null;
            }
        }
    }
}
