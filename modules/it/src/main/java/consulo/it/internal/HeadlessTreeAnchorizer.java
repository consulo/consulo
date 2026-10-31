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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.tree.TreeAnchorizer;
import jakarta.inject.Singleton;

/**
 * The production {@code PsiTreeAnchorizer} lives in {@code consulo.ide.impl}; structure view elements
 * ({@code PsiTreeElementBase}) need an anchorizer, so headless tests can build structure view models.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessTreeAnchorizer extends TreeAnchorizer {
}
