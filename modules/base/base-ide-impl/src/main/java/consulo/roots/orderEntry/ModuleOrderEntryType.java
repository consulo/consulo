/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.orderEntry;

import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.impl.ModuleOrderEntryImpl;
import com.intellij.openapi.util.InvalidDataException;
import consulo.roots.ModuleRootLayer;
import consulo.roots.impl.ModuleRootLayerImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public class ModuleOrderEntryType implements OrderEntryType<ModuleOrderEntryImpl> {
  @Nonnull
  public static ModuleOrderEntryType getInstance() {
    return EP_NAME.findExtension(ModuleOrderEntryType.class);
  }

  @NonNls
  public static final String MODULE_NAME_ATTR = "module-name";
  @NonNls
  private static final String EXPORTED_ATTR = "exported";
  @NonNls
  private static final String PRODUCTION_ON_TEST_ATTRIBUTE = "production-on-test";

  @Nonnull
  @Override
  public String getId() {
    return "module";
  }

  @Nonnull
  @Override
  public ModuleOrderEntryImpl loadOrderEntry(@Nonnull Element element, @Nonnull ModuleRootLayer moduleRootLayer) throws InvalidDataException {
    String moduleName = element.getAttributeValue(MODULE_NAME_ATTR);
    if (moduleName == null) {
      throw new InvalidDataException();
    }
    DependencyScope dependencyScope = DependencyScope.readExternal(element);
    boolean exported = element.getAttributeValue(EXPORTED_ATTR) != null;
    boolean productionOnTestDependency = element.getAttributeValue(PRODUCTION_ON_TEST_ATTRIBUTE) != null;
    return new ModuleOrderEntryImpl(moduleName, (ModuleRootLayerImpl)moduleRootLayer, dependencyScope, exported, productionOnTestDependency);
  }

  @Override
  public void storeOrderEntry(@Nonnull Element element, @Nonnull ModuleOrderEntryImpl orderEntry) {
    element.setAttribute(MODULE_NAME_ATTR, orderEntry.getModuleName());
    if (orderEntry.isExported()) {
      element.setAttribute(EXPORTED_ATTR, "");
    }
    orderEntry.getScope().writeExternal(element);
    if (orderEntry.isProductionOnTestDependency()) {
      element.setAttribute(PRODUCTION_ON_TEST_ATTRIBUTE, "");
    }
  }
}
