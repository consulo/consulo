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

/**
 * Represents ArrangementSettingsToken designed for use with standard GUI, i.e. a token that knows its UI role.
 * @author Roman.Shein
 * @since 2013-07-19
 */
public class StdArrangementSettingsToken extends ArrangementSettingsToken {

  
  private final StdArrangementTokenType myTokenType;

  
  public static StdArrangementSettingsToken tokenById(String id,
                                                      StdArrangementTokenType tokenType) {
    return new StdArrangementSettingsToken(id, id.toLowerCase().replace("_", " "), tokenType);
  }

  
  public static StdArrangementSettingsToken token(String id,
                                                  String name,
                                                  StdArrangementTokenType tokenType) {
    return new StdArrangementSettingsToken(id, name, tokenType);
  }

  
  public static StdArrangementSettingsToken tokenByBundle(String id,
                                                          @PropertyKey(resourceBundle = CodeStyleBundle.BUNDLE) String key,
                                                          StdArrangementTokenType tokenType) {
    return new StdArrangementSettingsToken(id, CodeStyleBundle.message(key), tokenType);
  }

  
  public StdArrangementTokenType getTokenType() {
    return myTokenType;
  }

  protected StdArrangementSettingsToken(String id,
                                        String uiName,
                                        StdArrangementTokenType tokenType) {
    super(id, uiName);
    myTokenType = tokenType;
  }
}
