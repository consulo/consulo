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
package consulo.project.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import jakarta.inject.Inject;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 25/12/2021
 */
public class SingleFileProjectFactoryImpl<P extends SingleFileProjectImpl> implements PersistentStateComponent<Element>, Disposable {
  private static final Logger LOG = Logger.getInstance(SingleFileProjectFactoryImpl.class);
  @Nonnull
  private final String myRootTagName;

  private P myProject;
  private boolean myProjectWasDisposed = false;

  @Inject
  public SingleFileProjectFactoryImpl(@Nonnull P project, @Nonnull String rootTagName) {
    myRootTagName = rootTagName;
    myProject = project;
    myProject.initNotLazyServices(null);
    myProject.setInitialized();
  }

  @TestOnly
  public boolean isProjectInitialized() {
    return myProject != null;
  }

  @Nonnull
  public P getProject() {
    LOG.assertTrue(!myProjectWasDisposed, "Project has been already disposed!");
    return myProject;
  }

  @RequiredWriteAction
  @Nullable
  @Override
  public Element getState() {
    assert myProject != null;

    myProject.save();

    Element stateElement = myProject.getStateElement();

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
    Element defaultProjectElement = state.getChild(myRootTagName);
    if (defaultProjectElement != null) {
      defaultProjectElement.detach();

      myProject.setStateElement(defaultProjectElement);
    }
  }

  @Override
  public void afterLoadState() {
    try {
      myProject.getStateStore().load();
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @Override
  public void dispose() {
    if (myProject != null) {
      Disposer.dispose(myProject);

      myProject = null;
      myProjectWasDisposed = true;
    }
  }
}