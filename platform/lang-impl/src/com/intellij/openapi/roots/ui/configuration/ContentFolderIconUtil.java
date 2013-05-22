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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.roots.ContentFolderType;

import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 20
 * @author 2003
 */
public class ContentFolderIconUtil {

  public static Icon getRootIcon(ContentFolderType contentFolderType) {
    switch (contentFolderType) {
      case SOURCE:
        return AllIcons.Modules.SourceRoot;
      case TEST:
        return AllIcons.Modules.TestRoot;
      case RESOURCE:
        return AllIcons.Modules.ResourceRoot;
      case EXCLUDED:
      case EXCLUDED_OUTPUT:
        return AllIcons.Modules.ExcludeRoot;
    }
    return null;
  }
}
