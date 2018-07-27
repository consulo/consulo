/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.util.registry;

import com.intellij.openapi.components.*;
import consulo.annotations.NotLazy;
import org.jdom.Element;

import javax.inject.Singleton;

@State(name = "Registry", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
@NotLazy
@Singleton
public class RegistryState implements PersistentStateComponent<Element> {
  @Override
  public Element getState() {
    return Registry.getInstance().getState();
  }

  @Override
  public void loadState(Element state) {
    Registry.getInstance().loadState(state);
  }
}