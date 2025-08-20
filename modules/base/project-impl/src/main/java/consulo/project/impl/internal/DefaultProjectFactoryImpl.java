/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.project.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.internal.ComponentBinding;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.DefaultProjectFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;

/**
 * @author yole
 */
@State(name = "ProjectManager", storages = @Storage("project.default.xml"))
@Singleton
@ServiceImpl
public class DefaultProjectFactoryImpl extends DefaultProjectFactory implements PersistentStateComponent<Element>, Disposable {
  private static final Logger LOG = Logger.getInstance(DefaultProjectFactoryImpl.class);

  private DefaultProjectImpl myDefaultProject;
  private boolean myDefaultProjectWasDisposed = false;

  @Inject
  public DefaultProjectFactoryImpl(@Nonnull Application application,
                                   @Nonnull ProjectManager projectManager,
                                   @Nonnull ComponentBinding binding) {
    myDefaultProject = new DefaultProjectImpl(application, projectManager, "", binding);
    myDefaultProject.initNotLazyServices();
    myDefaultProject.setInitialized();
  }

  @TestOnly
  public boolean isDefaultProjectInitialized() {
    return myDefaultProject != null;
  }

  @Override
  @Nonnull
  public Project getDefaultProject() {
    LOG.assertTrue(!myDefaultProjectWasDisposed, "Default project has been already disposed!");
    return myDefaultProject;
  }

  @RequiredWriteAction
  @Nullable
  @Override
  public Element getState() {
    assert myDefaultProject != null;

    myDefaultProject.save();

    Element stateElement = myDefaultProject.getStateElement();

    if (stateElement == null) {
      // we are not ready to save
      return null;
    }

    Element element = new Element("state");
    stateElement.detach();
    element.addContent(stateElement);
    return element;
  }

  @RequiredReadAction
  @Override
  public void loadState(Element state) {
    Element defaultProjectElement = state.getChild("defaultProject");
    if (defaultProjectElement != null) {
      defaultProjectElement.detach();

      myDefaultProject.setStateElement(defaultProjectElement);
    }
  }

  @Override
  public void afterLoadState() {
    try {
      myDefaultProject.getStateStore().load();
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Override
  public void dispose() {
    if (myDefaultProject != null) {
      Disposer.dispose(myDefaultProject);

      myDefaultProject = null;
      myDefaultProjectWasDisposed = true;
    }
  }
}