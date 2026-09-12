// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.lookup.impl.actions;

import consulo.application.internal.SlowOperations;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.template.impl.editorActions.ExpandLiveTemplateCustomAction;
import consulo.language.editor.completion.CodeCompletionFeatures;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupFocusDegree;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.hint.HintManager;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

public abstract class ChooseItemAction extends EditorAction implements HintManager.ActionToIgnore {
    protected static class Handler extends EditorActionHandler {
        final boolean focusedOnly;
        final char finishingChar;

        public Handler(boolean focusedOnly, char finishingChar) {
            this.focusedOnly = focusedOnly;
            this.finishingChar = finishingChar;
        }

        @Override
        @RequiredUIAccess
        public void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            LookupEx lookup = LookupManager.getActiveLookup(editor);
            assert lookup != null;

            if ((finishingChar == Lookup.NORMAL_SELECT_CHAR || finishingChar == Lookup.REPLACE_SELECT_CHAR)
                && lookup.hasTemplatePrefix(finishingChar)) {
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
        public boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
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

    protected ChooseItemAction(LocalizeValue text, Handler handler) {
        super(text, handler);
    }
}
