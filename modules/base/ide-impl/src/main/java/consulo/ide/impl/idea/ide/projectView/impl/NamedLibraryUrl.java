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

import consulo.ide.impl.idea.ide.projectView.impl.nodes.NamedLibraryElement;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import org.jetbrains.annotations.NonNls;

/**
 * @author cdr
 */
public class NamedLibraryUrl extends AbstractUrl {

  @NonNls private static final String ELEMENT_TYPE = "namedLibrary";

  public NamedLibraryUrl(String url, String moduleName) {
    super(url, moduleName, ELEMENT_TYPE);
  }

  @Override
  public Object[] createPath(Project project) {
    final Module module = moduleName != null ? ModuleManager.getInstance(project).findModuleByName(moduleName) : null;
    if (module == null) return null;
    for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
      if (orderEntry instanceof LibraryOrderEntry || orderEntry instanceof ModuleExtensionWithSdkOrderEntry && orderEntry.getPresentableName().equals(url)) {
        return new Object[]{new NamedLibraryElement(module, orderEntry)};
      }
    }
    return null;
  }

  @Override
  protected AbstractUrl createUrl(String moduleName, String url) {
      return new NamedLibraryUrl(url, moduleName);
  }

  @Override
  public AbstractUrl createUrlByElement(Object element) {
    if (element instanceof NamedLibraryElement) {
      NamedLibraryElement libraryElement = (NamedLibraryElement)element;

      Module context = libraryElement.getModule();
      
      return new NamedLibraryUrl(libraryElement.getOrderEntry().getPresentableName(), context != null ? context.getName() : null);
    }
    return null;
  }
}
