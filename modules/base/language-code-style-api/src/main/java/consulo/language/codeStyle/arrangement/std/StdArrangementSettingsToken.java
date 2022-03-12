/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.codeStyle.arrangement.std;

import consulo.language.codeStyle.CodeStyleBundle;
import org.jetbrains.annotations.PropertyKey;

import javax.annotation.Nonnull;

/**
 * Represents ArrangementSettingsToken designed for use with standard GUI, i.e. a token that knows its UI role.
 * @author Roman.Shein
 * Date: 19.07.13
 */
public class StdArrangementSettingsToken extends ArrangementSettingsToken {

  @Nonnull
  private final StdArrangementTokenType myTokenType;

  @Nonnull
  public static StdArrangementSettingsToken tokenById(@Nonnull String id,
                                                      @Nonnull StdArrangementTokenType tokenType) {
    return new StdArrangementSettingsToken(id, id.toLowerCase().replace("_", " "), tokenType);
  }

  @Nonnull
  public static StdArrangementSettingsToken token(@Nonnull String id,
                                                  @Nonnull String name,
                                                  @Nonnull StdArrangementTokenType tokenType) {
    return new StdArrangementSettingsToken(id, name, tokenType);
  }

  @Nonnull
  public static StdArrangementSettingsToken tokenByBundle(@Nonnull String id,
                                                          @Nonnull @PropertyKey(resourceBundle = CodeStyleBundle.BUNDLE) String key,
                                                          @Nonnull StdArrangementTokenType tokenType) {
    return new StdArrangementSettingsToken(id, CodeStyleBundle.message(key), tokenType);
  }

  @Nonnull
  public StdArrangementTokenType getTokenType() {
    return myTokenType;
  }

  protected StdArrangementSettingsToken(@Nonnull String id,
                                        @Nonnull String uiName,
                                        @Nonnull StdArrangementTokenType tokenType) {
    super(id, uiName);
    myTokenType = tokenType;
  }
}
