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

package com.intellij.openapi.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

public class ServiceDescriptor {
  private static NotNullLazyValue<Boolean> ourCompilerServerMode = new NotNullLazyValue<Boolean>() {
    @NotNull
    @Override
    protected Boolean compute() {
      return ApplicationManager.getApplication().isCompilerServerMode();
    }
  };

  @Attribute("serviceInterface")
  public String serviceInterface;

  @Attribute("serviceImplementation")
  public String serviceImplementation;

  @Attribute("serviceImplementationForCompilerServer")
  public String serviceImplementationForCompilerServer;

  @Attribute("overrides")
  @Deprecated
  public boolean overrides = false;


  public String getInterface() {
    return serviceInterface != null ? serviceInterface : getImplementation();
  }

  public String getImplementation() {
    if(ourCompilerServerMode.getValue()) {
      if(!StringUtil.isEmpty(serviceImplementationForCompilerServer)) {
        return serviceImplementationForCompilerServer;
      }
    }

    return serviceImplementation;
  }
}
