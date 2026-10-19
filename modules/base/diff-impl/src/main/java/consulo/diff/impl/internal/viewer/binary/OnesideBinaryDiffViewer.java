/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer.binary;

import consulo.application.progress.ProgressIndicator;
import consulo.diff.DiffContext;
import consulo.diff.impl.internal.viewer.BinaryEditorHolder;
import consulo.diff.impl.internal.viewer.DiffNotifications;
import consulo.diff.impl.internal.viewer.side.OnesideDiffViewer;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.Side;
import consulo.fileEditor.FileEditor;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

public class OnesideBinaryDiffViewer extends OnesideDiffViewer<BinaryEditorHolder> {
    public static final Logger LOG = Logger.getInstance(OnesideBinaryDiffViewer.class);

    @RequiredUIAccess
    public OnesideBinaryDiffViewer(DiffContext context, DiffRequest request) {
        super(context, (ContentDiffRequest)request, BinaryEditorHolder.BinaryEditorHolderFactory.INSTANCE);
    }

    //
    // Diff
    //

    @Override
    protected Runnable performRediff(ProgressIndicator indicator) {
        return () -> {
            clearDiffPresentation();
            myPanel.addNotification(getSide() == Side.LEFT ? DiffNotifications.createRemovedContent() : DiffNotifications.createInsertedContent());
        };
    }

    private void clearDiffPresentation() {
        myPanel.resetNotifications();
    }

    //
    // Getters
    //

    
    FileEditor getEditor() {
        return getEditorHolder().getEditor();
    }

    //
    // Misc
    //

    public static boolean canShowRequest(DiffContext context, DiffRequest request) {
        return OnesideDiffViewer.canShowRequest(context, request, BinaryEditorHolder.BinaryEditorHolderFactory.INSTANCE);
    }
}
