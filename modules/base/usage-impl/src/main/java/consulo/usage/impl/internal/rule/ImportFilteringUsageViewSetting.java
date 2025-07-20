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
package consulo.usage.impl.internal.rule;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@State(name = "ImportFilteringUsageViewSetting", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class ImportFilteringUsageViewSetting implements PersistentStateComponent<ImportFilteringUsageViewSetting> {
  public static ImportFilteringUsageViewSetting getInstance() {
    return Application.get().getService(ImportFilteringUsageViewSetting.class);
  }

  public boolean SHOW_IMPORTS = true;

  @Override
  public ImportFilteringUsageViewSetting getState() {
    return this;
  }

  @Override
  public void loadState(final ImportFilteringUsageViewSetting state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
