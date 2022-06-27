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

package consulo.ide.impl.idea.ui.tabs;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.Project;
import consulo.ide.impl.idea.ui.FileColorManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;

/**
 * @author spleaner
 */
@Singleton
@State(name = "SharedFileColors", storages = @Storage("fileColors.xml"))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class FileColorSharedConfigurationManager implements PersistentStateComponent<Element> {
  private final Project myProject;

  @Inject
  public FileColorSharedConfigurationManager(@Nonnull final Project project) {
    myProject = project;
  }

  @Override
  public Element getState() {
    return ((FileColorManagerImpl)FileColorManager.getInstance(myProject)).getState(true);
  }

  @Override
  public void loadState(Element state) {
    ((FileColorManagerImpl)FileColorManager.getInstance(myProject)).loadState(state, true);
  }
}
