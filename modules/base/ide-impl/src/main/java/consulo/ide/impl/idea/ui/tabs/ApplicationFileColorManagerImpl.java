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
package consulo.ide.impl.idea.ui.tabs;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.ui.view.tree.ApplicationFileColorManager;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 21-Jul-24
 */
@Singleton
@ServiceImpl
@State(name = "ApplicationFileColorManage", storages = @Storage("file.colors.xml"))
public class ApplicationFileColorManagerImpl implements ApplicationFileColorManager, PersistentStateComponent<ApplicationFileColorManagerImpl.State> {
  public static class State {
    public boolean enabled = true;

    public boolean enabledForTabs = true;

    public boolean enabledForProjectView = true;
  }

  private final State myState = new State();

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    XmlSerializerUtil.copyBean(state, myState);
  }

  @Override
  public boolean isEnabled() {
    return myState.enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    myState.enabled = enabled;
  }

  @Override
  public boolean isEnabledForTabs() {
    return myState.enabledForTabs;
  }

  @Override
  public void setEnabledForTabs(boolean enabled) {
    myState.enabledForTabs = enabled;
  }

  @Override
  public boolean isEnabledForProjectView() {
    return myState.enabledForProjectView;
  }

  @Override
  public void setEnabledForProjectView(boolean enabled) {
    myState.enabledForProjectView = enabled;
  }
}
