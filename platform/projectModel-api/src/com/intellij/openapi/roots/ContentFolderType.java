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
package com.intellij.openapi.roots;

import org.consulo.annotations.Immutable;

/**
* @author VISTALL
* @since 13:20/22.05.13
*/
@Deprecated
public enum ContentFolderType {
  @Deprecated
  SOURCE,   //TODO [VISTALL] remove it after first beta
  @Deprecated
  RESOURCE, //TODO [VISTALL] remove it after first beta

  PRODUCTION,
  PRODUCTION_RESOURCE,
  TEST,
  TEST_RESOURCE,
  EXCLUDED;

  @Immutable
  public static final ContentFolderType[] ALL_SOURCE_ROOTS = new ContentFolderType[] {PRODUCTION, PRODUCTION_RESOURCE, TEST, TEST_RESOURCE};
  @Immutable
  public static final ContentFolderType[] ONLY_PRODUCTION_ROOTS = new ContentFolderType[] {PRODUCTION, PRODUCTION_RESOURCE};
  @Immutable
  public static final ContentFolderType[] ONLY_TEST_ROOTS = new ContentFolderType[] {TEST, TEST_RESOURCE};
}
