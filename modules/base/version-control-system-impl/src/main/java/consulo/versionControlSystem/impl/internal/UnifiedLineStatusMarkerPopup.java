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
package consulo.versionControlSystem.impl.internal;

import consulo.codeEditor.Editor;
import consulo.diff.fragment.DiffFragment;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;


/**
 * @author VISTALL
 * @since 2026-08-11
 */
public class UnifiedLineStatusMarkerPopup extends LineStatusMarkerPopupBase {
    public UnifiedLineStatusMarkerPopup(LineStatusTracker tracker, Editor editor, VcsRange range) {
        super(tracker, editor, range);
    }

    @Override
    @RequiredUIAccess
    public void showHintAt(@Nullable InputDetails details) {
        if (!myTracker.isValid()) {
            return;
        }

        Disposable disposable = Disposable.newDisposable();

        List<DiffFragment> wordDiff = computeWordDiff();
        installMasterEditorHighlighters(wordDiff, disposable);

        ActionGroup group = buildActionGroup(details, disposable);

        ActionToolbar toolbar =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.FILEHISTORY_VIEW_TOOLBAR, group, true);
        toolbar.setTargetUIComponent(myEditor.getContentUIComponent());

        Component preview = createPreview(wordDiff, disposable);

        toolbar.updateActionsAsync().whenCompleteAsync(
            (actions, throwable) -> show(toolbar, preview, details, disposable),
            UIAccess.current()
        );
    }

    @RequiredUIAccess
    private void show(ActionToolbar toolbar, @Nullable Component preview, @Nullable InputDetails details, Disposable disposable) {
        LineStatusMarkerPopupPanel.show(myEditor, toolbar, preview, details, disposable);
    }

    @RequiredUIAccess
    private @Nullable Component createPreview(@Nullable List<DiffFragment> wordDiff, Disposable disposable) {
        if (myRange.getType() == VcsRange.INSERTED) {
            return null;
        }

        return LineStatusMarkerPopupPanel.createPreview(myTracker.getProject(),
            myEditor,
            myTracker.getVcsContent(myRange),
            getFileType(),
            getFileName(myTracker.getDocument()),
            wordDiff,
            disposable);
    }

    private static String getFileName(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return file == null ? "" : file.getName();
    }
}
