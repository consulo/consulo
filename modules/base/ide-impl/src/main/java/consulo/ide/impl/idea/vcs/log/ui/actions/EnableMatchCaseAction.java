/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.data.VcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogInternalDataKeys;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.log.VcsLogProperties;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@ActionImpl(id = "Vcs.Log.MatchCaseAction")
public class EnableMatchCaseAction extends BooleanPropertyToggleAction {
    public EnableMatchCaseAction() {
        super(VersionControlSystemLogLocalize.actionMatchCaseText());

        getTemplatePresentation().setIcon(PlatformIconGroup.actionsMatchcase());
        getTemplatePresentation().setHoveredIcon(PlatformIconGroup.actionsMatchcasehovered());
        getTemplatePresentation().setSelectedIcon(PlatformIconGroup.actionsMatchcaseselected());
    }

    @Override
    protected VcsLogUiProperties.VcsLogUiProperty<Boolean> getProperty() {
        return MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);

        VcsLogUi ui = e.getData(VcsLogUi.KEY);
        VcsLogUiProperties properties = e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);
        if (ui != null && properties != null && properties.exists(MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE)) {
            boolean regexEnabled = properties.exists(MainVcsLogUiProperties.TEXT_FILTER_REGEX)
                && properties.get(MainVcsLogUiProperties.TEXT_FILTER_REGEX);
            if (!regexEnabled) {
                e.getPresentation().setTextValue(VersionControlSystemLogLocalize.actionMatchCaseText());
            }
            else {
                Collection<VcsLogProvider> providers = new LinkedHashSet<>(ui.getDataPack().getLogProviders().values());
                List<VcsLogProvider> supported =
                    ContainerUtil.filter(providers, p -> VcsLogProperties.get(p, VcsLogProperties.CASE_INSENSITIVE_REGEX));
                e.getPresentation().setVisible(true);
                e.getPresentation().setEnabled(!supported.isEmpty());
                if (providers.size() == supported.size() || supported.isEmpty()) {
                    e.getPresentation().setTextValue(VersionControlSystemLogLocalize.actionMatchCaseText());
                }
                else {
                    String supportedText = StringUtil.join(
                        ContainerUtil.map(supported, p -> p.getSupportedVcs().getName().toLowerCase()),
                        ", "
                    );
                    e.getPresentation().setTextValue(VersionControlSystemLogLocalize.actionMatchCaseOnlySupported(supportedText));
                }
            }
        }
    }
}
