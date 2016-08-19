/*
 * Copyright 2013 must-be.org
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
package consulo.lang;

import com.intellij.lang.Language;
import consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 18:31/31.08.13
 */
public class LanguagePointerUtil {
  private static final Map<String, NamedPointer<Language>> ourPointersCache = new HashMap<String, NamedPointer<Language>>();

  @NotNull
  public static NamedPointer<Language> createPointer(@NotNull String name) {
    NamedPointer<Language> languageNamedPointer = ourPointersCache.get(name);
    if(languageNamedPointer != null) {
      return languageNamedPointer;
    }
    languageNamedPointer = new LanguagePointerImpl(name);
    ourPointersCache.put(name, languageNamedPointer);
    return languageNamedPointer;
  }
}
