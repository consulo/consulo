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

package consulo.language.editor.refactoring.ui;

import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewDescriptor;
import consulo.usage.localize.UsageLocalize;

/**
 * @author dsl
 */
public abstract class UsageViewDescriptorAdapter implements UsageViewDescriptor {

  @Override
  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return UsageLocalize.referencesToBeChanged(UsageViewBundle.getReferencesString(usagesCount, filesCount)).get();
  }

  @Override
  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return null;
  }

}
