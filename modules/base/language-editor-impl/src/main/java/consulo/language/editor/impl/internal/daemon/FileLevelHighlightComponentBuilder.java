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
package consulo.language.editor.impl.internal.daemon;

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.GutterMark;
import consulo.disposer.Disposable;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.intention.IntentionListStep;
import consulo.language.editor.impl.internal.intention.ShowIntentionActionsHandler;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.intention.EmptyIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.internal.intention.IntentionActionWithTextCaching;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects what is known about a file level highlight, then asks a {@link FileLevelHighlightComponentProvider}
 * to present it. Only the presentation crosses that interface: everything a highlight is described by is
 * gathered here, so the daemon needs no frontend of its own.
 *
 * @author VISTALL
 */
public class FileLevelHighlightComponentBuilder {
    private final Project myProject;
    private final PsiFile myPsiFile;
    private final Editor myEditor;

    private LocalizeValue myDescription = LocalizeValue.empty();
    private LocalizeValue myTooltip = LocalizeValue.empty();
    private HighlightSeverity mySeverity = HighlightSeverity.INFORMATION;
    private @Nullable GutterMark myGutterMark;
    private @Nullable List<Pair<IntentionActionDescriptor, TextRange>> myIntentions;

    public FileLevelHighlightComponentBuilder(Project project, PsiFile psiFile, Editor editor) {
        myProject = project;
        myPsiFile = psiFile;
        myEditor = editor;
    }

    public FileLevelHighlightComponentBuilder description(LocalizeValue description) {
        myDescription = description;
        return this;
    }

    public FileLevelHighlightComponentBuilder severity(HighlightSeverity severity) {
        mySeverity = severity;
        return this;
    }

    public FileLevelHighlightComponentBuilder gutterMark(@Nullable GutterMark gutterMark) {
        myGutterMark = gutterMark;
        return this;
    }

    public FileLevelHighlightComponentBuilder intentions(@Nullable List<Pair<IntentionActionDescriptor, TextRange>> intentions) {
        myIntentions = intentions;
        return this;
    }

    public FileLevelHighlightComponentBuilder tooltip(LocalizeValue tooltip) {
        myTooltip = tooltip;
        return this;
    }

    public Project getProject() {
        return myProject;
    }

    public PsiFile getPsiFile() {
        return myPsiFile;
    }

    public Editor getEditor() {
        return myEditor;
    }

    public LocalizeValue getDescription() {
        return myDescription;
    }

    public LocalizeValue getTooltip() {
        return myTooltip;
    }

    public HighlightSeverity getSeverity() {
        return mySeverity;
    }

    public @Nullable GutterMark getGutterMark() {
        return myGutterMark;
    }

    public @Nullable List<Pair<IntentionActionDescriptor, TextRange>> getIntentions() {
        return myIntentions;
    }

    public NotificationType getNotificationType() {
        SeverityRegistrar severityRegistrar = SeverityRegistrarImpl.getSeverityRegistrar(myProject);
        if (severityRegistrar.compare(mySeverity, HighlightSeverity.ERROR) >= 0) {
            return NotificationType.ERROR;
        }

        if (severityRegistrar.compare(mySeverity, HighlightSeverity.WARNING) >= 0) {
            return NotificationType.WARNING;
        }

        return NotificationType.INFO;
    }

    public List<IntentionAction> getIntentionActions() {
        if (myIntentions == null) {
            return List.of();
        }

        List<IntentionAction> actions = new ArrayList<>();
        for (Pair<IntentionActionDescriptor, TextRange> intention : myIntentions) {
            IntentionAction action = intention.getFirst().getAction();
            if (!(action instanceof EmptyIntentionAction)) {
                actions.add(action);
            }
        }
        return actions;
    }

    public boolean hasIntentions() {
        return myIntentions != null && !myIntentions.isEmpty();
    }

    @RequiredUIAccess
    public void invokeIntention(IntentionAction action, LocalizeValue text) {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        ShowIntentionActionsHandler.chooseActionAndInvoke(myPsiFile, myEditor, action, text.get());
    }

    @RequiredUIAccess
    public void showIntentionOptions(Component component, InputDetails inputDetails) {
        if (myIntentions == null || myIntentions.isEmpty()) {
            return;
        }

        CachedIntentions cachedIntentions = new CachedIntentions(myProject, myPsiFile, myEditor);
        IntentionListStep step = new IntentionListStep(null, myEditor, myPsiFile, myProject, cachedIntentions);
        IntentionActionDescriptor descriptor = myIntentions.get(0).getFirst();
        IntentionActionWithTextCaching actionWithTextCaching = cachedIntentions.wrapAction(descriptor, myPsiFile, myPsiFile, myEditor);
        if (step.hasSubstep(actionWithTextCaching)) {
            step = step.getSubStep(actionWithTextCaching, LocalizeValue.empty());
        }

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
        popup.showBy(component, inputDetails);
    }

    /**
     * @return the shown highlight, to be disposed to take it away again, or null when this highlight is not
     *         presented - it is then still recorded by the daemon, only not shown
     */
    @RequiredUIAccess
    public @Nullable Disposable show(FileEditor fileEditor) {
        return myProject.getInstance(FileLevelHighlightComponentProvider.class).createComponent(fileEditor, this);
    }
}
