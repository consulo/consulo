/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.project.ui.wm.WindowManager;
import consulo.ide.impl.dataContext.BaseDataManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedDataManagerImpl extends BaseDataManager {
  @Inject
  public UnifiedDataManagerImpl(Application application, Provider<WindowManager> windowManagerProvider) {
    super(application, windowManagerProvider);
  }

  @Nonnull
  @Override
  public DataContext getDataContext() {
    return new MyUIDataContext(this, null);
  }

  @Nonnull
  @Override
  public AsyncDataContext createAsyncDataContext(@Nonnull DataContext dataContext) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataContext getDataContextTest(Component component) {
    return getDataContext();
  }
}
