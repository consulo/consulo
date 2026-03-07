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
package consulo.fileEditor.impl.internal.text;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.*;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.language.Language;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.util.EditorHelper;
import consulo.language.editor.util.IdeView;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.LinkedHashSet;

import static consulo.ui.ex.action.AnActionEvent.injectedId;
import static consulo.util.collection.ContainerUtil.addIfNotNull;

/**
 * A {@link UiDataRule} that derives PSI-related data from the editor snapshot lazily under read access.
 * <p>
 * This separates EDT-safe data capture (done by {@link TextEditorComponent#uiDataSnapshot})
 * from PSI-requiring data resolution (done here under {@code tryRunReadAction}).
 * <p>
 * Replaces the old {@link TextEditorPsiDataProvider} which was called synchronously
 * on EDT via the {@code EditorDataProvider} pipeline.
 *
 * @author VISTALL
 * @since 2025-03-02
 */
@ExtensionImpl
public class TextEditorPsiDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(@Nonnull DataSink sink, @Nonnull DataSnapshot snapshot) {
        Editor editor = snapshot.get(Editor.KEY);
        if (editor == null || !(editor instanceof EditorEx)) {
            return;
        }

        VirtualFile file = snapshot.get(VirtualFile.KEY);
        if (file == null || !file.isValid()) {
            return;
        }

        Caret caret = snapshot.get(Caret.KEY);
        if (caret == null) {
            return;
        }

        Project project = editor.getProject();

        // HOST_EDITOR — no PSI needed, EDT-safe
        sink.set(EditorKeys.HOST_EDITOR, editor instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : editor);

        // PsiFile — lazy, needs read access
        sink.lazy(PsiFile.KEY, () -> getPsiFile(editor, file));

        // PsiElement — lazy, needs read access
        sink.lazy(PsiElement.KEY, () -> getPsiElementIn(editor, caret, file));

        // Language — lazy, needs read access
        sink.lazy(Language.KEY, () -> {
            PsiFile psiFile = getPsiFile(editor, file);
            if (psiFile == null) {
                return null;
            }
            return getLanguageAtCurrentPositionInEditor(caret, psiFile);
        });

        // IdeView — lazy, needs PSI
        sink.lazy(IdeView.KEY, () -> {
            if (project == null) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            PsiDirectory psiDirectory = psiFile != null ? psiFile.getParent() : null;
            if (psiDirectory != null && psiDirectory.isPhysical()) {
                return new IdeView() {
                    @Override
                    public void selectElement(PsiElement element) {
                        Editor ed = EditorHelper.openInEditor(element);
                        if (ed != null) {
                            ToolWindowManager.getInstance(element.getProject()).activateEditorComponent();
                        }
                    }

                    @Nonnull
                    @Override
                    public PsiDirectory[] getDirectories() {
                        return new PsiDirectory[]{psiDirectory};
                    }

                    @Override
                    public PsiDirectory getOrChooseDirectory() {
                        return psiDirectory;
                    }
                };
            }
            return null;
        });

        // CONTEXT_LANGUAGES — lazy, needs read access
        sink.lazy(LangDataKeys.CONTEXT_LANGUAGES, () -> computeLanguages(editor, caret, file));

        // Injected variants — all lazy, need PSI access

        // injected Editor
        sink.lazy(injectedId(Editor.KEY), () -> {
            if (project == null || PsiDocumentManager.getInstance(project).isUncommited(editor.getDocument())) {
                return editor;
            }
            return InjectedEditorManager.getInstance(project)
                .getEditorForInjectedLanguageNoCommit(editor, caret, getPsiFile(editor, file));
        });

        // injected Caret
        sink.lazy(injectedId(Caret.KEY), () -> {
            Editor injectedEditor = getInjectedEditor(editor, caret, file);
            return getInjectedCaret(injectedEditor, caret);
        });

        // injected PsiFile
        sink.lazy(injectedId(PsiFile.KEY), () -> {
            Editor injectedEditor = getInjectedEditor(editor, caret, file);
            if (project == null) {
                return null;
            }
            return PsiDocumentManager.getInstance(project).getPsiFile(injectedEditor.getDocument());
        });

        // injected PsiElement
        sink.lazy(injectedId(PsiElement.KEY), () -> {
            Editor injectedEditor = getInjectedEditor(editor, caret, file);
            Caret injectedCaret = getInjectedCaret(injectedEditor, caret);
            return getPsiElementIn(injectedEditor, injectedCaret, file);
        });

        // injected Language
        sink.lazy(injectedId(Language.KEY), () -> {
            Editor injectedEditor = getInjectedEditor(editor, caret, file);
            if (project == null) {
                return null;
            }
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(injectedEditor.getDocument());
            if (psiFile == null) {
                return null;
            }
            Caret injectedCaret = getInjectedCaret(injectedEditor, caret);
            return getLanguageAtCurrentPositionInEditor(injectedCaret, psiFile);
        });

