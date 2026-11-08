/*
 * Copyright 2013-2026 consulo.io
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
package consulo.repository.ui;

import consulo.localize.LocalizeValue;
import consulo.repository.ui.localize.RepositoryUILocalize;

/**
 * A scope a package is installed into, as a project system defines it: the dependencies or the dev dependencies of a package.json,
 * the user site of a Python interpreter.
 *
 * @param id   the identifier the service knows the scope by
 * @param name the presentable name of the scope
 */
public record PackageInstallScope(String id, LocalizeValue name) {
    public static final String DEFAULT_ID = "default";
    public static final String USER_ID = "user";

    public static final PackageInstallScope DEFAULT = new PackageInstallScope(DEFAULT_ID, RepositoryUILocalize.scopeDefault());
}
