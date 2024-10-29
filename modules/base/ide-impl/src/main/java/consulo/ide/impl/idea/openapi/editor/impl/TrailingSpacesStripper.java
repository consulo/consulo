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
package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.application.Application;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.OverrideEditorFileKeys;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.DocumentRunnable;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.document.impl.DocumentImpl;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.util.collection.ArrayUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.document.DocumentWindow;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.util.lang.ShutDownTracker;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public final class TrailingSpacesStripper implements FileDocumentManagerListener {
    private static final Key<Boolean> DISABLE_FOR_FILE_KEY = Key.create("DISABLE_TRAILING_SPACE_STRIPPER_FOR_FILE_KEY");

    private final Set<Document> myDocumentsToStripLater = new HashSet<>();

    @Override
    @RequiredUIAccess
    public void beforeAllDocumentsSaving() {
        Set<Document> documentsToStrip = new HashSet<>(myDocumentsToStripLater);
        myDocumentsToStripLater.clear();
        for (Document document : documentsToStrip) {
            strip(document);
        }
    }

    @Override
    @RequiredUIAccess
    public void beforeDocumentSaving(@Nonnull Document document) {
        strip(document);
    }

    @RequiredUIAccess
    private void strip(@Nonnull final Document document) {
        if (!document.isWritable()) {
            return;
        }
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        VirtualFile file = fileDocumentManager.getFile(document);
        if (file == null || !file.isValid() || Boolean.TRUE.equals(DISABLE_FOR_FILE_KEY.get(file))) {
            return;
        }

        final EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();

        final String overrideStripTrailingSpacesData = file.getUserData(OverrideEditorFileKeys.OVERRIDE_STRIP_TRAILING_SPACES_KEY);
        final Boolean overrideEnsureNewlineData = file.getUserData(OverrideEditorFileKeys.OVERRIDE_ENSURE_NEWLINE_KEY);
        @EditorSettingsExternalizable.StripTrailingSpaces
        String stripTrailingSpaces =
            overrideStripTrailingSpacesData != null ? overrideStripTrailingSpacesData : settings.getStripTrailingSpaces();
        final boolean doStrip = !stripTrailingSpaces.equals(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE);
        final boolean ensureEOL = overrideEnsureNewlineData != null ? overrideEnsureNewlineData : settings.isEnsureNewLineAtEOF();

        if (doStrip) {
            final boolean inChangedLinesOnly = !stripTrailingSpaces.equals(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE);
            boolean success = strip(document, inChangedLinesOnly, settings.isKeepTrailingSpacesOnCaretLine());
            if (!success) {
                myDocumentsToStripLater.add(document);
            }
        }

        final int lines = document.getLineCount();
        if (ensureEOL && lines > 0) {
            final int start = document.getLineStartOffset(lines - 1);
            final int end = document.getLineEndOffset(lines - 1);
            if (start != end) {
                final CharSequence content = document.getCharsSequence();
                Application.get().runWriteAction(new DocumentRunnable(document, null) {
                    @Override
                    public void run() {
                        CommandProcessor.getInstance().runUndoTransparentAction(() -> {
                            if (CharArrayUtil.containsOnlyWhiteSpaces(content.subSequence(start, end)) && doStrip) {
                                document.deleteString(start, end);
                            }
                            else {
                                document.insertString(end, "\n");
                            }
                        });
                    }
                });
            }
        }
    }

    // clears line modification flags except lines which was not stripped because the caret was in the way
    public void clearLineModificationFlags(@Nonnull Document document) {
        if (document instanceof DocumentWindow) {
            document = ((DocumentWindow)document).getDelegate();
        }
        if (!(document instanceof DocumentImpl)) {
            return;
        }

        Editor activeEditor = getActiveEditor(document);

        // when virtual space enabled, we can strip whitespace anywhere
        boolean isVirtualSpaceEnabled = activeEditor == null || activeEditor.getSettings().isVirtualSpace();

        final EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
        if (settings == null) {
            return;
        }

        boolean enabled = !Boolean.TRUE.equals(DISABLE_FOR_FILE_KEY.get(FileDocumentManager.getInstance().getFile(document)));
        if (!enabled) {
            return;
        }
        String stripTrailingSpaces = settings.getStripTrailingSpaces();
        final boolean doStrip = !stripTrailingSpaces.equals(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE);
        final boolean inChangedLinesOnly = !stripTrailingSpaces.equals(EditorSettingsExternalizable.STRIP_TRAILING_SPACES_WHOLE);

        int[] caretLines;
        if (activeEditor != null && inChangedLinesOnly && doStrip && !isVirtualSpaceEnabled) {
            List<Caret> carets = activeEditor.getCaretModel().getAllCarets();
            caretLines = new int[carets.size()];
            for (int i = 0; i < carets.size(); i++) {
                Caret caret = carets.get(i);
                caretLines[i] = caret.getLogicalPosition().line;
            }
        }
        else {
            caretLines = ArrayUtil.EMPTY_INT_ARRAY;
        }
        ((DocumentImpl)document).clearLineModificationFlagsExcept(caretLines);
    }

    private static Editor getActiveEditor(@Nonnull Document document) {
        Component focusOwner = IdeFocusManager.getGlobalInstance().getFocusOwner();
        DataContext dataContext = DataManager.getInstance().getDataContext(focusOwner);
        boolean isDisposeInProgress = Application.get().isDisposeInProgress(); // ignore caret placing when exiting
        Editor activeEditor = isDisposeInProgress ? null : dataContext.getData(Editor.KEY);
        if (activeEditor != null && activeEditor.getDocument() != document) {
            activeEditor = null;
        }
        return activeEditor;
    }

    public static boolean strip(@Nonnull Document document, boolean inChangedLinesOnly, boolean skipCaretLines) {
        if (document instanceof DocumentWindow) {
            document = ((DocumentWindow)document).getDelegate();
        }
        if (!(document instanceof DocumentImpl)) {
            return true;
        }
        Editor activeEditor = getActiveEditor(document);

        final List<Caret> carets = activeEditor == null ? Collections.emptyList() : activeEditor.getCaretModel().getAllCarets();
        final List<VisualPosition> visualCarets = new ArrayList<>(carets.size());
        int[] caretOffsets = new int[carets.size()];
        for (int i = 0; i < carets.size(); i++) {
            Caret caret = carets.get(i);
            visualCarets.add(caret.getVisualPosition());
            caretOffsets[i] = caret.getOffset();
        }

        boolean markAsNeedsStrippingLater = ((DocumentImpl)document).stripTrailingSpaces(
            getProject(document, activeEditor),
            inChangedLinesOnly,
            skipCaretLines ? caretOffsets : null
        );

        if (activeEditor != null && !ShutDownTracker.isShutdownHookRunning()) {
            activeEditor.getCaretModel().runBatchCaretOperation(() -> {
                for (int i = 0; i < carets.size(); i++) {
                    Caret caret = carets.get(i);
                    if (caret.isValid()) {
                        caret.moveToVisualPosition(visualCarets.get(i));
                    }
                }
            });
        }

        return !markAsNeedsStrippingLater;
    }

    @Nullable
    private static Project getProject(@Nonnull Document document, @Nullable Editor editor) {
        if (editor != null) {
            return editor.getProject();
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null) {
            return ProjectUtil.guessProjectForFile(file);
        }
        return null;
    }

    public void documentDeleted(@Nonnull Document doc) {
        myDocumentsToStripLater.remove(doc);
    }

    @Override
    public void unsavedDocumentsDropped() {
        myDocumentsToStripLater.clear();
    }

    public static void setEnabled(@Nonnull VirtualFile file, boolean enabled) {
        DISABLE_FOR_FILE_KEY.set(file, enabled ? null : Boolean.TRUE);
    }

    public static boolean isEnabled(@Nonnull VirtualFile file) {
        return !Boolean.TRUE.equals(DISABLE_FOR_FILE_KEY.get(file));
    }
}
