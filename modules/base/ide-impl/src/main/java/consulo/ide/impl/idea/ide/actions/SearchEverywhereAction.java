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
package consulo.ide.impl.idea.ide.actions;

import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.ide.HelpTooltip;
import consulo.ide.impl.idea.ide.IdeEventQueue;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereManager;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereManagerImpl;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionButtonImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import consulo.application.dumb.DumbAware;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * @author Konstantin Bulenkov
 */
public class SearchEverywhereAction extends AnAction implements CustomComponentAction, DumbAware {
  public SearchEverywhereAction(ModifierKeyDoubleClickHandler modifierKeyDoubleClickHandler) {
    setEnabledInModalContext(false);

    modifierKeyDoubleClickHandler.registerAction(IdeActions.ACTION_SEARCH_EVERYWHERE, KeyEvent.VK_SHIFT, -1);
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(Presentation presentation, String place) {
    return new ActionButtonImpl(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {
      @Override
      protected void updateToolTipText() {
        String shortcutText = getShortcut();

        if (Registry.is("ide.helptooltip.enabled")) {
          HelpTooltip.dispose(this);

          new HelpTooltip().setTitle(myPresentation.getText()).setShortcut(shortcutText).setDescription("Searches for:<br/> - Classes<br/> - Files<br/> - Tool Windows<br/> - Actions<br/> - Settings")
                  .installOn(this);
        }
        else {
          setToolTipText("<html><body>Search Everywhere<br/>Press <b>" + shortcutText + "</b> to access<br/> - Classes<br/> - Files<br/> - Tool Windows<br/> - Actions<br/> - Settings</body></html>");
        }
      }
    };
  }

  private static String getShortcut() {
    String shortcutText;
    final Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_SEARCH_EVERYWHERE);
    if (shortcuts.length == 0) {
      shortcutText = "Double " + (SystemInfo.isMac ? MacKeymapUtil.SHIFT : "Shift");
    }
    else {
      shortcutText = KeymapUtil.getShortcutsText(shortcuts);
    }
    return shortcutText;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    actionPerformed(e, null);
  }

  public void actionPerformed(AnActionEvent e, MouseEvent me) {
    if (Registry.is("new.search.everywhere") && e.getData(CommonDataKeys.PROJECT) != null) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_SEARCH_EVERYWHERE);

      SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(e.getData(CommonDataKeys.PROJECT));
      String searchProviderID = SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID;
      if (seManager.isShown()) {
        if (searchProviderID.equals(seManager.getSelectedContributorID())) {
          seManager.toggleEverywhereFilter();
        }
        else {
          seManager.setSelectedContributor(searchProviderID);
          //FeatureUsageData data = SearchEverywhereUsageTriggerCollector.createData(searchProviderID).addInputEvent(e);
          //SearchEverywhereUsageTriggerCollector.trigger(e.getProject(), SearchEverywhereUsageTriggerCollector.TAB_SWITCHED, data);
        }
        return;
      }

      //FeatureUsageData data = SearchEverywhereUsageTriggerCollector.createData(searchProviderID);
      //SearchEverywhereUsageTriggerCollector.trigger(e.getProject(), SearchEverywhereUsageTriggerCollector.DIALOG_OPEN, data);
      IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
      String text = GotoActionBase.getInitialTextForNavigation(e);
      seManager.show(searchProviderID, text, e);
      return;
    }
  }
}

