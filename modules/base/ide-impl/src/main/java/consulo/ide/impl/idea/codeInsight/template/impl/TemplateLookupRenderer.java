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

package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.LookupItem;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.completion.lookup.ElementLookupRenderer;
import consulo.language.editor.template.Template;

/**
 * @author yole
 */
@ExtensionImpl
public class TemplateLookupRenderer implements ElementLookupRenderer<Template> {
  @Override
  public boolean handlesItem(final Object element) {
    return element instanceof Template;
  }

  @Override
  public void renderElement(final LookupItem item, final Template element, final LookupElementPresentation presentation) {
    presentation.setItemText(element.getKey());
    presentation.setTypeText(element.getDescription());
  }

}
