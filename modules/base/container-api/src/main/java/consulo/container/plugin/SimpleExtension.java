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
package consulo.container.plugin;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 21-Sep-16
 */
public final class SimpleExtension {
  private final String myKey;
  private final String[] myValues;

  public SimpleExtension(String key, String[] values) {
    myKey = key;
    myValues = values;
  }

  @Nonnull
  public String getKey() {
    return myKey;
  }

  @Nonnull
  public String[] getValues() {
    return myValues;
  }
}
