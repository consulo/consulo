/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.language.psi;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.internal.AbstractProgressIndicatorExBase;
import consulo.application.internal.ProgressWrapper;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressIndicator;
import consulo.language.content.FileIndexFacade;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.psi.PsiModificationTracker;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 17-Feb-22
 */
@Singleton
@ServiceImpl
public class IdePsiManagerImpl extends PsiManagerImpl {
  @Inject
  public IdePsiManagerImpl(@Nonnull Application application,
                           @Nonnull Project project,
                           @Nonnull Provider<FileIndexFacade> fileIndexFacadeProvider,
                           @Nonnull PsiModificationTracker modificationTracker) {
    super(application, project, fileIndexFacadeProvider, modificationTracker);
  }

  public void dropResolveCacheRegularly(@Nonnull ProgressIndicator indicator) {
    indicator = ProgressWrapper.unwrap(indicator);
    if (indicator instanceof ProgressIndicatorEx) {
      ((ProgressIndicatorEx)indicator).addStateDelegate(new AbstractProgressIndicatorExBase() {
        private final AtomicLong lastClearedTimeStamp = new AtomicLong();

        @Override
        public void setFraction(double fraction) {
          long current = System.currentTimeMillis();
          long last = lastClearedTimeStamp.get();
          if (current - last >= 500 && lastClearedTimeStamp.compareAndSet(last, current)) {
            // fraction is changed when each file is processed =>
            // resolve caches used when searching in that file are likely to be not needed anymore
            dropResolveCaches();
          }
        }
      });
    }
  }
}
