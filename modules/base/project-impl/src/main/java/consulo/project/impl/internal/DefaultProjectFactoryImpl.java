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
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.internal.ComponentBinding;
import consulo.component.persist.PersistentStateComponentAsync;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.DefaultProjectFactory;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.io.IOException;

/**
 * @author yole
 */
@State(name = "ProjectManager", storages = @Storage("project.default.xml"))
@Singleton
@ServiceImpl
public class DefaultProjectFactoryImpl extends DefaultProjectFactory implements PersistentStateComponentAsync<Element>, Disposable {
  private static final Logger LOG = Logger.getInstance(DefaultProjectFactoryImpl.class);

  private DefaultProjectImpl myDefaultProject;
  private boolean myDefaultProjectWasDisposed = false;

  @Inject
  public DefaultProjectFactoryImpl(Application application,
                                   ProjectManager projectManager,
                                   ComponentBinding binding) {
    myDefaultProject = new DefaultProjectImpl(application, projectManager, "", binding);
    myDefaultProject.initNotLazyServices();
    myDefaultProject.setInitialized();
  }

  @Override
  public Project getDefaultProject() {
    LOG.assertTrue(!myDefaultProjectWasDisposed, "Default project has been already disposed!");
    return myDefaultProject;
  }

  @Override
  public Coroutine<?, Element> getStateAsync() {
    assert myDefaultProject != null;

    UIAccess uiAccess = myDefaultProject.getApplication().getLastUIAccess();

    // Save the default project as a subroutine (its save session populates the state element)
    return Coroutine.<Object, Object>first(CallSubroutine.call(myDefaultProject.saveAsync(uiAccess)))
      .then(CodeExecution.<Object, Element>apply(input -> {
        Element stateElement = myDefaultProject.getStateElement();

        if (stateElement == null) {
          // we are not ready to save
          return null;
        }

        Element element = new Element("state");
        element.addContent(stateElement.clone());
        return element;
      }));
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