/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.template;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.completion.lookup.RealLookupElementPresentation;
import consulo.language.editor.internal.TemplateConstants;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.event.KeyEvent;

/**
 * @author peter
 */
abstract public class LiveTemplateLookupElement extends LookupElement {
  private final String myLookupString;
  public final boolean sudden;
  private final boolean myWorthShowingInAutoPopup;
  private final String myDescription;

  public LiveTemplateLookupElement(@Nonnull String lookupString, @Nullable String description, boolean sudden, boolean worthShowingInAutoPopup) {
    myDescription = description;
    this.sudden = sudden;
    myLookupString = lookupString;
    myWorthShowingInAutoPopup = worthShowingInAutoPopup;
  }

  @Nonnull
  @Override
  public String getLookupString() {
    return myLookupString;
  }

  @Nonnull
  protected String getItemText() {
    return myLookupString;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    super.renderElement(presentation);
    char shortcut = getTemplateShortcut();
    presentation.setItemText(getItemText());
    if (sudden) {
      presentation.setItemTextBold(true);
      if (!presentation.isReal() || !((RealLookupElementPresentation)presentation).isLookupSelectionTouched()) {
        if (shortcut == TemplateConstants.DEFAULT_CHAR) {
          shortcut = TemplateSettings.getInstance().getDefaultShortcutChar();
        }
        presentation.setTypeText("  [" + KeyEvent.getKeyText(shortcut) + "] ");
      }
      if (StringUtil.isNotEmpty(myDescription)) {
        presentation.setTailText(" (" + myDescription + ")", true);
      }
    }
    else {
      presentation.setTypeText(myDescription);
    }
  }

  @Override
  public boolean isWorthShowingInAutoPopup() {
    return myWorthShowingInAutoPopup;
  }

  public abstract char getTemplateShortcut();
}
