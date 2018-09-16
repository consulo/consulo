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
package consulo.lang.injection;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.util.xmlb.annotations.Attribute;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23:59/28.08.13
 */
public class MultiHostInjectorExtensionPoint extends AbstractExtensionPointBean {
  @Attribute("forClass")
  public String psiElementClass;

  @Attribute("implementationClass")
  public String implementationClass;

  private MultiHostInjector myInstance;

  private final NotNullLazyValue<Class<PsiElement>> myForClassHandler = new NotNullLazyValue<Class<PsiElement>>() {
    @Nonnull
    @Override
    protected Class<PsiElement> compute() {
      assert psiElementClass != null : getPluginDescriptor().getPluginId().getIdString();
      Class<PsiElement> clazz = findClassNoExceptions(psiElementClass);
      assert clazz != null : psiElementClass;
      return clazz;
    }
  };

  private final NotNullLazyValue<Class<MultiHostInjector>> myImplementationClassHandler = new NotNullLazyValue<Class<MultiHostInjector>>() {
    @Nonnull
    @Override
    protected Class<MultiHostInjector> compute() {
      assert implementationClass != null : getPluginDescriptor().getPluginId().getIdString();
      Class<MultiHostInjector> clazz = findClassNoExceptions(implementationClass);
      assert clazz != null : implementationClass;
      return clazz;
    }
  };

  @Nonnull
  public Class<PsiElement> getKey() {
    return myForClassHandler.getValue();
  }

  @Nonnull
  public MultiHostInjector getInstance(@Nonnull ComponentManager componentManager) {
    if(myInstance == null) {
      myInstance = instantiate(myImplementationClassHandler.getValue(), componentManager.getInjectingContainer());
    }
    return myInstance;
  }
}
