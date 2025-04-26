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
package consulo.language.impl.internal.util.proximity;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.util.proximity.ProximityLocation;
import consulo.language.util.proximity.ProximityWeigher;
import consulo.module.Module;
import jakarta.annotation.Nonnull;

/**
 * @author peter
*/
@ExtensionImpl(id = "sameModule", order = "after sameLogicalRoot")
public class SameModuleWeigher extends ProximityWeigher {

  @Override
  public Comparable weigh(@Nonnull final PsiElement element, @Nonnull final ProximityLocation location) {
    final Module elementModule = element.getModule();
    if (location.getPositionModule() == elementModule) {
      return 2;
    }

    if (elementModule != null) {
      return 1; // in project => still not bad
    }

    return 0;
  }
}
