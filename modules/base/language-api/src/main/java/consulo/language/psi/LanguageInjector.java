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

package consulo.language.psi;

import consulo.component.extension.ExtensionPointName;
import consulo.document.util.TextRange;
import consulo.language.Language;

import javax.annotation.Nonnull;

/**
 * Describes logic for injecting language inside hosting PSI element.
 * E.g. "inject XPath language into all XML attributes named 'select' that sit inside XML tag prefixed with 'xsl:'".
 * @see PsiLanguageInjectionHost
 */
public interface LanguageInjector {
  ExtensionPointName<LanguageInjector> EXTENSION_POINT_NAME = ExtensionPointName.create("consulo.languageInjector");

  /**
   * @param host PSI element inside which your language will be injected.
   * @param injectionPlacesRegistrar stores places where injection occurs. <br>
   *        Call its {@link com.intellij.psi.InjectedLanguagePlaces#addPlace(Language, TextRange, String, String)}
   *        method to register particular injection place.
   *        For example, to inject your language in string literal inside quotes, you might want to <br>
   *        <code>injectionPlacesRegistrar.addPlace(myLanguage, new TextRange(1,host.getTextLength()-1))</code>
   */
  void getLanguagesToInject(@Nonnull PsiLanguageInjectionHost host, @Nonnull InjectedLanguagePlaces injectionPlacesRegistrar);
}
