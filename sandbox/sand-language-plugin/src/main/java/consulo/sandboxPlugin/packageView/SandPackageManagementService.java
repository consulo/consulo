/*
 * Copyright 2013-2021 consulo.io
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
package consulo.sandboxPlugin.packageView;

import com.intellij.util.CatchingConsumer;
import com.intellij.util.containers.MultiMap;
import com.intellij.webcore.packaging.InstalledPackage;
import com.intellij.webcore.packaging.PackageManagementServiceEx;
import com.intellij.webcore.packaging.RepoPackage;
import consulo.packagesView.SearchablePackageManagementService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 30/05/2021
 */
public class SandPackageManagementService extends PackageManagementServiceEx implements SearchablePackageManagementService {
  private MultiMap<String, RepoPackage> myMap = new MultiMap<>();

  public SandPackageManagementService() {
    for (int i = 0; i < 100; i++) {
      myMap.putValue("test", new RepoPackage("Test " + i, null, i + "." + i));
    }

    for (int i = 0; i < 100; i++) {
      myMap.putValue("my", new RepoPackage("My " + i, null, i + "." + i));
    }
  }

  @Override
  public void updatePackage(@Nonnull InstalledPackage installedPackage, @Nullable String version, @Nonnull Listener listener) {

  }

  @Override
  public void fetchLatestVersion(@Nonnull InstalledPackage pkg, @Nonnull CatchingConsumer<String, Exception> consumer) {

  }

  @Nonnull
  @Override
  public List<RepoPackage> getAllPackages() throws IOException {
    return List.of();
  }

  @Override
  public void installPackage(RepoPackage repoPackage, @Nullable String version, boolean forceUpgrade, @Nullable String extraOptions, Listener listener, boolean installToUser) {

  }

  @Override
  public void uninstallPackages(List<InstalledPackage> installedPackages, Listener listener) {

  }

  @Override
  public void fetchPackageVersions(String packageName, CatchingConsumer<List<String>, Exception> consumer) {

  }

  @Override
  public void fetchPackageDetails(String packageName, CatchingConsumer<String, Exception> consumer) {
    consumer.accept("Some Description");
  }

  @Nonnull
  @Override
  public List<RepoPackage> getPackages(@Nonnull String searchQuery, int from, int to) {
    if(searchQuery.isEmpty()) {
      return new ArrayList<>(myMap.values());
    }
    return new ArrayList<>(myMap.get(searchQuery));
  }

  @Override
  public int getPageSize() {
    return 100;
  }
}
