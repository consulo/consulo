package consulo.codeInsight.navigation.actions;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 02.05.2015
 */
public interface GotoDeclarationHandlerEx extends GotoDeclarationHandler {
  @NotNull
  PsiElementListCellRenderer<PsiElement> createRender(@NotNull PsiElement[] elements);
}
