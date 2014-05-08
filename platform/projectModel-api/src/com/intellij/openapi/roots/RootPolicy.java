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
package com.intellij.openapi.roots;

import org.mustbe.consulo.DeprecationInfo;

/**
 * @author dsl
 */
public class RootPolicy<R> {
  public R visitOrderEntry(OrderEntry orderEntry, R value) {
    return value;
  }

  public R visitModuleSourceOrderEntry(ModuleSourceOrderEntry moduleSourceOrderEntry, R value) {
    return visitOrderEntry(moduleSourceOrderEntry, value);
  }

  public R visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, R value) {
    return visitOrderEntry(libraryOrderEntry, value);
  }

  public R visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, R value) {
    return visitOrderEntry(moduleOrderEntry, value);
  }

  @Deprecated
  @DeprecationInfo(value = "Use #visitModuleExtensionSdkOrderEntry", until = "2.0")
  public R visitJdkOrderEntry(SdkOrderEntry sdkOrderEntry, R value) {
    return visitOrderEntry(sdkOrderEntry, value);
  }

  @Deprecated
  @DeprecationInfo(value = "Use #visitModuleExtensionSdkOrderEntry", until = "2.0")
  public R visitModuleJdkOrderEntry(ModuleExtensionWithSdkOrderEntry jdkOrderEntry, R value) {
    return visitJdkOrderEntry(jdkOrderEntry, value);
  }

  public R visitModuleExtensionSdkOrderEntry(ModuleExtensionWithSdkOrderEntry orderEntry, R value) {
    return visitModuleJdkOrderEntry(orderEntry, value);
  }
}
