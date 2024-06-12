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
package consulo.ide.impl.idea.ide.ui;

import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.ide.impl.idea.ide.SearchTopHitProvider;
import consulo.ide.impl.idea.ide.ui.search.BooleanOptionDescription;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author Konstantin Bulenkov
 */
public abstract class OptionsTopHitProvider implements SearchTopHitProvider {
  protected static final BiFunction<LocalizeManager, String, String> HTML_STRIP = (localizeManager, s) -> StringUtil.stripHtml(s, false);

  @Nonnull
  public abstract Collection<BooleanOptionDescription> getOptions(@Nullable Project project);

  @Override
  public final void consumeTopHits(String pattern, Consumer<Object> collector, Project project) {
    if (!pattern.startsWith("#")) return;
    pattern = pattern.substring(1);
    final List<String> parts = StringUtil.split(pattern, " ");

    if (parts.size() == 0) return;

    String id = parts.get(0);
    if (getId().startsWith(id)) {
      pattern = pattern.substring(id.length()).trim().toLowerCase();
      final MinusculeMatcher matcher = NameUtil.buildMatcher("*" + pattern, NameUtil.MatchingCaseSensitivity.NONE);
      for (BooleanOptionDescription option : getOptions(project)) {
        if (matcher.matches(option.getOption())) {
          collector.accept(option);
        }
      }
    }
  }

  public abstract String getId();

  public boolean isEnabled(@Nullable Project project) {
    return true;
  }

  @Deprecated
  static String message(LocalizeValue message) {
    return StringUtil.stripHtml(message.get(), false);
  }

  /*
   * Marker interface for option provider containing only descriptors which are backed by toggle actions.
   * E.g. UiSettings.SHOW_STATUS_BAR is backed by View > Status Bar action.
   */
  @Deprecated
  public static interface CoveredByToggleActions { // for search everywhere only
  }
}
