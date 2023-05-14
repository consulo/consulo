/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.base;

import consulo.annotation.component.ServiceImpl;
import consulo.find.internal.FindApiInternal;
import consulo.ide.impl.idea.ide.util.scopeChooser.ScopeChooserConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 14/05/2023
 */
@Singleton
@ServiceImpl
public class FindApiInternalImpl implements FindApiInternal {
  private final ShowSettingsUtil myShowSettingsUtil;

  @Inject
  public FindApiInternalImpl(ShowSettingsUtil showSettingsUtil) {
    myShowSettingsUtil = showSettingsUtil;
  }

  @Override
  @RequiredUIAccess
  public AsyncResult<Void> openScopeConfigurable(Project project, @Nullable String selection) {
    return myShowSettingsUtil.showAndSelect(project, ScopeChooserConfigurable.class, configurable -> {
      if (selection != null) {
        configurable.selectNodeInTree(selection);
      }
    }) ;
  }
}
