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

package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiElement;

/**
 * Allows to disable highlighting of certain elements as unused when such elements are not referenced
 * from the code but are referenced in some other way
 *
 * @author yole
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface ImplicitUsageProvider {
  /**
   * @return true if element should not be reported as unused
   */
  default boolean isImplicitUsage(PsiElement element) {
    return false;
  }

  /**
   * @return true if element should not be reported as "assigned but not used"
   */
  default boolean isImplicitRead(PsiElement element) {
    return false;
  }

  /**
   * @return true if element should not be reported as "referenced but never assigned"
   */
  default boolean isImplicitWrite(PsiElement element) {
    return false;
  }
}
