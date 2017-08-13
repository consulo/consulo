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
package com.intellij.ide.util.treeView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.util.containers.ConcurrentWeakHashMap;
import consulo.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentMap;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 6, 2004
 */
public class TreeViewUtil {
  private static final int SUBPACKAGE_LIMIT = 2;
  private static final Key<ConcurrentMap<PsiPackage, Boolean>> SHOULD_ABBREV_PACK_KEY = Key.create("PACK_ABBREV_CACHE");

  private static boolean shouldAbbreviateName(PsiPackage aPackage) {
    final Project project = aPackage.getProject();
    ConcurrentMap<PsiPackage, Boolean> map = project.getUserData(SHOULD_ABBREV_PACK_KEY);
    if (map == null) {
      final ConcurrentWeakHashMap<PsiPackage, Boolean> newMap = new ConcurrentWeakHashMap<PsiPackage, Boolean>();
      map = ((UserDataHolderEx)project).putUserDataIfAbsent(SHOULD_ABBREV_PACK_KEY, newMap);
      if (map == newMap) {
        ((PsiManagerEx)PsiManager.getInstance(project)).registerRunnableToRunOnChange(new Runnable() {
          @Override
          public void run() {
            newMap.clear();
          }
        });
      }
    }

    Boolean ret = map.get(aPackage);
    if (ret != null) return ret;
    ret = scanPackages(aPackage, 1);
    map.put(aPackage, ret);
    return ret;
  }

  private static boolean scanPackages(@NotNull PsiPackage p, int packageNameOccurrencesFound) {
    final PsiPackage[] subPackages = p.getSubPackages();
    packageNameOccurrencesFound += subPackages.length;
    if (packageNameOccurrencesFound > SUBPACKAGE_LIMIT) {
      return true;
    }
    for (PsiPackage subPackage : subPackages) {
      if (scanPackages(subPackage, packageNameOccurrencesFound)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public static String calcAbbreviatedPackageFQName(@NotNull PsiPackage aPackage) {
    final StringBuilder name = new StringBuilder(aPackage.getName());
    for (PsiPackage parentPackage = aPackage.getParentPackage(); parentPackage != null; parentPackage = parentPackage.getParentPackage()) {
      final String packageName = parentPackage.getName();
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

  @NotNull
  public static String getNodeName(@NotNull ViewSettings settings,
                                   PsiPackage aPackage,
                                   final PsiPackage parentPackageInTree,
                                   @NotNull String defaultShortName,
                                   boolean isFQNameShown) {
    final String name;
    if (isFQNameShown) {
      name = settings.isAbbreviatePackageNames() ?
             aPackage == null ? defaultShortName : calcAbbreviatedPackageFQName(aPackage) :
             aPackage == null ? defaultShortName : aPackage.getQualifiedName();
    }
    else if (parentPackageInTree != null || aPackage != null && aPackage.getParentPackage() != null) {
      PsiPackage parentPackage = aPackage.getParentPackage();
      final StringBuilder buf = new StringBuilder();
      buf.append(aPackage.getName());
      while (parentPackage != null && (parentPackageInTree == null || !parentPackage.equals(parentPackageInTree))) {
        final String parentPackageName = parentPackage.getName();
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
