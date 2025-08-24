// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.lookup.impl.actions;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.completion.CodeCompletionFeatures;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.language.editor.impl.internal.template.LiveTemplateCompletionContributor;
import consulo.ide.impl.idea.codeInsight.template.impl.editorActions.ExpandLiveTemplateCustomAction;
import consulo.ide.impl.idea.openapi.editor.actionSystem.LatencyAwareEditorAction;
import consulo.ide.impl.idea.util.SlowOperations;
import consulo.language.editor.completion.CompletionProcess;
import consulo.language.editor.completion.CompletionService;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupFocusDegree;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.template.LiveTemplateLookupElement;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.TemplateSettings;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public abstract class ChooseItemAction extends EditorAction implements HintManagerImpl.ActionToIgnore, LatencyAwareEditorAction {
    public ChooseItemAction(Handler handler) {
        super(handler);
    }

    public static class Handler extends EditorActionHandler {
        final boolean focusedOnly;
        final char finishingChar;

        public Handler(boolean focusedOnly, char finishingChar) {
            this.focusedOnly = focusedOnly;
            this.finishingChar = finishingChar;
        }

        @Override
        public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            LookupEx lookup = LookupManager.getActiveLookup(editor);
            assert lookup != null;

            if ((finishingChar == Lookup.NORMAL_SELECT_CHAR || finishingChar == Lookup.REPLACE_SELECT_CHAR)
                && hasTemplatePrefix(lookup, finishingChar)) {
                lookup.hideLookup(true);

                ExpandLiveTemplateCustomAction.createExpandTemplateHandler(finishingChar).execute(editor, null, dataContext);

                return;
            }

            if (finishingChar == Lookup.NORMAL_SELECT_CHAR) {
                if (!lookup.isFocused()) {
                    FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_CONTROL_ENTER);
                }
            }
            else if (finishingChar == Lookup.COMPLETE_STATEMENT_SELECT_CHAR) {
                FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_SMART_ENTER);
            }
            else if (finishingChar == Lookup.REPLACE_SELECT_CHAR) {
                FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_REPLACE);
            }
            else if (finishingChar == '.') {
                FeatureUsageTracker.getInstance().triggerFeatureUsed(CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_CONTROL_DOT);
            }

            SlowOperations.allowSlowOperations(() -> lookup.finishLookup(finishingChar));
        }

        @Override
        public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
            LookupEx lookup = LookupManager.getActiveLookup(editor);
            if (lookup == null) {
                return false;
            }
            if (!lookup.isAvailableToUser()) {
                return false;
            }
            if (lookup.getCurrentItemOrEmpty() == null) {
                return false;
            }
            if (focusedOnly && lookup.getLookupFocusDegree() == LookupFocusDegree.UNFOCUSED) {
                return false;
            }
            //noinspection SimplifiableIfStatement
            if (finishingChar == Lookup.REPLACE_SELECT_CHAR) {
                return !lookup.getItems().isEmpty();
            }

            return true;
        }
    }

    public static boolean hasTemplatePrefix(LookupEx lookup, char shortcutChar) {
        lookup.refreshUi(false, false); // to bring the list model up to date

        CompletionProcess completion = CompletionService.getCompletionService().getCurrentCompletion();
        if (completion == null || !completion.isAutopopupCompletion()) {
            return false;
        }

        if (lookup.isSelectionTouched()) {
            return false;
        }

        PsiFile file = lookup.getPsiFile();
        if (file == null) {
            return false;
        }

        Editor editor = lookup.getEditor();
        int offset = editor.getCaretModel().getOffset();
        PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());

        LiveTemplateLookupElement liveTemplateLookup = ContainerUtil.findInstance(lookup.getItems(), LiveTemplateLookupElement.class);
        if (liveTemplateLookup == null || !liveTemplateLookup.sudden) {
            // Lookup doesn't contain sudden live templates. It means that
            // - there are no live template with given key:
            //    in this case we should find live template with appropriate prefix (custom live templates doesn't participate in this action).
            // - completion provider worked too long:
            //    in this case we should check custom templates that provides completion lookup.
            if (LiveTemplateCompletionContributor.customTemplateAvailableAndHasCompletionItem(shortcutChar, editor, file, offset)) {
                return true;
            }

            List<? extends Template> templates = SlowOperations
                .allowSlowOperations(() -> TemplateManager.getInstance(file.getProject())
                    .listApplicableTemplateWithInsertingDummyIdentifier(TemplateActionContext.expanding(file, editor)));
            Template template = LiveTemplateCompletionContributor.findFullMatchedApplicableTemplate(editor, offset, templates);
            return template != null && shortcutChar == TemplateSettings.getInstance().getShortcutChar(template);
        }

        return liveTemplateLookup.getTemplateShortcut() == shortcutChar;
    }

    public static class Replacing extends ChooseItemAction {
        public Replacing() {
            super(new Handler(false, Lookup.REPLACE_SELECT_CHAR));
        }
    }

    public static class CompletingStatement extends ChooseItemAction {
        public CompletingStatement() {
            super(new Handler(true, Lookup.COMPLETE_STATEMENT_SELECT_CHAR));
        }
    }

    public static class ChooseWithDot extends ChooseItemAction {
        public ChooseWithDot() {
            super(new Handler(false, '.'));
        }
    }
}
