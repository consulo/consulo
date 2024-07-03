/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.dashboard.RunDashboardManager;
import consulo.execution.impl.internal.configuration.ConfigurationTypeSelector;
import consulo.execution.impl.internal.service.action.AddServiceActionGroup;
import consulo.execution.localize.ExecutionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 13.05.2024
 */
@ActionImpl(id = "RunDashboard.AddType", parents = @ActionParentRef(@ActionRef(type = AddServiceActionGroup.class)))
public class AddRunConfigurationTypeAction extends DumbAwareAction {
  private static final Comparator<ConfigurationType> IGNORE_CASE_DISPLAY_NAME_COMPARATOR =
    (o1, o2) -> o1.getDisplayName().compareIgnoreCase(o2.getDisplayName());

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }
    RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(project);
    Set<String> addedTypes = runDashboardManager.getTypes();
    showAddPopup(project, addedTypes, newTypes -> {
      Set<String> updatedTypes = new HashSet<>(addedTypes);
      for (ConfigurationType type : newTypes) {
        updatedTypes.add(type.getId());
      }
      runDashboardManager.setTypes(updatedTypes);
    }, popup -> popup.showInBestPositionFor(e.getDataContext()), true);
  }

  private void showAddPopup(Project project, Set<String> addedTypes,
                            Consumer<List<ConfigurationType>> onAddCallback,
                            Consumer<JBPopup> popupOpener,
                            boolean showApplicableTypesOnly) {
    List<ConfigurationType> allTypes =
      ContainerUtil.filter(project.getApplication().getExtensionList(ConfigurationType.class), it -> !addedTypes.contains(it.getId()));

    List<ConfigurationType> configurationTypes = new ArrayList<>(ConfigurationTypeSelector.getTypesToShow(project,
                                                                                                          showApplicableTypesOnly && !project.isDefault(),
                                                                                                          allTypes));

    configurationTypes.sort(IGNORE_CASE_DISPLAY_NAME_COMPARATOR);
    var hiddenCount = allTypes.size() - configurationTypes.size();
    List<Object> popupList = new ArrayList<>(configurationTypes);
    if (hiddenCount > 0) {
      popupList.add(ExecutionLocalize.showIrrelevantConfigurationsActionName(hiddenCount).get());
    }

    var builder = JBPopupFactory.getInstance().createPopupChooserBuilder(popupList)
      .setTitle(ExecutionLocalize.runDashboardConfigurableAddConfigurationType().get())
      .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
      .setRenderer(new ColoredListCellRenderer() {
        @Override
        protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
          if (value instanceof ConfigurationType configurationType) {
            setIcon(configurationType.getIcon());
            append(configurationType.getDisplayName().get());
          }
          else {
            append(String.valueOf(value));
          }
        }
      })
      .setMovable(true)
      .setResizable(true)
      .setNamerForFiltering(o -> o instanceof ConfigurationType type ? type.getDisplayName().get() : null)
      .setAdText(ExecutionLocalize.runDashboardConfigurableTypesPanelHint().get())
      .setItemsChosenCallback(selectedValues -> {
        var value = ContainerUtil.getOnlyItem(selectedValues);
        if (value instanceof String) {
          showAddPopup(project, addedTypes, onAddCallback, popupOpener, false);
          return;
        }

        onAddCallback.accept(ContainerUtil.filterIsInstance(selectedValues, ConfigurationType.class));
      });

    popupOpener.accept(builder.createPopup());
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null);
  }
}
