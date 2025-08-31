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

package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.project.ui.view.internal.AbstractUrl;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import org.jetbrains.annotations.NonNls;


public class LibraryModuleGroupUrl extends AbstractUrl {

  @NonNls private static final String ELEMENT_TYPE = "libraryModuleGroup";

  public LibraryModuleGroupUrl(String moduleName) {
    super(null, moduleName, ELEMENT_TYPE);
  }

  @Override
  public Object[] createPath(Project project) {
    Module module = moduleName != null ? ModuleManager.getInstance(project).findModuleByName(moduleName) : null;
    if (module == null) return null;
    return new Object[]{new LibraryGroupElement(module)};
  }

  @Override
  protected AbstractUrl createUrl(String moduleName, String url) {
      return new LibraryModuleGroupUrl(moduleName);
  }

  @Override
  public AbstractUrl createUrlByElement(Object element) {
    if (element instanceof LibraryGroupElement) {
      LibraryGroupElement libraryGroupElement = (LibraryGroupElement)element;
      return new LibraryModuleGroupUrl(libraryGroupElement.getModule() != null ? libraryGroupElement.getModule().getName() : null);
    }
    return null;
  }
}
