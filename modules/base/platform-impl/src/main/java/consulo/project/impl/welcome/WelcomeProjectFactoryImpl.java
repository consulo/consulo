/*
 * Copyright 2013-2021 consulo.io
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
package consulo.project.impl.welcome;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import consulo.components.impl.stores.DefaultProjectStoreImpl;
import consulo.disposer.Disposable;
import consulo.project.WelcomeProjectFactory;
import consulo.project.impl.SingleFileProjectFactoryImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25/12/2021
 */
@State(name = "ProjectManager", storages = @Storage("welcome.project.xml"))
@Singleton
public class WelcomeProjectFactoryImpl extends SingleFileProjectFactoryImpl<WelcomeProjectImpl> implements WelcomeProjectFactory, PersistentStateComponent<Element>, Disposable {
  @Inject
  public WelcomeProjectFactoryImpl(@Nonnull Application application, @Nonnull ProjectManager projectManager) {
    super(new WelcomeProjectImpl(application, projectManager, "", application.isUnitTestMode()), DefaultProjectStoreImpl.ROOT_TAG_NAME);
  }

  @Nonnull
  @Override
  public Project getWelcomeProject() {
    return getProject();
  }
}