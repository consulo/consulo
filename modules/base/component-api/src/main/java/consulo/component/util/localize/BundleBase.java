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
package consulo.component.util.localize;

import consulo.platform.Platform;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * @author yole
 */
public abstract class BundleBase {
  public static final char MNEMONIC = 0x1B;
  public static final String MNEMONIC_STRING = Character.toString(MNEMONIC);

  public static boolean assertKeyIsFound = false;

  @Contract("_,_,null,_ -> null; _,_,!null,_ -> !null")
  @Nullable
  public static String messageOrDefault(
    @Nullable ResourceBundle bundle,
    String key,
    @Nullable String defaultValue,
    Object... params
  ) {
    if (bundle == null) return defaultValue;

    String value;
    try {
      value = bundle.getString(key);
    }
    catch (MissingResourceException e) {
      if (defaultValue != null) {
        value = defaultValue;
      }
      else {
        value = "!" + key + "!";
        if (assertKeyIsFound) {
          assert false : "'" + key + "' is not found in " + bundle;
        }
      }
    }

    value = replaceMnemonicAmpersand(value);

    return format(value, params);
  }

  public static String format(String value, Object... params) {
    if (params.length > 0 && value.indexOf('{') >= 0) {
      return MessageFormat.format(value, params);
    }

    return value;
  }

  @Nullable
  public static String message(ResourceBundle bundle, String key, Object... params) {
    return messageOrDefault(bundle, key, null, params);
  }

  @Contract("null -> null; !null -> !null")
  @Nullable
  public static String replaceMnemonicAmpersand(@Nullable String value) {
    if (value == null) {
      return null;
    }

    if (value.indexOf('&') >= 0) {
      boolean useMacMnemonic = value.contains("&&");
      StringBuilder realValue = new StringBuilder();
      int i = 0;
      while (i < value.length()) {
        char c = value.charAt(i);
        if (c == '\\') {
          if (i < value.length() - 1 && value.charAt(i + 1) == '&') {
            realValue.append('&');
            i++;
          }
          else {
            realValue.append(c);
          }
        }
        else if (c == '&') {
          if (i < value.length() - 1 && value.charAt(i + 1) == '&') {
            if (Platform.current().os().isMac()) {
              realValue.append(MNEMONIC);
            }
            i++;
          }
          else {
            if (!Platform.current().os().isMac() || !useMacMnemonic) {
              realValue.append(MNEMONIC);
            }
          }
        }
        else {
          realValue.append(c);
        }
        i++;
      }

      return realValue.toString();
    }
    return value;
  }
}
