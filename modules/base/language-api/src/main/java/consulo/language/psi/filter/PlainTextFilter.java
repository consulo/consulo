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
package consulo.language.psi.filter;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import org.jspecify.annotations.Nullable;

/**
 * @author yole
 */
public class PlainTextFilter implements ElementFilter {
  protected final @Nullable String[] myValue;
  protected boolean myCaseInsensitiveFlag = false;

  public PlainTextFilter(String value, boolean insensitiveFlag) {
    myCaseInsensitiveFlag = insensitiveFlag;
    myValue = new String[1];
    myValue[0] = value;
  }

  public PlainTextFilter(String... values) {
    myValue = values;
  }

  public PlainTextFilter(String value1, String value2) {
    myValue = new String[2];
    myValue[0] = value1;
    myValue[1] = value2;
  }

  @Override
  public boolean isClassAcceptable(Class hintClass) {
    return true;
  }

  @Override
  public boolean isAcceptable(Object element, @Nullable PsiElement context) {
    if (element != null) {
      for (String value : myValue) {
        if (value == null) {
          return true;
        }
        String elementText = getTextByElement(element);
        if (myCaseInsensitiveFlag) {
          if (value.equalsIgnoreCase(elementText)) return true;
        }
        else {
          if (value.equals(elementText)) return true;
        }
      }
    }

    return false;
  }

  @Override
  public String toString() {
    String ret = "(";
    for (int i = 0; i < myValue.length; i++) {
      ret += myValue[i];
      if (i < myValue.length - 1) {
        ret += " | ";
      }
    }
    ret += ")";
    return ret;
  }

  @RequiredReadAction
  protected @Nullable String getTextByElement(Object element) {
    if (element instanceof PsiNamedElement namedElem) {
      return namedElem.getName();
    }
    else if (element instanceof PsiElement psiElem) {
      return psiElem.getText();
    }
    return null;
  }
}
