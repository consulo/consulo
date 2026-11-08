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

import java.util.List;

/**
 * A problem of the environment which prevents managing packages, with the fixes the user can run.
 *
 * @see PackageManagementService#findProblem()
 */
public record PackageManagementProblem(LocalizeValue message, List<PackageManagementFix> fixes) {
    public PackageManagementProblem(LocalizeValue message) {
        this(message, List.of());
    }
}
