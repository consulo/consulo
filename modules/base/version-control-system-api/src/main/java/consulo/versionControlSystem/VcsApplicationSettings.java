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
package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

/**
 * We don't use roaming type PER_OS - path macros is enough ($USER_HOME$/Dropbox for example)
 */
@Singleton
@State(name = "VcsApplicationSettings", storages = @Storage("vcs.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class VcsApplicationSettings implements PersistentStateComponent<VcsApplicationSettings> {
  public String PATCH_STORAGE_LOCATION = null;
  public boolean SHOW_WHITESPACES_IN_LST = false;
  public boolean SHOW_LST_GUTTER_MARKERS = true;
  public boolean SHOW_LST_WORD_DIFFERENCES = true;
  public boolean DETECT_PATCH_ON_THE_FLY = false;
  public boolean CREATE_CHANGELISTS_AUTOMATICALLY = false;

  public static VcsApplicationSettings getInstance() {
    return Application.get().getInstance(VcsApplicationSettings.class);
  }

  @Override
  public VcsApplicationSettings getState() {
    return this;
  }

  @Override
  public void loadState(VcsApplicationSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
