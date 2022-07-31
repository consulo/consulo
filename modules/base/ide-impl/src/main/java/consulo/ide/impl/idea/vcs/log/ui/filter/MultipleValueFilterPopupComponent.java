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
package consulo.ide.impl.idea.vcs.log.ui.filter;

import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract class MultipleValueFilterPopupComponent<Filter extends VcsLogFilter> extends FilterPopupComponent<Filter> {

  private static final int MAX_FILTER_VALUE_LENGTH = 30;

  @Nonnull
  protected final MainVcsLogUiProperties myUiProperties;

  MultipleValueFilterPopupComponent(@Nonnull String filterName,
                                    @Nonnull MainVcsLogUiProperties uiProperties,
                                    @Nonnull FilterModel<Filter> filterModel) {
    super(filterName, filterModel);
    myUiProperties = uiProperties;
  }

  @Nonnull
  protected abstract List<List<String>> getRecentValuesFromSettings();

  protected abstract void rememberValuesInSettings(@Nonnull Collection<String> values);

  @Nonnull
  protected abstract List<String> getAllValues();

  @Nonnull
  protected ActionGroup createRecentItemsActionGroup() {
    DefaultActionGroup group = new DefaultActionGroup();
    List<List<String>> recentlyFiltered = getRecentValuesFromSettings();
    if (!recentlyFiltered.isEmpty()) {
      group.addSeparator("Recent");
      for (List<String> recentGroup : recentlyFiltered) {
        if (!recentGroup.isEmpty()) {
          group.add(new PredefinedValueAction(recentGroup));
        }
      }
      group.addSeparator();
    }
    return group;
  }

  @Nonnull
  static String displayableText(@Nonnull Collection<String> values) {
    if (values.size() == 1) {
      return values.iterator().next();
    }
    return StringUtil.shortenTextWithEllipsis(StringUtil.join(values, "|"), MAX_FILTER_VALUE_LENGTH, 0, true);
  }

  @Nonnull
  static String tooltip(@Nonnull Collection<String> values) {
    return StringUtil.join(values, ", ");
  }

  @Nonnull
  protected AnAction createSelectMultipleValuesAction() {
    return new SelectMultipleValuesAction();
  }

  /**
   * Return true if the filter supports "negative" values, i.e. values like "-value" which means "match anything but 'value'".
   */
  protected boolean supportsNegativeValues() {
    return false;
  }

  protected class PredefinedValueAction extends DumbAwareAction {

    @Nonnull
    protected final List<String> myValues;

    public PredefinedValueAction(@Nonnull String value) {
      this(Collections.singletonList(value));
    }

    public PredefinedValueAction(@Nonnull List<String> values) {
      super(null, tooltip(values), null);
      getTemplatePresentation().setText(displayableText(values), false);
      myValues = values;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myFilterModel.setFilter(myFilterModel.createFilter(myValues));
      rememberValuesInSettings(myValues);
    }
  }

  private class SelectMultipleValuesAction extends DumbAwareAction {

    @Nonnull
    private final Collection<String> myVariants;

    SelectMultipleValuesAction() {
      super("Select...");
      myVariants = getAllValues();
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      Project project = e.getData(CommonDataKeys.PROJECT);
      if (project == null) {
        return;
      }

      Filter filter = myFilterModel.getFilter();
      List<String> values = filter == null
                            ? Collections.emptyList()
                            : myFilterModel.getFilterValues(filter);
      final MultilinePopupBuilder popupBuilder = new MultilinePopupBuilder(project, myVariants,
                                                                           getPopupText(values),
                                                                           supportsNegativeValues());
      JBPopup popup = popupBuilder.createPopup();
      popup.addListener(new JBPopupAdapter() {
        @Override
        public void onClosed(LightweightWindowEvent event) {
          if (event.isOk()) {
            List<String> selectedValues = popupBuilder.getSelectedValues();
            if (selectedValues.isEmpty()) {
              myFilterModel.setFilter(null);
            }
            else {
              myFilterModel.setFilter(myFilterModel.createFilter(selectedValues));
              rememberValuesInSettings(selectedValues);
            }
          }
        }
      });
      popup.showUnderneathOf(MultipleValueFilterPopupComponent.this);
    }

    @Nonnull
    private String getPopupText(@Nullable Collection<String> selectedValues) {
      return selectedValues == null || selectedValues.isEmpty() ? "" : StringUtil.join(selectedValues, "\n");
    }
  }
}
