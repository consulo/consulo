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
package consulo.ui.ex.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.TreeExpander;

import javax.swing.*;

/**
 * @author max
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CommonActionsManager {
  public static CommonActionsManager getInstance() {
    return Application.get().getInstance(CommonActionsManager.class);
  }

  public abstract AnAction createPrevOccurenceAction(OccurenceNavigator navigator);

  public abstract AnAction createNextOccurenceAction(OccurenceNavigator navigator);

  @Deprecated
  public abstract AnAction createExpandAllAction(TreeExpander expander);

  public abstract AnAction createExpandAllAction(TreeExpander expander, JComponent component);

  public abstract AnAction createExpandAllHeaderAction(JTree tree);

  @Deprecated
  public abstract AnAction createCollapseAllAction(TreeExpander expander);

  public abstract AnAction createCollapseAllAction(TreeExpander expander, JComponent component);

  public abstract AnAction createCollapseAllHeaderAction(JTree tree);

  public abstract AnAction createHelpAction(String helpId);

  /**
   * Installs autoscroll capability support to JTree passed. Toggle action returned.
   *
   * @param project
   * @param tree           should provide DataConstants.NAVIGATABLE for handler to work on
   * @param optionProvider get/set API to externalizable property.
   * @return toggle action to be inserted to appropriate toolbar
   */
  public abstract AnAction installAutoscrollToSourceHandler(ComponentManager project, JTree tree, AutoScrollToSourceOptionProvider optionProvider);

  public abstract AnAction createExportToTextFileAction(ExporterToTextFile exporter);
}
