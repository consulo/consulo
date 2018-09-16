/*
 * Copyright 2013-2016 consulo.io
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

import com.intellij.lang.LanguageExtension;

/**
 * @author VISTALL
 * @since 18:04/30.05.13
 */
public class LanguageVersionResolvers extends LanguageExtension<LanguageVersionResolver> {
  public static final LanguageVersionResolvers INSTANCE = new LanguageVersionResolvers();

  private LanguageVersionResolvers() {
    super("com.intellij.lang.versionResolver", LanguageVersionResolver.DEFAULT);
  }
}
