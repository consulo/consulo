/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.util;

import consulo.util.lang.Couple;
import consulo.versionControlSystem.log.VcsUser;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VcsUserUtil {
  
  private static final Pattern NAME_PATTERN = Pattern.compile("(\\w+)[\\W_](\\w+)");
  
  private static final Pattern PRINTABLE_ASCII_PATTERN = Pattern.compile("[ -~]*");

  
  public static String toExactString(VcsUser user) {
    return getString(user.getName(), user.getEmail());
  }

  
  private static String getString(String name, String email) {
    if (name.isEmpty()) return email;
    if (email.isEmpty()) return name;
    return name + " <" + email + ">";
  }

  public static boolean isSamePerson(VcsUser user1, VcsUser user2) {
    return getNameInStandardForm(getName(user1)).equals(getNameInStandardForm(getName(user2)));
  }

  
  public static String getShortPresentation(VcsUser user) {
    return getName(user);
  }

  
  private static String getName(VcsUser user) {
    return getUserName(user.getName(), user.getEmail());
  }

  
  public static String getUserName(String name, String email) {
    if (!name.isEmpty()) return name;
    String emailNamePart = getNameFromEmail(email);
    if (emailNamePart != null) return emailNamePart;
    return email;
  }

  @Nullable
  public static String getNameFromEmail(String email) {
    int at = email.indexOf('@');
    String emailNamePart = null;
    if (at > 0) {
      emailNamePart = email.substring(0, at);
    }
    return emailNamePart;
  }

  
  public static String getNameInStandardForm(String name) {
    Couple<String> firstAndLastName = getFirstAndLastName(name);
    if (firstAndLastName != null) {
      return firstAndLastName.first.toLowerCase(Locale.ENGLISH) + " " + firstAndLastName.second.toLowerCase(Locale.ENGLISH); // synonyms detection is currently english-only
    }
    return nameToLowerCase(name);
  }

  @Nullable
  public static Couple<String> getFirstAndLastName(String name) {
    Matcher matcher = NAME_PATTERN.matcher(name);
    if (matcher.matches()) {
      return Couple.of(matcher.group(1), matcher.group(2));
    }
    return null;
  }

  
  public static String nameToLowerCase(String name) {
    if (!PRINTABLE_ASCII_PATTERN.matcher(name).matches()) return name;
    return name.toLowerCase(Locale.ENGLISH);
  }

  
  public static String capitalizeName(String name) {
    if (name.isEmpty()) return name;
    if (!PRINTABLE_ASCII_PATTERN.matcher(name).matches()) return name;
    return name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
  }

  
  public static String emailToLowerCase(String email) {
    return email.toLowerCase(Locale.ENGLISH);
  }
}
