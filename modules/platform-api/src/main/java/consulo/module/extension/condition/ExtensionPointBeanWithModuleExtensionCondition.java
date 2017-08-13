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
package consulo.module.extension.condition;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 2:39/10.09.13
 */
public class ExtensionPointBeanWithModuleExtensionCondition extends AbstractExtensionPointBean {
  @Attribute("requireModuleExtensions")
  public String requireModuleExtensions;

  private NotNullLazyValue<ModuleExtensionCondition> myModuleExtensionCondition = new NotNullLazyValue<ModuleExtensionCondition>() {
    @NotNull
    @Override
    protected ModuleExtensionCondition compute() {
      return ModuleExtensionConditionImpl.create(requireModuleExtensions);
    }
  };


  @NotNull
  public ModuleExtensionCondition getModuleExtensionCondition() {
    return myModuleExtensionCondition.getValue();
  }
}
