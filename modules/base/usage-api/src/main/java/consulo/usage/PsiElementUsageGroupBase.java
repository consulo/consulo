/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataSink;
import consulo.language.editor.util.NavigationItemFileStatus;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.navigation.NavigationItem;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.status.FileStatus;

import jakarta.annotation.Nonnull;

/**
 * @author Maxim.Mossienko
 */
public class PsiElementUsageGroupBase<T extends PsiElement & NavigationItem> implements UsageGroup, NamedPresentably {
  private final SmartPsiElementPointer myElementPointer;
  private final String myName;
  private final Image myIcon;

  public PsiElementUsageGroupBase(@Nonnull T element, Image icon) {
    String myName = element.getName();
    if (myName == null) myName = "<anonymous>";
    this.myName = myName;
    myElementPointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);

    myIcon = icon;
  }

  @RequiredReadAction
  public PsiElementUsageGroupBase(@Nonnull T element) {
    this(element, IconDescriptorUpdaters.getIcon(element, 0));
  }

  @Override
  public Image getIcon() {
    return myIcon;
  }

  @RequiredReadAction
  @SuppressWarnings("unchecked")
  public T getElement() {
    return (T)myElementPointer.getElement();
  }

  @Override
  @Nonnull
  public String getText(UsageView view) {
    return myName;
  }

  @Override
  @RequiredReadAction
  public FileStatus getFileStatus() {
    return isValid() ? NavigationItemFileStatus.get(getElement()) : null;
  }

  @Override
  @RequiredReadAction
  public boolean isValid() {
    final T element = getElement();
    return element != null && element.isValid();
  }

  @Override
  @RequiredReadAction
  public void navigate(boolean focus) throws UnsupportedOperationException {
    if (canNavigate()) {
      getElement().navigate(focus);
    }
  }

  @Override
  @RequiredReadAction
  public boolean canNavigate() {
    return isValid();
  }

  @Override
  @RequiredReadAction
  public boolean canNavigateToSource() {
    return canNavigate();
  }

  @Override
  public void update() {
  }

  @Override
  public int compareTo(@Nonnull final UsageGroup o) {
    String name;
    if (o instanceof NamedPresentably) {
      name = ((NamedPresentably)o).getPresentableName();
    } else {
      name = o.getText(null);
    }
    return myName.compareToIgnoreCase(name);
  }

  @RequiredReadAction
  public boolean equals(final Object obj) {
    if (!(obj instanceof PsiElementUsageGroupBase)) return false;
    PsiElementUsageGroupBase group = (PsiElementUsageGroupBase)obj;
    if (isValid() && group.isValid()) {
      return getElement().getManager().areElementsEquivalent(getElement(), group.getElement());
    }
    return Comparing.equal(myName, ((PsiElementUsageGroupBase)obj).myName);
  }

  public int hashCode() {
    return myName.hashCode();
  }

  @RequiredReadAction
  public void calcData(final Key<?> key, final DataSink sink) {
    if (!isValid()) return;
    if (PsiElement.KEY == key) {
      sink.put(PsiElement.KEY, getElement());
    }
    if (UsageView.USAGE_INFO_KEY == key) {
      T element = getElement();
      if (element != null) {
        sink.put(UsageView.USAGE_INFO_KEY, new UsageInfo(element));
      }
    }
  }

  @Override
  @Nonnull
  public String getPresentableName() {
    return myName;
  }
}
