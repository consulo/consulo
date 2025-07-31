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
package consulo.ide.impl.idea.lang.customFolding;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.ide.localize.IdeLocalize;
import consulo.language.Language;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.folding.CustomFoldingBuilder;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.editor.folding.LanguageFolding;
import consulo.language.editor.internal.CompositeFoldingBuilder;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Rustam Vishnyakov
 */
@ActionImpl(id = "GotoCustomRegion")
public class GotoCustomRegionAction extends AnAction implements DumbAware, PopupAction {
    public GotoCustomRegionAction() {
        super(IdeLocalize.gotoCustomRegionMenuItem());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        Editor editor = e.getRequiredData(Editor.KEY);
        if (Boolean.TRUE.equals(e.getData(PlatformDataKeys.IS_MODAL_CONTEXT))) {
            return;
        }
        DumbService dumbService = DumbService.getInstance(project);
        if (dumbService.isDumb()) {
            dumbService.showDumbModeNotification(IdeLocalize.gotoCustomRegionMessageDumbMode());
            return;
        }
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(IdeLocalize.gotoCustomRegionCommand())
            .run(() -> {
                Collection<FoldingDescriptor> foldingDescriptors = getCustomFoldingDescriptors(editor, project);
                if (foldingDescriptors.size() > 0) {
                    CustomFoldingRegionsPopup regionsPopup = new CustomFoldingRegionsPopup(foldingDescriptors, editor, project);
                    regionsPopup.show();
                }
                else {
                    notifyCustomRegionsUnavailable(editor, project);
                }
            });
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(Editor.KEY) && e.hasData(Project.KEY));
    }

    @Nonnull
    @RequiredReadAction
    private static Collection<FoldingDescriptor> getCustomFoldingDescriptors(@Nonnull Editor editor, @Nonnull Project project) {
        Set<FoldingDescriptor> foldingDescriptors = new HashSet<>();
        Document document = editor.getDocument();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        PsiFile file = documentManager != null ? documentManager.getPsiFile(document) : null;
        if (file != null) {
            FileViewProvider viewProvider = file.getViewProvider();
            for (Language language : viewProvider.getLanguages()) {
                PsiFile psi = viewProvider.getPsi(language);
                FoldingBuilder foldingBuilder = FoldingBuilder.forLanguageComposite(language);
                if (psi != null) {
                    for (FoldingDescriptor descriptor : LanguageFolding.buildFoldingDescriptors(foldingBuilder, psi, document, false)) {
                        CustomFoldingBuilder customFoldingBuilder = getCustomFoldingBuilder(foldingBuilder, descriptor);
                        if (customFoldingBuilder != null && customFoldingBuilder.isCustomRegionStart(descriptor.getElement())) {
                            foldingDescriptors.add(descriptor);
                        }
                    }
                }
            }
        }
        return foldingDescriptors;
    }

    @Nullable
    private static CustomFoldingBuilder getCustomFoldingBuilder(FoldingBuilder builder, FoldingDescriptor descriptor) {
        if (builder instanceof CustomFoldingBuilder customFoldingBuilder) {
            return customFoldingBuilder;
        }
        FoldingBuilder originalBuilder = descriptor.getElement().getUserData(CompositeFoldingBuilder.FOLDING_BUILDER);
        if (originalBuilder instanceof CustomFoldingBuilder customFoldingBuilder) {
            return customFoldingBuilder;
        }
        return null;
    }

    private static void notifyCustomRegionsUnavailable(@Nonnull Editor editor, @Nonnull Project project) {
        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        Balloon balloon = popupFactory.createHtmlTextBalloonBuilder(
                IdeLocalize.gotoCustomRegionMessageUnavailable().get(),
                NotificationType.INFO,
                null
            )
            .setFadeoutTime(2000)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .createBalloon();
        Disposer.register(project, balloon);
        balloon.show(EditorPopupHelper.getInstance().guessBestPopupLocation(editor), Balloon.Position.below);
    }
}
