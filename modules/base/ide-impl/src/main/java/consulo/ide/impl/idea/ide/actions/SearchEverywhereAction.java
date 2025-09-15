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
import consulo.application.dumb.DumbAware;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.searchEverywhere.SearchEverywhereManager;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereManagerImpl;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.internal.CustomTooltipBuilder;
import consulo.ui.ex.internal.KeyMapSetting;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.awt.event.KeyEvent;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = IdeActions.ACTION_SEARCH_EVERYWHERE)
public class SearchEverywhereAction extends AnAction implements DumbAware {
    private final KeyMapSetting myKeyMapSetting;

    @Inject
    public SearchEverywhereAction(KeyMapSetting keyMapSetting,
                                  ModifierKeyDoubleClickHandler modifierKeyDoubleClickHandler) {
        super(ActionLocalize.actionSearcheverywhereText(), LocalizeValue.empty(), PlatformIconGroup.actionsFind());
        myKeyMapSetting = keyMapSetting;
        setEnabledInModalContext(false);

        modifierKeyDoubleClickHandler.registerAction(IdeActions.ACTION_SEARCH_EVERYWHERE, KeyEvent.VK_SHIFT, -1);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().putClientProperty(
            CustomTooltipBuilder.KEY,
            (tooltip, presentation) -> {
                String shortcutText = getShortcut();

                tooltip.setTitle(presentation.getText())
                    .setShortcut(shortcutText)
                    .setDescription("Searches for:<br/> - Classes<br/> - Files<br/> - Tool Windows<br/> - Actions<br/> - Settings");
            }
        );
    }

    private static String getShortcut() {
        String shortcutText;
        Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(IdeActions.ACTION_SEARCH_EVERYWHERE);
        if (shortcuts.length == 0) {
            shortcutText = "Double " + (Platform.current().os().isMac() ? MacKeymapUtil.SHIFT : "Shift");
        }
        else {
            shortcutText = KeymapUtil.getShortcutsText(shortcuts);
        }
        return shortcutText;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (!myKeyMapSetting.isEnabledDoublePressShortcuts()
            && e.getInputEvent() instanceof KeyEvent keyEvent
            && keyEvent.getKeyCode() == KeyEvent.VK_SHIFT) {
            return;
        }

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

