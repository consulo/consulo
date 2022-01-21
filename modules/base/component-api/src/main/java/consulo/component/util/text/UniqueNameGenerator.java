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
package consulo.component.util.text;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author peter
 */
public class UniqueNameGenerator implements Predicate<String> {
  private final Set<String> myExistingNames = new HashSet<>();

  public <T> UniqueNameGenerator(final Collection<T> elements, @Nullable Function<T, String> namer) {
    for (final T t : elements) {
      addExistingName(namer != null ? namer.apply(t) : t.toString());
    }
  }

  public UniqueNameGenerator() {
  }

  @Override
  public final boolean test(final String candidate) {
    return !myExistingNames.contains(candidate);
  }

  public final boolean isUnique(final String name, String prefix, String suffix) {
    return test(prefix + name + suffix);
  }

  public static String generateUniqueName(final String defaultName, final Collection<String> existingNames) {
    return generateUniqueName(defaultName, "", "", existingNames);
  }

  public static String generateUniqueName(final String defaultName, final String prefix, final String suffix, final Collection<String> existingNames) {
    return generateUniqueName(defaultName, prefix, suffix, s -> !existingNames.contains(s));
  }
  
  public static String generateUniqueName(final String defaultName, final Predicate<String> validator) {
    return generateUniqueName(defaultName, "", "", validator);
  }

  public static String generateUniqueName(final String defaultName, final String prefix, final String suffix, final Predicate<String> validator) {
    return generateUniqueName(defaultName, prefix, suffix, "", "", validator);
  }

  public static String generateUniqueName(final String defaultName, final String prefix, final String suffix,
                                          final String beforeNumber, final String afterNumber, final Predicate<String> validator) {
    final String defaultFullName = prefix + defaultName + suffix;
    if (validator.test(defaultFullName)) {
      return defaultFullName;
    }

    for (int i = 2; ; i++) {
      final String fullName = prefix + defaultName + beforeNumber + i + afterNumber + suffix;
      if (validator.test(fullName)) {
        return fullName;
      }
    }
  }

  public String generateUniqueName(final String defaultName, final String prefix, final String suffix) {
    return generateUniqueName(defaultName, prefix, suffix, "", "");
  }

  public String generateUniqueName(final String defaultName, final String prefix, final String suffix, final String beforeNumber, final String afterNumber) {
    final String result = generateUniqueName(defaultName, prefix, suffix, beforeNumber, afterNumber, this);
    addExistingName(result);
    return result;
  }

  public void addExistingName(String result) {
    myExistingNames.add(result);
  }

  public String generateUniqueName(final String defaultName) {
    return generateUniqueName(defaultName, "", "");
  }

}
