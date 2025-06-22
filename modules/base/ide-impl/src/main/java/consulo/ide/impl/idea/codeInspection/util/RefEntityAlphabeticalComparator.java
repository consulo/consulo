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
package consulo.ide.impl.idea.codeInspection.util;

import consulo.language.editor.inspection.reference.RefEntity;
import jakarta.annotation.Nonnull;

import java.util.Comparator;

/**
 * @author max
 * @since 2002-01-20
 */
public class RefEntityAlphabeticalComparator implements Comparator<RefEntity> {

  @Override
  public int compare(@Nonnull final RefEntity o1, @Nonnull final RefEntity o2) {
    if (o1 == o2) return 0;
    return o1.getQualifiedName().compareToIgnoreCase(o2.getQualifiedName());
  }

  private static class RefEntityAlphabeticalComparatorHolder {
    private static final RefEntityAlphabeticalComparator ourEntity = new RefEntityAlphabeticalComparator();
  }

  public static RefEntityAlphabeticalComparator getInstance() {

    return RefEntityAlphabeticalComparatorHolder.ourEntity;
  }
}
