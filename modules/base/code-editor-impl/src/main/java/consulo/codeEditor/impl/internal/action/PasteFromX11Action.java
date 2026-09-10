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
package consulo.codeEditor.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import javax.swing.*;
import java.awt.*;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.codeEditor.impl.util.EditorImplUtil;
import org.jspecify.annotations.Nullable;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.concurrent.CompletableFuture;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * @author msk
 */
@ActionImpl(id = "EditorPasteFromX11")
public class PasteFromX11Action extends EditorAction {
    private static final Logger LOG = Logger.getInstance(PasteFromX11Action.class);

    public PasteFromX11Action() {
        super(CodeEditorLocalize.actionPasteFromX11Text(), new Handler());
    }

    @Override
    public void updateInUI(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Editor editor = e.getData(Editor.KEY);
        if (editor == null || !Platform.current().os().isUnix()) {
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
        /**
         * The x11 selection is an awt clipboard of its own and this action only exists where there is one, so the
         * transferable is carried through as the rich half next to the text every paste understands.
         */
        @Override
        protected CompletableFuture<@Nullable DataTransfer> getContentsToPaste(Editor editor, DataContext dataContext) {
            Clipboard clip = editor.getComponent().getToolkit().getSystemSelection();
            if (clip == null) {
                return CompletableFuture.completedFuture(null);
            }

            try {
                Transferable contents = clip.getContents(null);
                String text = (String)contents.getTransferData(DataFlavor.stringFlavor);

                return CompletableFuture.completedFuture(DataTransfer.builder()
                    .put(DataTransferType.TEXT, text)
                    .put(EditorImplUtil.TRANSFERABLE, contents)
                    .build());
            }
            catch (Exception e) {
                LOG.info(e);
                return CompletableFuture.completedFuture(null);
            }
        }
    }
}
