/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.ui.console;

import consulo.util.lang.Pair;

import jakarta.annotation.Nullable;
import java.util.List;

public interface InputFilter {

  /**
   * @param text        the text to be filtered.
   * @param contentType the content type of filtered text
   * @return            <tt>null</tt>, if there was no match, otherwise, a list of pairs like ('string to use', 'content type to use')
   */
  @Nullable
  List<Pair<String, ConsoleViewContentType>> applyFilter(String text, ConsoleViewContentType contentType);

}
