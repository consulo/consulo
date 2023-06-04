package consulo.language.editor.ui;

import consulo.annotation.UsedInPlugin;
import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
* @author yole
*/
@UsedInPlugin
public class DefaultGutterIconNavigationHandler<T extends PsiElement> implements GutterIconNavigationHandler<T> {
  private final Collection<? extends NavigatablePsiElement> myReferences;
  private final String myTitle;

  public DefaultGutterIconNavigationHandler(Collection<? extends NavigatablePsiElement> references, String title) {
    myReferences = references;
    myTitle = title;
  }

  public Collection<? extends NavigatablePsiElement> getReferences() {
    return myReferences;
  }

  @RequiredUIAccess
  @Override
  public void navigate(MouseEvent e, T elt) {
    PsiElementListNavigator.openTargets(e,
                                        myReferences.toArray(new NavigatablePsiElement[myReferences.size()]),
                                        myTitle, null, createListCellRenderer());
  }

  protected ListCellRenderer createListCellRenderer() {
    return new DefaultPsiElementCellRenderer();
  }
}
