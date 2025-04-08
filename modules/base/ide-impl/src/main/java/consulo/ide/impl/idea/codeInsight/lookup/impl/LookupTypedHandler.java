// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.ExtensionTypedActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionFeatures;
import consulo.ide.impl.idea.codeInsight.completion.CompletionPhase;
import consulo.ide.impl.idea.codeInsight.completion.CompletionProgressIndicator;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.ide.impl.idea.codeInsight.editorActions.AutoHardWrapHandler;
import consulo.ide.impl.idea.codeInsight.editorActions.TypedHandler;
import consulo.language.editor.completion.lookup.CharFilter;
import consulo.ide.impl.idea.codeInsight.lookup.impl.actions.ChooseItemAction;
import consulo.language.editor.impl.internal.template.TemplateSettingsImpl;
import consulo.ide.impl.idea.codeInsight.template.impl.editorActions.TypedActionHandlerBase;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "lookup")
public class LookupTypedHandler extends TypedActionHandlerBase implements ExtensionTypedActionHandler {
    private static final Logger LOG = Logger.getInstance(LookupTypedHandler.class);

    @Override
    public void execute(@Nonnull Editor originalEditor, char charTyped, @Nonnull DataContext dataContext) {
        final Project project = dataContext.getData(Project.KEY);
        PsiFile file = project == null ? null : PsiUtilBase.getPsiFileInEditor(originalEditor, project);

        if (file == null) {
            if (myOriginalHandler != null) {
                myOriginalHandler.execute(originalEditor, charTyped, dataContext);
            }
            return;
        }

        if (!EditorModificationUtil.checkModificationAllowed(originalEditor)) {
            return;
        }

        CompletionPhase oldPhase = CompletionServiceImpl.getCompletionPhase();
        if (oldPhase instanceof CompletionPhase.CommittingDocuments && oldPhase.indicator != null) {
            oldPhase.indicator.scheduleRestart();
        }

        Editor editor = TypedHandler.injectedEditorIfCharTypedIsSignificant(charTyped, originalEditor, file);
        if (editor != originalEditor) {
            file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        }

        if (originalEditor.isInsertMode() && beforeCharTyped(charTyped, project, originalEditor, editor, file)) {
            return;
        }

        if (myOriginalHandler != null) {
            myOriginalHandler.execute(originalEditor, charTyped, dataContext);
        }
    }

    private static boolean beforeCharTyped(
        final char charTyped,
        Project project,
        final Editor originalEditor,
        final Editor editor,
        PsiFile file
    ) {
        final LookupEx lookup = LookupManager.getActiveLookup(originalEditor);
        if (lookup == null) {
            return false;
        }

        if (charTyped == ' ' && ChooseItemAction.hasTemplatePrefix(lookup, TemplateSettingsImpl.SPACE_CHAR)) {
            return false;
        }

        final CharFilter.Result result = getLookupAction(charTyped, lookup);
        if (lookup.isLookupDisposed()) {
            return false;
        }

        if (result == CharFilter.Result.ADD_TO_PREFIX) {
            Document document = editor.getDocument();
            long modificationStamp = document.getModificationStamp();

            if (!lookup.performGuardedChange(() -> {
                lookup.fireBeforeAppendPrefix(charTyped);
                EditorModificationUtil.typeInStringAtCaretHonorMultipleCarets(originalEditor, String.valueOf(charTyped), true);
            })) {
                return true;
            }
            lookup.appendPrefix(charTyped);
            if (lookup.isStartCompletionWhenNothingMatches() && lookup.getItems().isEmpty()) {
                final CompletionProgressIndicator completion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
                if (completion != null) {
                    completion.scheduleRestart();
                }
                else {
                    AutoPopupController.getInstance(editor.getProject()).scheduleAutoPopup(editor);
                }
            }

            AutoHardWrapHandler.getInstance().wrapLineIfNecessary(
                originalEditor,
                DataManager.getInstance().getDataContext(originalEditor.getContentComponent()),
                modificationStamp
            );

            final CompletionProgressIndicator completion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
            if (completion != null) {
                completion.prefixUpdated();
            }
            return true;
        }

        if (result == CharFilter.Result.SELECT_ITEM_AND_FINISH_LOOKUP && lookup.isFocused()) {
            LookupElement item = lookup.getCurrentItem();
            if (item != null) {
                if (completeTillTypedCharOccurrence(charTyped, lookup, item)) {
                    return true;
                }

                FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_DOT_ETC);
                lookup.finishLookupInWritableFile(charTyped, item);
                return true;
            }
        }

        lookup.hide();
        TypedHandler.autoPopupCompletion(editor, charTyped, project, file);
        return false;
    }

    private static boolean completeTillTypedCharOccurrence(char charTyped, LookupEx lookup, LookupElement item) {
        PrefixMatcher matcher = lookup.itemMatcher(item);
        final String oldPrefix = matcher.getPrefix() + lookup.getAdditionalPrefix();
        PrefixMatcher expanded = matcher.cloneWithPrefix(oldPrefix + charTyped);
        if (expanded.prefixMatches(item)) {
            for (String s : item.getAllLookupStrings()) {
                if (matcher.prefixMatches(s)) {
                    int i = -1;
                    while (true) {
                        i = s.indexOf(charTyped, i + 1);
                        if (i < 0) {
                            break;
                        }
                        final String newPrefix = s.substring(0, i + 1);
                        if (expanded.prefixMatches(newPrefix)) {
                            lookup.replacePrefix(oldPrefix, newPrefix);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    static CharFilter.Result getLookupAction(final char charTyped, final LookupEx lookup) {
        CharFilter.Result filtersDecision = getFilterDecision(charTyped, lookup);
        if (filtersDecision != null) {
            return filtersDecision;
        }
        return CharFilter.Result.HIDE_LOOKUP;
    }

    @Nullable
    private static CharFilter.Result getFilterDecision(char charTyped, LookupEx lookup) {
        lookup.checkValid();
        LookupElement item = lookup.getCurrentItem();
        int prefixLength = item == null ? lookup.getAdditionalPrefix().length() : lookup.itemPattern(item).length();

        for (CharFilter extension : CharFilter.EP_NAME.getExtensionList()) {
            CharFilter.Result result = extension.acceptChar(charTyped, prefixLength, lookup);
            if (result != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(extension + " of " + extension.getClass() + " returned " + result);
                }
                return result;
            }
            if (lookup.isLookupDisposed()) {
                throw new AssertionError("Lookup disposed after " + extension);
            }
        }
        return null;
    }
}
