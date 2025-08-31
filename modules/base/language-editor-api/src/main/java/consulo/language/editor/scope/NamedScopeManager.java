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
package consulo.language.editor.scope;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.content.scope.NamedScopesHolder;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@State(name = "NamedScopeManager", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class NamedScopeManager extends NamedScopesHolder {
  @Inject
  public NamedScopeManager(Project project) {
    super(project);
  }

  public static NamedScopeManager getInstance(Project project) {
    return project.getInstance(NamedScopeManager.class);
  }

  @Override
  public String getDisplayName() {
    return AnalysisScopeBundle.message("local.scopes.node.text");
  }

  @Override
  public Image getIcon() {
    return AllIcons.Ide.LocalScope;
  }
}
