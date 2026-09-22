/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotationSession;
import consulo.language.editor.annotation.ExternalAnnotator;
import consulo.language.editor.annotation.ExternalLanguageAnnotators;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.UpdateHighlightersUtil;
import consulo.language.editor.impl.internal.highlight.AnnotationHolderImpl;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Update;

import java.util.*;

/**
 * @author ven
 */
public class ExternalToolPass extends TextEditorHighlightingPass {
    private final PsiFile myFile;
    private final int myStartOffset;
    private final int myEndOffset;
    private final AnnotationHolderImpl myAnnotationHolder;
    private final Editor myEditor;

    private volatile DocumentListener myDocumentListener;
    private volatile boolean myDocumentChanged;

    private final Map<ExternalAnnotator, MyData> myAnnotator2DataMap;

    private final ExternalToolPassFactory myExternalToolPassFactory;

    private static class MyData {
        final PsiFile myPsiRoot;
        final Object myCollectedInfo;
        volatile Object myAnnotationResult;

        private MyData(PsiFile psiRoot, Object collectedInfo) {
            myPsiRoot = psiRoot;
            myCollectedInfo = collectedInfo;
        }
    }

    public ExternalToolPass(ExternalToolPassFactory externalToolPassFactory, PsiFile file, Editor editor, int startOffset, int endOffset) {
        super(file.getProject(), editor.getDocument(), false);
        myEditor = editor;
        myFile = file;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myAnnotationHolder = new AnnotationHolderImpl(file.getLanguage(), new AnnotationSession(file), false);

        myAnnotator2DataMap = new HashMap<>();
        myExternalToolPassFactory = externalToolPassFactory;
    }

    @Override
    @RequiredReadAction
    public void doCollectInformation(ProgressIndicator progress) {
        myDocumentChanged = false;

        FileViewProvider viewProvider = myFile.getViewProvider();
        Set<Language> relevantLanguages = viewProvider.getLanguages();
        for (Language language : relevantLanguages) {
            PsiFile psiRoot = viewProvider.getPsi(language);
            if (!HighlightingLevelManager.getInstance(myProject).shouldInspect(psiRoot)) {
                continue;
            }
            List<ExternalAnnotator> externalAnnotators = ExternalLanguageAnnotators.allForFile(language, psiRoot);

            if (!externalAnnotators.isEmpty()) {
                DaemonCodeAnalyzerInternal daemonCodeAnalyzer = DaemonCodeAnalyzerInternal.getInstanceEx(myProject);
                boolean errorFound = daemonCodeAnalyzer.getFileStatusMap().wasErrorFound(myDocument);

                for (ExternalAnnotator externalAnnotator : externalAnnotators) {
                    Object collectedInfo = externalAnnotator.collectInformation(psiRoot, myEditor, errorFound);
                    if (collectedInfo != null) {
                        myAnnotator2DataMap.put(externalAnnotator, new MyData(psiRoot, collectedInfo));
                    }
                }
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void doApplyInformationToEditor() {
        DaemonCodeAnalyzerInternal daemonCodeAnalyzer = DaemonCodeAnalyzerInternal.getInstanceEx(myProject);
        daemonCodeAnalyzer.getFileStatusMap().markFileUpToDate(myDocument, getId());

        myDocumentListener = new DocumentListener() {
            @Override
            public void beforeDocumentChange(DocumentEvent event) {
            }

            @Override
            public void documentChanged(DocumentEvent event) {
                myDocumentChanged = true;
            }
        };
        myDocument.addDocumentListener(myDocumentListener);

        final Runnable r = () -> {
            if (myDocumentChanged || myProject.isDisposed()) {
                doFinish();
                return;
            }
            doAnnotate();

            Application.get().runReadAction(() -> {
                if (myDocumentChanged || myProject.isDisposed()) {
                    doFinish();
                    return;
                }
                collectHighlighters();

                Application.get().invokeLater(
                    () -> {
                        if (myDocumentChanged || myProject.isDisposed()) {
                            doFinish();
                            return;
                        }

                        myDocument.removeDocumentListener(myDocumentListener);
                        List<HighlightInfo> infos = getHighlights();
                        UpdateHighlightersUtil.setHighlightersToEditor(
                            myProject,
                            myDocument,
                            myStartOffset,
                            myEndOffset,
                            infos,
                            getColorsScheme(),
                            getId()
                        );
                    },
                    IdeaModalityState.stateForComponent(myEditor.getComponent())
                );
            });
        };

        if (Application.get().isUnitTestMode()) {
            r.run();
        }
        else {
            myExternalToolPassFactory.scheduleExternalActivity(new Update(myFile) {
                @Override
                public void run() {
                    r.run();
                }

                @Override
                public void setRejected() {
                    super.setRejected();
                    doFinish();
                }
            });
        }
    }

    private List<HighlightInfo> getHighlights() {
        List<HighlightInfo> infos = new ArrayList<>();
        for (Annotation annotation : myAnnotationHolder) {
            infos.add(HighlightInfoImpl.fromAnnotation(annotation));
        }
        return infos;
    }

    private void collectHighlighters() {
        for (ExternalAnnotator annotator : myAnnotator2DataMap.keySet()) {
            MyData data = myAnnotator2DataMap.get(annotator);
            if (data != null) {
                annotator.apply(data.myPsiRoot, data.myAnnotationResult, myAnnotationHolder);
            }
        }
    }

    private void doFinish() {
        myDocument.removeDocumentListener(myDocumentListener);
        Runnable r = () -> {
            if (!myProject.isDisposed()) {
                UpdateHighlightersUtil.setHighlightersToEditor(
                    myProject,
                    myDocument,
                    myStartOffset,
                    myEndOffset,
                    Collections.<HighlightInfo>emptyList(),
                    getColorsScheme(),
                    getId()
                );
            }
        };
        if (Application.get().isDispatchThread()) {
            r.run();
        }
        else {
            Application.get().invokeLater(r);
        }
    }

    private void doAnnotate() {
        for (ExternalAnnotator annotator : myAnnotator2DataMap.keySet()) {
            MyData data = myAnnotator2DataMap.get(annotator);
            if (data != null) {
                data.myAnnotationResult = annotator.doAnnotate(data.myCollectedInfo);
            }
        }
    }
}
