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

import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.project.ui.view.internal.AbstractUrl;
import org.jetbrains.annotations.NonNls;

/**
 * @author cdr
 */
public class ModuleUrl extends AbstractUrl {
  @NonNls private static final String ELEMENT_TYPE = "module";

  public ModuleUrl(String url, String moduleName) {
    super(url, moduleName,ELEMENT_TYPE);
  }

  @Override
  public Object[] createPath(Project project) {
    final Module module = moduleName != null ? ModuleManager.getInstance(project).findModuleByName(moduleName) : null;
    if (module == null) return null;
    return new Object[]{module};
  }

  @Override
  protected AbstractUrl createUrl(String moduleName, String url) {
      return new ModuleUrl(url, moduleName);
  }

  @Override
  public AbstractUrl createUrlByElement(Object element) {
    if (element instanceof Module) {
      Module module = (Module)element;
      return new ModuleUrl("", module.getName());
    }
    return null;
  }
}
