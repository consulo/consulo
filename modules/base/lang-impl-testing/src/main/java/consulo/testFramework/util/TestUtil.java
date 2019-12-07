/*
 * Copyright 2013-2019 consulo.io
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
package consulo.testFramework.util;

import com.intellij.openapi.util.text.StringUtil;
import junit.framework.TestCase;

/**
 * @author VISTALL
 * @since 2019-12-07
 */
public class TestUtil {
  public static String getTestName(TestCase testCase, boolean lowercaseFirstLetter) {
    String name = testCase.getName();
    return getTestName(name, lowercaseFirstLetter);
  }

  public static String getTestName(String name, boolean lowercaseFirstLetter) {
    if (name == null) {
      return "";
    }
    name = StringUtil.trimStart(name, "test");
    if (StringUtil.isEmpty(name)) {
      return "";
    }
    return lowercaseFirstLetter(name, lowercaseFirstLetter);
  }

  public static String lowercaseFirstLetter(String name, boolean lowercaseFirstLetter) {
    if (lowercaseFirstLetter && !isAllUppercaseName(name)) {
      name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    return name;
  }

  public static boolean isAllUppercaseName(String name) {
    int uppercaseChars = 0;
    for (int i = 0; i < name.length(); i++) {
      if (Character.isLowerCase(name.charAt(i))) {
        return false;
      }
      if (Character.isUpperCase(name.charAt(i))) {
        uppercaseChars++;
      }
    }
    return uppercaseChars >= 3;
  }
}
