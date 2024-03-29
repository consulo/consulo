/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.psi.stub;

import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternProvider;

public class IndexPatternUtil {
  public static int getIndexPatternCount() {
    int patternsCount = 0;
    for(IndexPatternProvider provider: IndexPatternProvider.EP_NAME.getExtensionList()) {
      patternsCount += provider.getIndexPatterns().length;
    }
    return patternsCount;
  }

  public static IndexPattern[] getIndexPatterns() {
    IndexPattern[] result = new IndexPattern[getIndexPatternCount()];
    int destIndex = 0;
    for(IndexPatternProvider provider: IndexPatternProvider.EP_NAME.getExtensionList()) {
      for(IndexPattern pattern: provider.getIndexPatterns()) {
        result [destIndex++] = pattern;
      }
    }
    return result;
  }
}
