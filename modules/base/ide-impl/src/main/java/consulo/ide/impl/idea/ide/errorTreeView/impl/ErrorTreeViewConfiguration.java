/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.errorTreeView.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;


@Singleton
@State(name = "ErrorTreeViewConfiguration", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ErrorTreeViewConfiguration implements PersistentStateComponent<ErrorTreeViewConfiguration> {
  public boolean IS_AUTOSCROLL_TO_SOURCE = false;
  public boolean SHOW_WARNINGS = true;
  public boolean SHOW_INFOS = true;

  public static ErrorTreeViewConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, ErrorTreeViewConfiguration.class);
  }

  public boolean isAutoscrollToSource() {
    return IS_AUTOSCROLL_TO_SOURCE;
  }

  public void setAutoscrollToSource(boolean autoscroll) {
    IS_AUTOSCROLL_TO_SOURCE = autoscroll;
  }

  @Override
  public ErrorTreeViewConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(final ErrorTreeViewConfiguration state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
