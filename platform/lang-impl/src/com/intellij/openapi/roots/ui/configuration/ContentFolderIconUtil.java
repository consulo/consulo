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
import com.intellij.openapi.project.ProjectBundle;
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
      case PRODUCTION:
        return AllIcons.Modules.SourceRoot;
      case TEST:
        return AllIcons.Modules.TestRoot;
      case PRODUCTION_RESOURCE:
        return AllIcons.Modules.ResourcesRoot;
      case TEST_RESOURCE:
        return AllIcons.Modules.TestResourcesRoot;
      case EXCLUDED:
        return AllIcons.Modules.ExcludeRoot;
    }
    return null;
  }

  public static String getName(ContentFolderType contentFolderType) {
    switch (contentFolderType) {

      case PRODUCTION:
        return ProjectBundle.message("module.toggle.sources.action");
      case PRODUCTION_RESOURCE:
        return ProjectBundle.message("module.toggle.resources.action");
      case TEST:
        return ProjectBundle.message("module.toggle.test.sources.action");
      case TEST_RESOURCE:
        return ProjectBundle.message("module.toggle.test.resources.action");
      case EXCLUDED:
        return ProjectBundle.message("module.toggle.excluded.action");
      default:
        throw new IllegalArgumentException(contentFolderType.name());
    }
  }

  public static String getDescription(ContentFolderType contentFolderType) {
    switch (contentFolderType) {
      case PRODUCTION:
      case PRODUCTION_RESOURCE:
      case TEST:
      case TEST_RESOURCE:
        return ProjectBundle.message("module.toggle.0.action.description", getName(contentFolderType));
      case EXCLUDED:
        return ProjectBundle.message("module.toggle.excluded.action.description");
      default:
        throw new IllegalArgumentException(contentFolderType.name());
    }
  }
}
