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
package consulo.packagesView;

import com.intellij.webcore.packaging.PackageManagementService;
import com.intellij.webcore.packaging.RepoPackage;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 30/05/2021
 * <p>
 * Implementation of {@link PackageManagementService} but changed logic of {@link PackageManagementService#getAllPackages()}
 * <p>
 * Now method {@link #getPackages} will be called instead.
 * <p>
 * This implementation not support view all packages, only return of search. Query will be empty, if no search query
 */
public interface SearchablePackageManagementService {
  @Nonnull
  List<RepoPackage> getPackages(@Nonnull String searchQuery, int from, int to);

  int getPageSize();
}
