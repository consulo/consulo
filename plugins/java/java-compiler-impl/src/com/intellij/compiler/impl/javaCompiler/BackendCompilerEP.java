/*
 * Copyright 2013 Consulo.org
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
package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.project.Project;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

/**
 * @author VISTALL
 * @since 13:50/10.06.13
 */
public class BackendCompilerEP extends AbstractExtensionPointBean {
  @Attribute("implementationClass")
  public String implementationClass;

  private BackendCompiler myBackendCompiler;

  @NotNull
  public BackendCompiler getInstance(@NotNull Project project) {
    if(myBackendCompiler == null) {
      final Class<BackendCompiler> classNoExceptions = findClassNoExceptions(implementationClass);
      if(classNoExceptions != null) {
        final Constructor<BackendCompiler> constructor;
        try {
          constructor = classNoExceptions.getConstructor(Project.class);
        }
        catch (NoSuchMethodException e) {
           throw new Error(e);
        }
        myBackendCompiler = ReflectionUtil.createInstance(constructor, project);
      }
    }
    return myBackendCompiler;
  }
}
