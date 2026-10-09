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

import consulo.annotation.component.ActionImpl;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereManagerImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.searchEverywhere.SearchEverywhereManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ShortcutLocalizeHolder;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.ex.internal.CustomToolTipBuilder;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.internal.ModifierKeyDoubleClickHandler;
import consulo.ui.ex.keymap.util.KeymapUtil;
import jakarta.inject.Inject;

import java.awt.event.KeyEvent;


/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = IdeActions.ACTION_SEARCH_EVERYWHERE)
public class SearchEverywhereAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    @Inject
    public SearchEverywhereAction(ModifierKeyDoubleClickHandler modifierKeyDoubleClickHandler) {
        super(ActionLocalize.actionSearcheverywhereText(), LocalizeValue.empty(), PlatformIconGroup.actionsFind());
        setEnabledInModalContext(false);

        modifierKeyDoubleClickHandler.registerAction(IdeActions.ACTION_SEARCH_EVERYWHERE, Modifier.SHIFT, null);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().putClientProperty(
            CustomToolTipBuilder.KEY,
            (tooltip, presentation) -> {
                LocalizeValue shortcutText = getShortcut();

                tooltip.setTitle(presentation.getTextValue())
                    .setShortcut(shortcutText)
                    .setDescription(LocalizeValue.localizeTODO(
                        "Searches for:<br/> - Classes<br/> - Files<br/> - Tool Windows<br/> - Actions<br/> - Settings"
                    ));
            }
        );
    }

    private static LocalizeValue getShortcut() {
        LocalizeValue shortcutText;
        Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_SEARCH_EVERYWHERE);
        if (shortcuts.length == 0) {
            LocalizeValue keyText = ShortcutLocalizeHolder.getKeyText(KeyEvent.VK_SHIFT, ShortcutUtil.isUseUnicodeShortcuts());
            shortcutText = LocalizeValue.join(LocalizeValue.localizeTODO("Double "), keyText);
        }
        else {
            shortcutText = KeymapUtil.getShortcutsTextValue(shortcuts);
        }
        return shortcutText;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_SEARCH_EVERYWHERE);

        SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(project);
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
        IdeEventQueueProxy.getInstance().closeAllPopups(false);
        String text = GotoActionBase.getInitialTextForNavigation(e);
        seManager.show(searchProviderID, text, e);
    }
}

