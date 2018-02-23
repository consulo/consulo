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
package consulo.codeInsight;

import javax.annotation.Nonnull;

import java.util.Set;

/**
 * @author VISTALL
 * @since 20.04.2015
 */
public class TargetElementUtilExImpl extends TargetElementUtilEx.Adapter {
  @Override
  public void collectAllAccepted(@Nonnull Set<String> set) {
    set.add(REFERENCED_ELEMENT_ACCEPTED);
    set.add(ELEMENT_NAME_ACCEPTED);
    set.add(LOOKUP_ITEM_ACCEPTED);
  }

  @Override
  public void collectDefinitionSearchFlags(@Nonnull Set<String> set) {
    set.add(REFERENCED_ELEMENT_ACCEPTED);
    set.add(ELEMENT_NAME_ACCEPTED);
    set.add(LOOKUP_ITEM_ACCEPTED);
  }

  @Override
  public void collectReferenceSearchFlags(@Nonnull Set<String> set) {
    set.add(REFERENCED_ELEMENT_ACCEPTED);
    set.add(ELEMENT_NAME_ACCEPTED);
    set.add(LOOKUP_ITEM_ACCEPTED);
  }
}
