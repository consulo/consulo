/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.module.content;

import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author peter
 */
public class PackageDirectoryCache {
  private final MultiMap<String, VirtualFile> myRootsByPackagePrefix;
  private final Map<String, PackageInfo> myDirectoriesByPackageNameCache = new ConcurrentHashMap<>();
  private final Set<String> myNonExistentPackages = ConcurrentHashMap.newKeySet();

  public PackageDirectoryCache(@Nonnull MultiMap<String, VirtualFile> rootsByPackagePrefix) {
    myRootsByPackagePrefix = rootsByPackagePrefix;
  }

  public void onLowMemory() {
    myNonExistentPackages.clear();
  }

  @Nonnull
  public List<VirtualFile> getDirectoriesByPackageName(@Nonnull String packageName) {
    PackageInfo info = getPackageInfo(packageName);
    return info == null ? Collections.emptyList() : info.myPackageDirectories;
  }

  @Nullable
  private PackageInfo getPackageInfo(@Nonnull String packageName) {
    PackageInfo info = myDirectoriesByPackageNameCache.get(packageName);
    if (info == null) {
      if (myNonExistentPackages.contains(packageName)) return null;

      if (packageName.length() > Registry.intValue("java.max.package.name.length") || StringUtil.containsAnyChar(packageName, ";[/")) {
        return null;
      }

      List<VirtualFile> result = new ArrayList<>();

      if (StringUtil.isNotEmpty(packageName) && !StringUtil.startsWithChar(packageName, '.')) {
        int i = packageName.lastIndexOf('.');
        while (true) {
          PackageInfo parentInfo = getPackageInfo(i > 0 ? packageName.substring(0, i) : "");
          if (parentInfo != null) {
            result.addAll(parentInfo.getSubPackageDirectories(packageName.substring(i + 1)));
          }
          if (i < 0) break;
          i = packageName.lastIndexOf('.', i - 1);
          ProgressManager.checkCanceled();
        }
      }

      for (VirtualFile file : myRootsByPackagePrefix.get(packageName)) {
        if (file.isDirectory()) {
          result.add(file);
        }
      }

      if (!result.isEmpty()) {
        myDirectoriesByPackageNameCache.put(packageName, info = new PackageInfo(packageName, result));
      }
      else {
        myNonExistentPackages.add(packageName);
      }
    }

    return info;
  }

  public Set<String> getSubpackageNames(@Nonnull String packageName) {
    PackageInfo info = getPackageInfo(packageName);
    return info == null ? Collections.emptySet() : Collections.unmodifiableSet(info.mySubPackages.get().keySet());
  }

  private class PackageInfo {
    final String myQname;
    final List<VirtualFile> myPackageDirectories;
    final Supplier<MultiMap<String, VirtualFile>> mySubPackages;

    PackageInfo(String qname, List<VirtualFile> packageDirectories) {
      myQname = qname;
      myPackageDirectories = packageDirectories;
      mySubPackages = LazyValue.notNull(() -> {
        MultiMap<String, VirtualFile> result = MultiMap.createLinked();
        for (VirtualFile directory : myPackageDirectories) {
          for (VirtualFile child : directory.getChildren()) {
            String childName = child.getName();
            String packageName = myQname.isEmpty() ? childName : myQname + "." + childName;
            if (child.isDirectory() && isPackageDirectory(child, packageName)) {
              result.putValue(childName, child);
            }
          }
        }
        return result;
      });
    }

    @Nonnull
    Collection<VirtualFile> getSubPackageDirectories(String shortName) {
      return mySubPackages.get().get(shortName);
    }
  }

  protected boolean isPackageDirectory(@Nonnull VirtualFile dir, @Nonnull String packageName) {
    return true;
  }
}
