/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.updateSettings.impl.pluginsAdvertisement;

import com.intellij.openapi.extensions.ExtensionPointName;

/**
 * User: anna
 */
public class UnknownExtension {
  private final String myExtensionKey;
  private final String myValue;

  public UnknownExtension(ExtensionPointName<?> pointName, String value) {
    this(pointName.getName(), value);
  }

  public UnknownExtension(String extensionKey, String value) {
    myExtensionKey = extensionKey;
    myValue = value;
  }

  public String getExtensionKey() {
    return myExtensionKey;
  }

  public String getValue() {
    return myValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UnknownExtension feature = (UnknownExtension)o;

    if (!myExtensionKey.equals(feature.myExtensionKey)) return false;
    if (!myValue.equals(feature.myValue)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myExtensionKey.hashCode();
    result = 31 * result + myValue.hashCode();
    return result;
  }
}
