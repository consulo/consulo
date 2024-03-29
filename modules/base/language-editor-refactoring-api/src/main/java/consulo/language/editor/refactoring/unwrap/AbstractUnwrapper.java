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
package consulo.language.editor.refactoring.unwrap;

import consulo.codeEditor.Editor;
import consulo.language.util.IncorrectOperationException;
import consulo.language.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractUnwrapper<C extends AbstractUnwrapper.AbstractContext> implements Unwrapper {
  private final String myDescription;

  public AbstractUnwrapper(String description) {
    myDescription = description;
  }

  @Override
  public abstract boolean isApplicableTo(PsiElement e);

  @Override
  public void collectElementsToIgnore(PsiElement element, Set<PsiElement> result) {
  }

  @Override
  public String getDescription(PsiElement e) {
    return myDescription;
  }

  @Override
  public PsiElement collectAffectedElements(PsiElement e, List<PsiElement> toExtract) {
    try {
      C c = createContext();
      doUnwrap(e, c);
      toExtract.addAll(c.myElementsToExtract);
      return e;
    }
    catch (IncorrectOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public List<PsiElement> unwrap(Editor editor, PsiElement element) throws IncorrectOperationException {
    C c = createContext();
    c.myIsEffective = true;
    doUnwrap(element, c);
    return c.myElementsToExtract;
  }

  protected abstract void doUnwrap(PsiElement element, C context) throws IncorrectOperationException;

  protected abstract C createContext();

  public abstract static class AbstractContext {
    protected final List<PsiElement> myElementsToExtract = new ArrayList<PsiElement>();
    protected boolean myIsEffective;

    public void addElementToExtract(PsiElement e) {
      myElementsToExtract.add(e);
    }

    public void extractElement(PsiElement element, PsiElement from) throws IncorrectOperationException {
      extract(element, element, from);
    }

    protected abstract boolean isWhiteSpace(PsiElement element);

    protected void extract(PsiElement first, PsiElement last, PsiElement from) throws IncorrectOperationException {
      // trim leading empty spaces
      while (first != last && isWhiteSpace(first)) {
        //noinspection ConstantConditions
        first = first.getNextSibling();
      }

      // trim trailing empty spaces
      while (last != first && isWhiteSpace(last)) {
        //noinspection ConstantConditions
        last = last.getPrevSibling();
      }

      // nothing to extract
      if (first == null || last == null || first == last && isWhiteSpace(last)) return;

      PsiElement toExtract = first;
      if (myIsEffective) {
        toExtract = from.getParent().addRangeBefore(first, last, from);
      }

      do {
        if (toExtract != null) {
          addElementToExtract(toExtract);
          toExtract = toExtract.getNextSibling();
        }
        first = first.getNextSibling();
      }
      while (first != null && first.getPrevSibling() != last);
    }

    public void delete(PsiElement e) throws IncorrectOperationException {
      if (myIsEffective) e.delete();
    }
    
    public void deleteExactly(PsiElement e) throws IncorrectOperationException {
      if (myIsEffective) {
        // have to use 'parent.deleteChildRange' since 'e.delete' is too smart:
        // it attempts to remove not only the element but sometimes whole expression.
        e.getParent().deleteChildRange(e, e);
      }
    }
  }
}
