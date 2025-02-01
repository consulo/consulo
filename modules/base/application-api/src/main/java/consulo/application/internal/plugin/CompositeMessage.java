/*
 * Copyright 2013-2022 consulo.io
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
package consulo.application.internal.plugin;

import consulo.localize.LocalizeValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 30-Aug-22
 *
 * TODO replace this impl by consulo.localize.LocalizeValue#join(consulo.localize.LocalizeValue...)
 */
public class CompositeMessage {
  private final List<Object> myParts = new ArrayList<>();

  public CompositeMessage append(String value) {
    myParts.add(value);
    return this;
  }

  public CompositeMessage append(LocalizeValue value) {
    myParts.add(value);
    return this;
  }

  public boolean isEmpty() {
    return myParts.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Object part : myParts) {
      if (part instanceof LocalizeValue) {
        builder.append(((LocalizeValue)part).get());
      }
      else {
        builder.append(part.toString());
      }
    }
    return builder.toString();
  }
}
