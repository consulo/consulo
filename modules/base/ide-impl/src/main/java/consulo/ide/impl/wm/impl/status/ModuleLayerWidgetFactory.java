/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;


/**
 * @author VISTALL
 * @since 2020-11-01
 */
@ExtensionImpl(id = "moduleLayerWidget", order = "after encodingWidget")
public class ModuleLayerWidgetFactory implements StatusBarWidgetFactory {
  
  
  @Override
  public String getDisplayName() {
    return "Module Layer";
  }

  @Override
  public boolean isAvailable(Project project) {
    return true;
  }

  
  @Override
  public StatusBarWidget createWidget(Project project) {
    return new ModuleLayerWidget(project, this);
  }

  @Override
  public boolean canBeEnabledOn(StatusBar statusBar) {
    return true;
  }
}
