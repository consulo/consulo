/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.psi.util.PsiModificationTracker;
import consulo.injecting.InjectingContainerBuilder;
import consulo.test.light.impl.LightExtensionRegistrator;
import consulo.test.light.impl.LightFileIndexFacade;
import consulo.test.light.impl.LightProject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightProjectBuilder {
  public static class DefaultRegistrator extends LightExtensionRegistrator {
    @Override
    public void registerExtensionPointsAndExtensions(@Nonnull ExtensionsAreaImpl area) {

    }

    @Override
    public void registerServices(@Nonnull InjectingContainerBuilder builder) {
      builder.bind(PsiFileFactory.class).to(PsiFileFactoryImpl.class);
      builder.bind(PsiManager.class).to(PsiManagerImpl.class);
      builder.bind(FileIndexFacade.class).to(LightFileIndexFacade.class);
      builder.bind(PsiModificationTracker.class).to(PsiModificationTrackerImpl.class);
    }
  }

  @Nonnull
  public static LightProjectBuilder create(@Nonnull Application application) {
    return create(application, new DefaultRegistrator());
  }

  @Nonnull
  public static LightProjectBuilder create(@Nonnull Application application, @Nonnull DefaultRegistrator registrator) {
    return new LightProjectBuilder(application, registrator);
  }

  private final Application myApplication;
  private final LightExtensionRegistrator myRegistrator;

  private LightProjectBuilder(Application application, LightExtensionRegistrator registrator) {
    myApplication = application;
    myRegistrator = registrator;
  }

  @Nonnull
  public Project build() {
    return new LightProject(myApplication, "LightProjectBuilder:" + hashCode(), myRegistrator);
  }
}
