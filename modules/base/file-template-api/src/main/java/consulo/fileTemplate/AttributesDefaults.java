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

package consulo.fileTemplate;

import consulo.document.util.TextRange;
import consulo.util.lang.Pair;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Roman Chernyatchik
 */
public class AttributesDefaults {
  private final String myDefaultName;
  private final TextRange myDefaultRange;
  private final Map<String, Pair<String, TextRange>> myNamesToValueAndRangeMap = new HashMap<>();
  private Map<String, Object> myDefaultProperties = null;
  private boolean myFixedName;

  public AttributesDefaults(@Nullable String defaultName, @Nullable TextRange defaultRange) {
    myDefaultName = defaultName;
    myDefaultRange = defaultRange;
  }

  public AttributesDefaults(@Nullable String defaultName) {
    this(defaultName, null);
  }

  public AttributesDefaults() {
    this(null, null);
  }

  @Nullable
  public String getDefaultFileName() {
    return myDefaultName;
  }

  @Nullable
  public TextRange getDefaultFileNameSelection() {
    return myDefaultRange;
  }

  public void add(@NonNls @Nonnull String attributeKey, @NonNls @Nonnull String value, @Nullable TextRange selectionRange) {
    myNamesToValueAndRangeMap.put(attributeKey, new Pair<>(value, selectionRange));
  }

  public void add(@NonNls @Nonnull String attributeKey, @NonNls @Nonnull String value) {
    add(attributeKey, value, null);
  }

  public void addPredefined(@Nonnull String key, @Nonnull String value) {
    if (myDefaultProperties == null) {
      myDefaultProperties = new HashMap<>();
    }
    myDefaultProperties.put(key, value);
  }

  @Nullable
  public Map<String, Object> getDefaultProperties() {
    return myDefaultProperties;
  }

  @Nullable
  public TextRange getRangeFor(@NonNls @Nonnull String attributeKey) {
    Pair<String, TextRange> valueAndRange = myNamesToValueAndRangeMap.get(attributeKey);
    return valueAndRange == null ? null : valueAndRange.second;
  }

  @Nullable
  public String getDefaultValueFor(@NonNls @Nonnull String attributeKey) {
    Pair<String, TextRange> valueAndRange = myNamesToValueAndRangeMap.get(attributeKey);
    return valueAndRange == null ? null : valueAndRange.first;
  }

  public boolean isFixedName() {
    return myFixedName;
  }

  public AttributesDefaults withFixedName(boolean fixedName) {
    myFixedName = fixedName;
    return this;
  }
}
