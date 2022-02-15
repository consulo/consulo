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
package com.intellij.psi;

import com.intellij.openapi.util.NotNullLazyValue;
import consulo.application.Application;
import consulo.component.extension.AbstractExtensionPointBean;
import consulo.component.extension.KeyedLazyInstance;
import consulo.util.xml.serializer.annotation.Attribute;

import javax.annotation.Nonnull;

/**
 * @author peter
 */
public class WeigherExtensionPoint extends AbstractExtensionPointBean implements KeyedLazyInstance<Weigher> {

  // these must be public for scrambling compatibility
  @Attribute("key")
  public String key;

  @Attribute("implementationClass")
  public String implementationClass;

  @Attribute("id")
  public String id;

  private final NotNullLazyValue<Weigher> myHandler = new NotNullLazyValue<>() {
    @Override
    @Nonnull
    protected final Weigher compute() {
      try {
        Class<Weigher> tClass = findClass(implementationClass);
        final Weigher weigher = Application.get().getInjectingContainer().getUnbindedInstance(tClass);
        weigher.setDebugName(id);
        return weigher;
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  };

  @Override
  public Weigher getInstance() {
    return myHandler.getValue();
  }

  @Override
  public String getKey() {
    return key;
  }
}
