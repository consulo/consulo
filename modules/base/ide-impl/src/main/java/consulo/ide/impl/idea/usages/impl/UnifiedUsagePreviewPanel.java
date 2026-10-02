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
package consulo.ide.impl.idea.usages.impl;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.style.ComponentColors;
import consulo.usage.UsageInfo;
import consulo.usage.localize.UsageLocalize;
import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
public class UnifiedUsagePreviewPanel implements Disposable {
    private final Project myProject;
    private final boolean myIsEditor;
    private final UIAccess myUIAccess;

    private final DockLayout myLayout;
    private final Label myEmptyLabel;

    private Component myCenterComponent;

    private @Nullable Editor myEditor;
    private @Nullable List<? extends UsageInfo> myCachedSelectedUsageInfos;
    private boolean myDisposed;

    @RequiredUIAccess
    public UnifiedUsagePreviewPanel(Project project) {
        myProject = project;
        myIsEditor = Registry.is("ide.find.as.popup.editable.code");
        myUIAccess = UIAccess.current();

        myEmptyLabel = Label.create(
            UsageLocalize.selectTheUsageToPreview(),
            LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build()
        );
        myEmptyLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);

        myLayout = DockLayout.create(Space.NONE);
        myLayout.center(myEmptyLabel);
        myCenterComponent = myEmptyLabel;
    }

    public Component getComponent() {
        return myLayout;
    }

    public @Nullable Editor getEditor() {
        return myEditor;
    }

    @RequiredUIAccess
    public void updateLayout(@Nullable List<? extends UsageInfo> infos) {
        if (myDisposed) {
            return;
        }

        LocalizeValue cannotPreviewMessage = cannotPreviewMessage(infos);
        if (cannotPreviewMessage.isNotEmpty() || infos == null) {
            myEmptyLabel.setText(cannotPreviewMessage);
            setCenter(myEmptyLabel);

            releaseEditor();
            return;
        }

        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
        if (documentManager.hasUncommitedDocuments()) {
            documentManager.performLaterWhenAllCommitted(() -> myUIAccess.give(() -> updateLayout(infos)));
            return;
        }

        resetEditor(infos);
    }

    @RequiredUIAccess
    private void resetEditor(List<? extends UsageInfo> infos) {
        PsiElement psiElement = infos.get(0).getElement();
        if (psiElement == null) {
            return;
        }

        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile == null) {
            return;
        }

        PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(myProject).getInjectionHost(psiFile);
        if (host != null) {
            psiFile = host.getContainingFile();
            if (psiFile == null) {
                return;
            }
        }

        Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
        if (document == null) {
            return;
        }

        Editor editor = myEditor;
        if (editor == null || document != editor.getDocument()) {
            Editor newEditor = createEditor(psiFile, document);
            setCenter(newEditor.getUIComponent());

            releaseEditor();

            myEditor = newEditor;
            editor = newEditor;
        }

        if (!Comparing.equal(infos, myCachedSelectedUsageInfos)) {
            UsagePreviewPanel.highlight(infos, editor, myProject, true, HighlighterLayer.ADDITIONAL_SYNTAX);
            myCachedSelectedUsageInfos = infos;
        }
    }

    @RequiredUIAccess
    private void setCenter(Component component) {
        if (myCenterComponent == component) {
            return;
        }

        myLayout.remove(myCenterComponent);
        myLayout.center(component);
        myCenterComponent = component;
    }

    private Editor createEditor(PsiFile psiFile, Document document) {
        Editor editor = EditorFactory.getInstance().createEditor(document, myProject, psiFile.getVirtualFile(), !myIsEditor, EditorKind.PREVIEW);

        EditorSettings settings = editor.getSettings();
        settings.setLineMarkerAreaShown(myIsEditor);
        settings.setFoldingOutlineShown(false);
        settings.setAdditionalColumnsCount(0);
        settings.setAdditionalLinesCount(0);
        settings.setAnimatedScrolling(false);
        settings.setAutoCodeFoldingEnabled(false);
        return editor;
    }

    private void releaseEditor() {
        Editor editor = myEditor;
        if (editor != null) {
            EditorFactory.getInstance().releaseEditor(editor);
            myEditor = null;
            myCachedSelectedUsageInfos = null;
        }
    }

    public static LocalizeValue cannotPreviewMessage(@Nullable List<? extends UsageInfo> infos) {
        if (infos == null || infos.isEmpty()) {
            return UsageLocalize.selectTheUsageToPreview();
        }

        PsiFile psiFile = null;
        for (UsageInfo info : infos) {
            PsiElement element = info.getElement();
            if (element == null) {
                continue;
            }

            PsiFile file = element.getContainingFile();
            if (psiFile == null) {
                psiFile = file;
            }
            else if (psiFile != file) {
                return UsageLocalize.severalOccurrencesSelected();
            }
        }
        return LocalizeValue.empty();
    }

    @Override
    public void dispose() {
        myDisposed = true;

        releaseEditor();
    }
}
