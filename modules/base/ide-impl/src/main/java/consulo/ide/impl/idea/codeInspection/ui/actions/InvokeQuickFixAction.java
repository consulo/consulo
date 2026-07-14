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
package consulo.ide.impl.idea.codeInspection.ui.actions;

import consulo.application.ReadAction;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInspection.ex.InspectionRVContentProvider;
import consulo.ide.impl.idea.codeInspection.ex.QuickFixAction;
import consulo.ide.impl.idea.codeInspection.ui.InspectionResultsView;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionGroupUtil;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.coroutine.UIAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author anna
 * @since 2006-01-11
 */
public class InvokeQuickFixAction extends AnAction implements AnActionWithAsyncUpdate {
    private final InspectionResultsView myView;

    public InvokeQuickFixAction(InspectionResultsView view) {
        super(
            InspectionLocalize.inspectionActionApplyQuickfix(),
            InspectionLocalize.inspectionActionApplyQuickfixDescription(),
            PlatformIconGroup.actionsIntentionbulb()
        );
        myView = view;
        registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS).getShortcutSet(),
            myView.getTree()
        );
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        // the view/tree checks touch Swing, so run them on the UI thread; produce the fixes group to validate, or
        // finish early when the presentation is already decided (or the content is not loaded and must be left as is)
        return Coroutine.<Object, ActionGroup>first(UIAction.apply((input, continuation) -> {
                if (!myView.isSingleToolInSelection()) {
                    e.getPresentation().setEnabled(false);
                    continuation.finishEarly(null);
                    return null;
                }

                //noinspection ConstantConditions
                InspectionToolWrapper toolWrapper = myView.getTree().getSelectedToolWrapper();
                InspectionRVContentProvider provider = myView.getProvider();
                if (!provider.isContentLoaded()) {
                    continuation.finishEarly(null);
                    return null;
                }

                QuickFixAction[] quickFixes = ReadAction.compute(() -> provider.getQuickFixes(toolWrapper, myView.getTree()));
                if (quickFixes == null || quickFixes.length == 0) {
                    e.getPresentation().setEnabled(false);
                    continuation.finishEarly(null);
                    return null;
                }
                return getFixes(quickFixes);
            }))
            .then(CompletableFutureStep.<ActionGroup, Void>await(group ->
                ActionGroupUtil.isGroupEmptyAsync(group, e.getUpdateSession())
                    .thenAccept(empty -> e.getPresentation().setEnabled(!Boolean.TRUE.equals(empty)))));
    }

    private static ActionGroup getFixes(final QuickFixAction[] quickFixes) {
        return new ActionGroup() {
            @Override
            
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                List<QuickFixAction> children = new ArrayList<QuickFixAction>();
                for (QuickFixAction fix : quickFixes) {
                    if (fix != null) {
                        children.add(fix);
                    }
                }
                return children.toArray(new AnAction[children.size()]);
            }
        };
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        InspectionToolWrapper toolWrapper = myView.getTree().getSelectedToolWrapper();
        assert toolWrapper != null;
        QuickFixAction[] quickFixes = myView.getProvider().getQuickFixes(toolWrapper, myView.getTree());
        if (quickFixes == null || quickFixes.length == 0) {
            Messages.showInfoMessage(myView, "There are no applicable quickfixes", "Nothing found to fix");
            return;
        }
        ActionGroup fixes = getFixes(quickFixes);
        DataContext dataContext = e.getDataContext();
        ListPopup popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(
                InspectionLocalize.inspectionTreePopupTitle().get(),
                fixes,
                dataContext,
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
        InspectionResultsView.showPopup(e, popup);
    }
}