        // injected VirtualFile
        sink.lazy(injectedId(VirtualFile.KEY), () -> {
            Editor injectedEditor = getInjectedEditor(editor, caret, file);
            if (project == null) {
                return null;
            }
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(injectedEditor.getDocument());
            if (psiFile == null) {
                return null;
            }
            return psiFile.getVirtualFile();
        });
    }

    @Nonnull
    private static Editor getInjectedEditor(@Nonnull Editor editor, @Nonnull Caret caret, @Nonnull VirtualFile file) {
        Project project = editor.getProject();
        if (project == null || PsiDocumentManager.getInstance(project).isUncommited(editor.getDocument())) {
            return editor;
        }
        Editor injected = InjectedEditorManager.getInstance(project)
            .getEditorForInjectedLanguageNoCommit(editor, caret, getPsiFile(editor, file));
        return injected != null ? injected : editor;
    }

    @Nonnull
    private static Caret getInjectedCaret(@Nonnull Editor editor, @Nonnull Caret hostCaret) {
        if (!(editor instanceof EditorWindow) || hostCaret instanceof CaretDelegate) {
            return hostCaret;
        }

        for (Caret c : editor.getCaretModel().getAllCarets()) {
            if (((CaretDelegate) c).getDelegate() == hostCaret) {
                return c;
            }
        }
        throw new IllegalArgumentException("Cannot find injected caret corresponding to " + hostCaret);
    }

    @Nullable
    private static PsiFile getPsiFile(@Nonnull Editor editor, @Nonnull VirtualFile file) {
        if (!file.isValid()) {
            return null;
        }
        Project project = editor.getProject();
        if (project == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return psiFile != null && psiFile.isValid() ? psiFile : null;
    }

    @Nullable
    private static PsiElement getPsiElementIn(@Nonnull Editor editor, @Nonnull Caret caret, @Nonnull VirtualFile file) {
        PsiFile psiFile = getPsiFile(editor, file);
        if (psiFile == null) {
            return null;
        }

        try {
            return TargetElementUtil.findTargetElement(editor, TargetElementUtil.getReferenceSearchFlags(), caret.getOffset());
        }
        catch (IndexNotReadyException e) {
            return null;
        }
    }

    private static Language getLanguageAtCurrentPositionInEditor(@Nonnull Caret caret, @Nonnull PsiFile psiFile) {
        int caretOffset = caret.getOffset();
        int mostProbablyCorrectLanguageOffset =
            caretOffset == caret.getSelectionStart() || caretOffset == caret.getSelectionEnd()
                ? caret.getSelectionStart() : caretOffset;
        if (caret.hasSelection()) {
            return getLanguageAtOffset(psiFile, mostProbablyCorrectLanguageOffset, caret.getSelectionEnd());
        }

        return PsiUtilCore.getLanguageAtOffset(psiFile, mostProbablyCorrectLanguageOffset);
    }

    private static Language getLanguageAtOffset(@Nonnull PsiFile psiFile, int mostProbablyCorrectLanguageOffset, int end) {
        PsiElement elt = psiFile.findElementAt(mostProbablyCorrectLanguageOffset);
        if (elt == null) {
            return psiFile.getLanguage();
        }
        if (elt instanceof PsiWhiteSpace) {
            int incremented = elt.getTextRange().getEndOffset() + 1;
            if (incremented <= end) {
                return getLanguageAtOffset(psiFile, incremented, end);
            }
        }
        return PsiUtilCore.findLanguageFromElement(elt);
    }

    @Nullable
    private static Language[] computeLanguages(@Nonnull Editor editor, @Nonnull Caret caret, @Nonnull VirtualFile file) {
        LinkedHashSet<Language> set = new LinkedHashSet<>(4);

        // Injected language
        Editor injectedEditor = getInjectedEditor(editor, caret, file);
        Project project = editor.getProject();
        if (project != null) {
            PsiFile injectedPsiFile = PsiDocumentManager.getInstance(project).getPsiFile(injectedEditor.getDocument());
            if (injectedPsiFile != null) {
                Caret injectedCaret = getInjectedCaret(injectedEditor, caret);
                addIfNotNull(set, getLanguageAtCurrentPositionInEditor(injectedCaret, injectedPsiFile));
            }
        }

        // Host language
        PsiFile psiFile = getPsiFile(editor, file);
        if (psiFile != null) {
            addIfNotNull(set, getLanguageAtCurrentPositionInEditor(caret, psiFile));
            addIfNotNull(set, psiFile.getViewProvider().getBaseLanguage());
        }

        return set.isEmpty() ? null : set.toArray(new Language[0]);
    }
}
