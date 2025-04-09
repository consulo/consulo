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
package consulo.ide.impl.idea.codeInsight.navigation.actions;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.find.actions.ShowUsagesAction;
import consulo.language.editor.ui.DefaultPsiElementCellRenderer;
import consulo.language.psi.util.EditSourceUtil;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.editor.inject.EditorWindow;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.dumb.DumbAware;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.TargetElementUtilExtender;
import consulo.language.editor.ui.navigation.GotoDeclarationHandlerEx;
import consulo.component.extension.ExtensionException;
import consulo.component.extension.Extensions;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.popup.JBPopup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class GotoDeclarationAction extends BaseCodeInsightAction implements CodeInsightActionHandler, DumbAware {
    private static final Logger LOG = Logger.getInstance(GotoDeclarationAction.class);

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return this;
    }

    @Override
    protected boolean isValidForLookup() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void invoke(@Nonnull final Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        DumbService.getInstance(project).setAlternativeResolveEnabled(true);
        try {
            int offset = editor.getCaretModel().getOffset();
            Pair<PsiElement[], GotoDeclarationHandler> elementsInfo = findAllTargetElementsInfo(project, editor, offset);
            PsiElement[] elements = elementsInfo.getFirst();
            FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.declaration");

            if (elements.length != 1) {
                if (elements.length == 0) {
                    PsiElement element = findElementToShowUsagesOf(editor, editor.getCaretModel().getOffset());
                    if (element != null) {
                        ShowUsagesAction showUsages = (ShowUsagesAction)ActionManager.getInstance().getAction(ShowUsagesAction.ID);
                        RelativePoint popupPosition = EditorPopupHelper.getInstance().guessBestPopupLocation(editor);
                        showUsages.startFindUsages(element, popupPosition, editor, ShowUsagesAction.getUsagesPageSize());
                        return;
                    }
                }
                chooseAmbiguousTarget(editor, offset, elements, calcElementRender(elementsInfo.getSecond(), elements));
                return;
            }

            PsiElement element = elements[0];
            PsiElement navElement = element.getNavigationElement();
            navElement = TargetElementUtil.getGotoDeclarationTarget(element, navElement);
            if (navElement != null) {
                gotoTargetElement(navElement);
            }
        }
        catch (IndexNotReadyException e) {
            DumbService.getInstance(project).showDumbModeNotification("Navigation is not available here during index update");
        }
        finally {
            DumbService.getInstance(project).setAlternativeResolveEnabled(false);
        }
    }

    @Nullable
    private static PsiElementListCellRenderer<PsiElement> calcElementRender(
        @Nullable GotoDeclarationHandler declarationHandler,
        @Nonnull PsiElement[] elements
    ) {
        if (declarationHandler instanceof GotoDeclarationHandlerEx) {
            return ((GotoDeclarationHandlerEx)declarationHandler).createRender(elements);
        }
        return null;
    }

    public static PsiNameIdentifierOwner findElementToShowUsagesOf(@Nonnull Editor editor, int offset) {
        PsiElement elementAt =
            TargetElementUtil.findTargetElement(editor, ContainerUtil.newHashSet(TargetElementUtilExtender.ELEMENT_NAME_ACCEPTED), offset);
        if (elementAt instanceof PsiNameIdentifierOwner) {
            return (PsiNameIdentifierOwner)elementAt;
        }
        return null;
    }

    private static void chooseAmbiguousTarget(
        final Editor editor,
        int offset,
        PsiElement[] elements,
        @Nullable PsiElementListCellRenderer<PsiElement> render
    ) {
        PsiElementProcessor<PsiElement> navigateProcessor = new PsiElementProcessor<PsiElement>() {
            @Override
            public boolean execute(@Nonnull final PsiElement element) {
                gotoTargetElement(element);
                return true;
            }
        };
        boolean found = chooseAmbiguousTarget(
            editor,
            offset,
            navigateProcessor,
            CodeInsightBundle.message("declaration.navigation.title"),
            elements,
            render
        );
        if (!found) {
            HintManager.getInstance().showErrorHint(editor, "Cannot find declaration to go to");
        }
    }

    private static void gotoTargetElement(PsiElement element) {
        Navigatable navigatable = element instanceof Navigatable ? (Navigatable)element : EditSourceUtil.getDescriptor(element);
        if (navigatable != null && navigatable.canNavigate()) {
            navigatable.navigate(true);
        }
    }

    public static boolean chooseAmbiguousTarget(
        @Nonnull Editor editor,
        int offset,
        @Nonnull PsiElementProcessor<PsiElement> processor,
        @Nonnull String titlePattern,
        @Nullable PsiElement[] elements
    ) {
        return chooseAmbiguousTarget(editor, offset, processor, titlePattern, elements, null);
    }

    // returns true if processor is run or is going to be run after showing popup
    public static boolean chooseAmbiguousTarget(
        @Nonnull Editor editor,
        int offset,
        @Nonnull PsiElementProcessor<PsiElement> processor,
        @Nonnull String titlePattern,
        @Nullable PsiElement[] elements,
        @Nullable PsiElementListCellRenderer<PsiElement> renderer
    ) {
        if (TargetElementUtil.inVirtualSpace(editor, offset)) {
            return false;
        }

        final PsiReference reference = TargetElementUtil.findReference(editor, offset);

        if (elements == null || elements.length == 0) {
            final Collection<PsiElement> candidates = suggestCandidates(reference);
            elements = PsiUtilCore.toPsiElementArray(candidates);
        }

        if (elements.length == 1) {
            PsiElement element = elements[0];
            LOG.assertTrue(element != null);
            processor.execute(element);
            return true;
        }
        if (elements.length > 1) {
            String title;

            if (reference == null) {
                title = titlePattern;
            }
            else {
                final TextRange range = reference.getRangeInElement();
                final String elementText = reference.getElement().getText();
                LOG.assertTrue(
                    range.getStartOffset() >= 0 && range.getEndOffset() <= elementText.length(),
                    Arrays.toString(elements) + ";" + reference
                );
                final String refText = range.substring(elementText);
                title = MessageFormat.format(titlePattern, refText);
            }

            if (renderer == null) {
                renderer = new DefaultPsiElementCellRenderer();
            }
            JBPopup popup = PopupNavigationUtil.getPsiElementPopup(elements, renderer, title, processor);

            editor.showPopupInBestPositionFor(popup);
            return true;
        }
        return false;
    }

    private static Collection<PsiElement> suggestCandidates(final PsiReference reference) {
        if (reference == null) {
            return Collections.emptyList();
        }
        return TargetElementUtil.getTargetCandidates(reference);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Nullable
    public static PsiElement findTargetElement(Project project, Editor editor, int offset) {
        final Pair<PsiElement[], GotoDeclarationHandler> pair = findAllTargetElementsInfo(project, editor, offset);
        PsiElement[] targets = pair.getFirst();
        return targets.length == 1 ? targets[0] : null;
    }

    @Nonnull
    public static Pair<PsiElement[], GotoDeclarationHandler> findAllTargetElementsInfo(Project project, Editor editor, int offset) {
        if (TargetElementUtil.inVirtualSpace(editor, offset)) {
            return Pair.create(PsiElement.EMPTY_ARRAY, null);
        }

        Pair<PsiElement[], GotoDeclarationHandler> pair = findTargetElementsNoVSWithHandler(project, editor, offset, true);
        return Pair.create(ObjectUtil.notNull(pair.getFirst(), PsiElement.EMPTY_ARRAY), pair.getSecond());
    }

    @Nullable
    public static PsiElement[] findTargetElementsNoVS(Project project, Editor editor, int offset, boolean lookupAccepted) {
        return findTargetElementsNoVSWithHandler(project, editor, offset, lookupAccepted).getFirst();
    }

    @Nonnull
    public static Pair<PsiElement[], GotoDeclarationHandler> findTargetElementsNoVSWithHandler(
        Project project,
        Editor editor,
        int offset,
        boolean lookupAccepted
    ) {
        final Document document = editor.getDocument();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (file == null) {
            return Pair.empty();
        }

        if (file instanceof PsiCompiledElement) {
            PsiElement mirror = ((PsiCompiledElement)file).getMirror();
            if (mirror instanceof PsiFile) {
                file = (PsiFile)mirror;
            }
        }

        PsiElement elementAt = file.findElementAt(TargetElementUtil.adjustOffset(file, document, offset));
        for (GotoDeclarationHandler handler : Extensions.getExtensions(GotoDeclarationHandler.EP_NAME)) {
            try {
                PsiElement[] result = handler.getGotoDeclarationTargets(elementAt, offset, editor);
                if (result != null && result.length > 0) {
                    for (PsiElement element : result) {
                        if (element == null) {
                            LOG.error("Null target element is returned by " + handler.getClass().getName());
                            return Pair.empty();
                        }
                    }
                    return Pair.create(result, handler);
                }
            }
            catch (AbstractMethodError e) {
                LOG.error(new ExtensionException(handler.getClass()));
            }
        }

        Set<String> flags = ContainerUtil.newHashSet(TargetElementUtil.getAllAccepted());
        flags.remove(TargetElementUtilExtender.ELEMENT_NAME_ACCEPTED);
        if (!lookupAccepted) {
            flags.remove(TargetElementUtilExtender.LOOKUP_ITEM_ACCEPTED);
        }
        PsiElement element = TargetElementUtil.findTargetElement(editor, flags, offset);
        if (element != null) {
            return Pair.create(new PsiElement[]{element}, null);
        }

        // if no references found in injected fragment, try outer document
        if (editor instanceof EditorWindow) {
            EditorWindow window = (EditorWindow)editor;
            return findTargetElementsNoVSWithHandler(
                project,
                window.getDelegate(),
                window.getDocument().injectedToHost(offset),
                lookupAccepted
            );
        }

        return Pair.empty();
    }

    @RequiredUIAccess
    @Override
    public void update(final AnActionEvent event) {
        InputEvent inputEvent = event.getInputEvent();
        if (inputEvent instanceof MouseEvent) {
            Component component = inputEvent.getComponent();
            if (component != null) {
                Point point = ((MouseEvent)inputEvent).getPoint();
                Component componentAt = SwingUtilities.getDeepestComponentAt(component, point.x, point.y);
                if (componentAt instanceof EditorGutterComponentEx) {
                    event.getPresentation().setEnabled(false);
                    return;
                }
            }
        }

        for (GotoDeclarationHandler handler : GotoDeclarationHandler.EP_NAME.getExtensionList()) {
            try {
                String text = handler.getActionText(event.getDataContext());
                if (text != null) {
                    Presentation presentation = event.getPresentation();
                    presentation.setText(text);
                    break;
                }
            }
            catch (AbstractMethodError e) {
                LOG.error(handler.toString(), e);
            }
        }

        super.update(event);
    }
}
