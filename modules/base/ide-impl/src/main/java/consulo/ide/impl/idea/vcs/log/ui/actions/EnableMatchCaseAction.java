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

import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.versionControlSystem.log.VcsLogProperties;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.data.VcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogInternalDataKeys;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

public class EnableMatchCaseAction extends BooleanPropertyToggleAction {
  @Nonnull
  private static final String MATCH_CASE = "Match Case";

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
        e.getPresentation().setText(MATCH_CASE);
      }
      else {
        Collection<VcsLogProvider> providers =
          ContainerUtil.newLinkedHashSet(ui.getDataPack().getLogProviders().values());
        List<VcsLogProvider> supported =
          ContainerUtil.filter(providers, p -> VcsLogProperties.get(p, VcsLogProperties.CASE_INSENSITIVE_REGEX));
        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(!supported.isEmpty());
        if (providers.size() == supported.size() || supported.isEmpty()) {
          e.getPresentation().setText(MATCH_CASE);
        }
        else {
          String supportedText = StringUtil.join(
            ContainerUtil.map(supported, p -> p.getSupportedVcs().getName().toLowerCase()),
            ", "
          );
          e.getPresentation().setText(MATCH_CASE + " (" + supportedText + " only)");
        }
      }
    }
  }
}
