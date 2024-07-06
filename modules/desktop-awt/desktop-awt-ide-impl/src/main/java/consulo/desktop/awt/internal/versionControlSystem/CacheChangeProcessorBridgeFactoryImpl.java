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
package consulo.desktop.awt.internal.versionControlSystem;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.openapi.vcs.CacheChangeProcessorBridge;
import consulo.ide.impl.idea.openapi.vcs.CacheChangeProcessorBridgeFactory;
import consulo.ide.impl.idea.openapi.vcs.CacheChangeProcessorImpl;
import consulo.project.Project;
import consulo.versionControlSystem.change.Change;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author VISTALL
 * @since 06-Jul-24
 */
@Singleton
@ServiceImpl
public class CacheChangeProcessorBridgeFactoryImpl implements CacheChangeProcessorBridgeFactory {
  private final Project myProject;

  @Inject
  public CacheChangeProcessorBridgeFactoryImpl(Project project) {
    myProject = project;
  }

  @Override
  public CacheChangeProcessorBridge create(CacheChangeProcessorImpl impl) {
    return new CacheChangeProcessor(myProject, impl.getPlace()) {
      {
        impl.init(myProject, this);
      }

      @Override
      public boolean isWindowFocused() {
        Boolean windowFocused = impl.isWindowFocused();
        if (windowFocused != null) {
          return windowFocused;
        }
        return super.isWindowFocused();
      }

      @Nonnull
      @Override
      protected List<Change> getSelectedChanges() {
        return impl.getSelectedChanges();
      }

      @Nonnull
      @Override
      protected List<Change> getAllChanges() {
        return impl.getAllChanges();
      }

      @Override
      protected void selectChange(@Nonnull Change change) {
        impl.selectChange(change);
      }

      @Override
      protected void onAfterNavigate() {
        impl.onAfterNavigate();
      }
    };
  }
}
