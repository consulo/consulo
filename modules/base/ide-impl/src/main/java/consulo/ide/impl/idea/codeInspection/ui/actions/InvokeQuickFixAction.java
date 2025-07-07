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

import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInspection.ex.InspectionRVContentProvider;
import consulo.ide.impl.idea.codeInspection.ex.QuickFixAction;
import consulo.ide.impl.idea.codeInspection.ui.InspectionResultsView;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionGroupUtil;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author anna
 * @since 2006-01-11
 */
public class InvokeQuickFixAction extends AnAction {
  private final InspectionResultsView myView;

  public InvokeQuickFixAction(final InspectionResultsView view) {
    super(
        InspectionLocalize.inspectionActionApplyQuickfix(),
        InspectionLocalize.inspectionActionApplyQuickfixDescription(),
        PlatformIconGroup.actionsIntentionbulb()
    );
    myView = view;
    registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS).getShortcutSet(),
                              myView.getTree());
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (!myView.isSingleToolInSelection()) {
      e.getPresentation().setEnabled(false);
      return;
    }

    //noinspection ConstantConditions
    @Nonnull InspectionToolWrapper toolWrapper = myView.getTree().getSelectedToolWrapper();
    final InspectionRVContentProvider provider = myView.getProvider();
    if (provider.isContentLoaded()) {
      final QuickFixAction[] quickFixes = provider.getQuickFixes(toolWrapper, myView.getTree());
      if (quickFixes == null || quickFixes.length == 0) {
        e.getPresentation().setEnabled(false);
        return;
      }
      e.getPresentation().setEnabled(!ActionGroupUtil.isGroupEmpty(getFixes(quickFixes), e));
    }
  }

  private static ActionGroup getFixes(final QuickFixAction[] quickFixes) {
    return new ActionGroup() {
      @Override
      @Nonnull
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
  public void actionPerformed(AnActionEvent e) {
    InspectionToolWrapper toolWrapper = myView.getTree().getSelectedToolWrapper();
    assert toolWrapper != null;
    final QuickFixAction[] quickFixes = myView.getProvider().getQuickFixes(toolWrapper, myView.getTree());
    if (quickFixes == null || quickFixes.length == 0) {
      Messages.showInfoMessage(myView, "There are no applicable quickfixes", "Nothing found to fix");
      return;
    }
    ActionGroup fixes = getFixes(quickFixes);
    DataContext dataContext = e.getDataContext();
    final ListPopup popup = JBPopupFactory.getInstance()
                                          .createActionGroupPopup(InspectionsBundle.message("inspection.tree.popup.title"),
                                                                  fixes,
                                                                  dataContext,
                                                                  JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                                                  false);
    InspectionResultsView.showPopup(e, popup);
  }
}
