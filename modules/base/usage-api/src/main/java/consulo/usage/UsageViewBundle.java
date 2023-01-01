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

package consulo.usage;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author yole
 */
public class UsageViewBundle extends AbstractBundle {
  private static UsageViewBundle ourInstance = new UsageViewBundle();

  private static final String BUNDLE = "consulo.usage.UsageView";

  private UsageViewBundle() {
    super(BUNDLE);
  }

  public static String getUsagesString(int usagesCount, int filesCount) {
    return " (" + message("occurence.info.usage", usagesCount, filesCount) + ")";
  }

  public static String getOccurencesString(int usagesCount, int filesCount) {
    return " (" + message("occurence.info.occurence", usagesCount, filesCount) + ")";
  }

  public static String getReferencesString(int usagesCount, int filesCount) {
    return " (" + message("occurence.info.reference", usagesCount, filesCount) + ")";
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return ourInstance.getMessage(key, params);
  }
}
