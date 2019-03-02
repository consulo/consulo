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
package com.intellij.codeInspection.reference;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;

import java.util.Collection;

/**
 * Application component which provides utility methods for working with the reference
 * graph.
 *
 * @author anna
 * @since 6.0
 */
public class RefUtil {
  private RefUtil() {}

  public static boolean isImplicitUsage(PsiElement element) {
    for (ImplicitUsageProvider provider : ImplicitUsageProvider.EP_NAME.getExtensionList()) {
      if (provider.isImplicitUsage(element)) return true;
    }
    return false;
  }

  public static boolean contains(RefEntity element, @Nonnull Collection<RefEntity> entities){
    for (RefEntity refEntity : entities) {
      if (Comparing.equal(refEntity, element)) {
        return true;
      }
    }
    return false;
  }
}
