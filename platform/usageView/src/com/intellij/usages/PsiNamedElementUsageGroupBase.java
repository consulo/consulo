package com.intellij.usages;

import consulo.ide.IconDescriptorUpdaters;
import com.intellij.navigation.NavigationItem;
import com.intellij.navigation.NavigationItemFileStatus;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Maxim.Mossienko
 */
public class PsiNamedElementUsageGroupBase<T extends PsiNamedElement & NavigationItem> implements UsageGroup, NamedPresentably {
  private final SmartPsiElementPointer myElementPointer;
  private final String myName;
  private final Icon myIcon;

  public PsiNamedElementUsageGroupBase(@NotNull T element, Icon icon) {
    String myName = element.getName();
    if (myName == null) myName = "<anonymous>";
    this.myName = myName;
    myElementPointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);

    myIcon = icon;
  }

  public PsiNamedElementUsageGroupBase(@NotNull T element) {
    this(element, IconDescriptorUpdaters.getIcon(element, 0));
  }

  @Override
  public Icon getIcon(boolean isOpen) {
    return myIcon;
  }

  public T getElement() {
    return (T)myElementPointer.getElement();
  }

  @Override
  @NotNull
  public String getText(UsageView view) {
    return myName;
  }

  @Override
  public FileStatus getFileStatus() {
    return isValid() ? NavigationItemFileStatus.get(getElement()) : null;
  }

  @Override
  public boolean isValid() {
    final T element = getElement();
    return element != null && element.isValid();
  }

  @Override
  public void navigate(boolean focus) throws UnsupportedOperationException {
    if (canNavigate()) {
      getElement().navigate(focus);
    }
  }

  @Override
  public boolean canNavigate() {
    return isValid();
  }

  @Override
  public boolean canNavigateToSource() {
    return canNavigate();
  }

  @Override
  public void update() {
  }

  @Override
  public int compareTo(final UsageGroup o) {
    String name;
    if (o instanceof NamedPresentably) {
      name = ((NamedPresentably)o).getPresentableName();
    } else {
      name = o.getText(null);
    }
    return myName.compareToIgnoreCase(name);
  }

  public boolean equals(final Object obj) {
    if (!(obj instanceof PsiNamedElementUsageGroupBase)) return false;
    PsiNamedElementUsageGroupBase group = (PsiNamedElementUsageGroupBase)obj;
    if (isValid() && group.isValid()) {
      return getElement().getManager().areElementsEquivalent(getElement(), group.getElement());
    }
    return Comparing.equal(myName, ((PsiNamedElementUsageGroupBase)obj).myName);
  }

  public int hashCode() {
    return myName.hashCode();
  }

  public void calcData(final DataKey key, final DataSink sink) {
    if (!isValid()) return;
    if (LangDataKeys.PSI_ELEMENT == key) {
      sink.put(LangDataKeys.PSI_ELEMENT, getElement());
    }
    if (UsageView.USAGE_INFO_KEY == key) {
      T element = getElement();
      if (element != null) {
        sink.put(UsageView.USAGE_INFO_KEY, new UsageInfo(element));
      }
    }
  }

  @Override
  @NotNull
  public String getPresentableName() {
    return myName;
  }
}
