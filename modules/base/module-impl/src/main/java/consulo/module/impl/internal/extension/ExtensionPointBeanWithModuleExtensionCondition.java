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
package consulo.module.impl.internal.extension;

import consulo.component.extension.AbstractExtensionPointBean;
import consulo.module.extension.condition.ModuleExtensionCondition;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.xml.serializer.annotation.Attribute;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2:39/10.09.13
 */
public class ExtensionPointBeanWithModuleExtensionCondition extends AbstractExtensionPointBean {
  @Attribute("requireModuleExtensions")
  public String requireModuleExtensions;

  private Supplier<ModuleExtensionCondition> myModuleExtensionCondition = LazyValue.notNull(() -> {
    return ModuleExtensionConditionImpl.create(requireModuleExtensions);
  });

  @Nonnull
  public ModuleExtensionCondition getModuleExtensionCondition() {
    return myModuleExtensionCondition.get();
  }
}
