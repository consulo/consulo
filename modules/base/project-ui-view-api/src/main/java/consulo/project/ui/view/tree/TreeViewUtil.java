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
package consulo.project.ui.view.tree;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiPackage;
import consulo.project.ui.view.internal.PsiPackageAbbreviateHelper;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * @since 2004-10-06
 */
public class TreeViewUtil {
  private static boolean shouldAbbreviateName(PsiPackage aPackage) {
    return aPackage.getProject().getInstance(PsiPackageAbbreviateHelper.class).shouldAbbreviateName(aPackage);
  }

  @Nonnull
  @RequiredReadAction
  public static String calcAbbreviatedPackageFQName(@Nonnull PsiPackage aPackage) {
    StringBuilder name = new StringBuilder(aPackage.getName());
    for (PsiPackage parentPackage = aPackage.getParentPackage(); parentPackage != null; parentPackage = parentPackage.getParentPackage()) {
      String packageName = parentPackage.getName();
      if (packageName == null || packageName.isEmpty()) {
        break; // reached default package
      }
      name.insert(0, ".");
      if (packageName.length() > 2 && shouldAbbreviateName(parentPackage)) {
        name.insert(0, packageName.substring(0, 1));
      }
      else {
        name.insert(0, packageName);
      }
    }
    return name.toString();
  }

  @Nonnull
  @RequiredReadAction
  public static String getNodeName(@Nonnull ViewSettings settings, PsiPackage aPackage, PsiPackage parentPackageInTree, @Nonnull String defaultShortName, boolean isFQNameShown) {
    String name;
    if (isFQNameShown) {
      name = settings.isAbbreviatePackageNames() ? aPackage == null ? defaultShortName : calcAbbreviatedPackageFQName(aPackage) : aPackage == null ? defaultShortName : aPackage.getQualifiedName();
    }
    else if (parentPackageInTree != null || aPackage != null && aPackage.getParentPackage() != null) {
      PsiPackage parentPackage = aPackage.getParentPackage();
      StringBuilder buf = new StringBuilder();
      buf.append(aPackage.getName());
      while (parentPackage != null && (parentPackageInTree == null || !parentPackage.equals(parentPackageInTree))) {
        String parentPackageName = parentPackage.getName();
        if (parentPackageName == null || parentPackageName.isEmpty()) {
          break; // reached default package
        }
        buf.insert(0, ".");
        buf.insert(0, parentPackageName);
        parentPackage = parentPackage.getParentPackage();
      }
      name = buf.toString();
    }
    else {
      name = defaultShortName;
    }
    return name;
  }
}
