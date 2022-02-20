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
package consulo.compiler;

import consulo.component.extension.AbstractExtensionPointBean;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.util.xml.serializer.annotation.Attribute;

import java.lang.reflect.Constructor;

/**
 * @author VISTALL
 * @since 15:16/20.10.13
 */
public class DependencyCacheEP extends AbstractExtensionPointBean {
  @Attribute("implementationClass")
  public String implementationClass;

  private LazyValue<Constructor<DependencyCache>> myClassValue = LazyValue.notNull(() -> {
    Class<DependencyCache> classNoExceptions = findClassNoExceptions(implementationClass);
    if (classNoExceptions == null) {
      throw new IllegalArgumentException();
    }

    try {
      return classNoExceptions.getConstructor(Project.class, String.class);
    }
    catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(e);
    }
  });

  public DependencyCache create(Project project, String cacheDir) {
    return ReflectionUtil.createInstance(myClassValue.get(), project, cacheDir);
  }
}
