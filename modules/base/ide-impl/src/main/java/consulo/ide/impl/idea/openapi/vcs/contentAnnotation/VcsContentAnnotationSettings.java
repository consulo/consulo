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
package consulo.ide.impl.idea.openapi.vcs.contentAnnotation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import jakarta.inject.Singleton;

/**
 * @author Irina.Chernushina
 * @since 2011-08-03
 */
@Singleton
@State(name = "VcsContentAnnotationSettings", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsContentAnnotationSettings implements PersistentStateComponent<VcsContentAnnotationSettings.State> {
  public static final long ourMillisecondsInDay = 24 * 60 * 60 * 1000L;
  public static final int ourMaxDays = 31; // approx
  public static final long ourAbsoluteLimit = ourMillisecondsInDay * ourMaxDays;

  private State myState = new State();

  {
    myState.myLimit = ourAbsoluteLimit;
  }

  public static VcsContentAnnotationSettings getInstance(final Project project) {
    return ServiceManager.getService(project, VcsContentAnnotationSettings.class);
  }

  public static class State {
    public boolean myShow1 = false;
    public long myLimit;
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  public long getLimit() {
    return myState.myLimit;
  }

  public int getLimitDays() {
    return (int)(myState.myLimit / ourMillisecondsInDay);
  }

  public void setLimit(int limit) {
    myState.myLimit = ourMillisecondsInDay * limit;
  }

  public boolean isShow() {
    return myState.myShow1;
  }

  public void setShow(final boolean value) {
    myState.myShow1 = value;
  }
}
