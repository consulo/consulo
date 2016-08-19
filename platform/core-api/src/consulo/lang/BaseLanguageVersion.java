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
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 13:23/25.08.13
 */
public class BaseLanguageVersion<T extends Language> implements LanguageVersion<T> {
  private final String myName;
  private final T myLanguage;

  public BaseLanguageVersion(String name, T language) {
    myName = name;
    myLanguage = language;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public T getLanguage() {
    return myLanguage;
  }

  @Override
  public String toString() {
    return "LanguageVersion: " + getName() + " for language: " + getLanguage();
  }
}
