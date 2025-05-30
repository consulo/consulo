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
package consulo.language.editor.completion.lookup;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;

import jakarta.annotation.Nullable;

/*
 * @author mike
 * @since 2002-07-23
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CharFilter {
  public static final ExtensionPointName<CharFilter> EP_NAME = ExtensionPointName.create(CharFilter.class);

  public static enum Result {
    ADD_TO_PREFIX, SELECT_ITEM_AND_FINISH_LOOKUP, HIDE_LOOKUP
  }

  /**
   * Informs about further action on typing character c when completion lookup has specified prefix. If
   * @param c character being inserted
   * @param prefixLength
   * @param lookup
   * @return further action or null, which indicates that some other {@link CharFilter}
   * should handle this char. Default char filter handles common cases like finishing with ' ', '(', ';', etc.  
   */
  @Nullable
  public abstract Result acceptChar(char c, int prefixLength, Lookup lookup);
}
