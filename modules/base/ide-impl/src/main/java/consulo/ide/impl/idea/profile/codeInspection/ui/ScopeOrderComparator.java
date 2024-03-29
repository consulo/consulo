/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.profile.codeInspection.ui;

import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.ide.impl.idea.util.ArrayUtil;

import java.util.Comparator;

/**
 * @author Dmitry Batkovich
 */
public class ScopeOrderComparator implements Comparator<String> {
  private final String[] myScopesOrder;

  public ScopeOrderComparator(final InspectionProfileImpl inspectionProfile) {
    this(inspectionProfile.getScopesOrder());
  }

  public ScopeOrderComparator(String[] scopesOrder) {
    myScopesOrder = scopesOrder;
  }

  private int getKey(String scope) {
    return myScopesOrder == null ? -1 : ArrayUtil.indexOf(myScopesOrder, scope);
  }

  @Override
  public int compare(String scope1, String scope2) {
    final int key = getKey(scope1);
    final int key1 = getKey(scope2);
    if (key >= 0) {
      if (key1 >= 0) {
        return key - key1;
      }
      else {
        return -1;
      }
    }
    else {
      if (key1 >= 0) {
        return 1;
      }
      else {
        return scope1.compareTo(scope2);
      }
    }
  }
}