/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.actions.GotoActionBase;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2005-01-26
 */
@Singleton
@ServiceImpl
public class ChooseByNameFactoryImpl extends ChooseByNameFactory {
  private final Project myProject;

  @Inject
  public ChooseByNameFactoryImpl(final Project project) {
    myProject = project;
  }

  @Override
  public ChooseByNamePopup createChooseByNamePopupComponent(@Nonnull final ChooseByNameModel model) {
    return ChooseByNamePopup.createPopup(myProject, model, GotoActionBase.getPsiContext(myProject));  
  }
}
