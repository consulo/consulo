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
package consulo.compiler.make;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import javax.annotation.Nonnull;

import java.lang.reflect.Constructor;

/**
 * @author VISTALL
 * @since 15:16/20.10.13
 */
public class DependencyCacheEP extends AbstractExtensionPointBean {
  @Attribute("implementationClass")
  public String implementationClass;

  private NotNullLazyValue<Constructor<DependencyCache>> myClassValue = new NotNullLazyValue<Constructor<DependencyCache>>() {
    @Nonnull
    @Override
    protected Constructor<DependencyCache> compute() {
      Class<DependencyCache> classNoExceptions = findClassNoExceptions(implementationClass);
      if(classNoExceptions == null) {
        throw new IllegalArgumentException();
      }

      try {
        return classNoExceptions.getConstructor(Project.class, String.class);
      }
      catch (NoSuchMethodException e) {
        throw new IllegalArgumentException(e);
      }
    }
  };

  public DependencyCache create(Project project, String cacheDir) {
    return ReflectionUtil.createInstance(myClassValue.getValue(), project, cacheDir);
  }
}
