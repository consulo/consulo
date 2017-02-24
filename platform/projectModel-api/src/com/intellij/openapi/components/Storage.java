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

package com.intellij.openapi.components;

import consulo.annotations.DeprecationInfo;
import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Storage {
  @NonNls String value() default "";

  @Deprecated
  @DeprecationInfo("Use #value()")
  @NonNls String file() default "";

  @Deprecated
  StorageScheme scheme() default StorageScheme.DEFAULT;

  /**
   * If deprecated: Data will be removed on write. And ignored on read if (and only if) new storage exists.
   */
  boolean deprecated() default false;

  /**
   * You must not store components with different roaming types in one file ({@link #value()}).
   */
  RoamingType roamingType() default RoamingType.PER_USER;

  Class<? extends StateSplitterEx> stateSplitter() default StateSplitterEx.class;
}
