package consulo.webBrowser;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.io.Url;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public abstract class OpenInBrowserRequest {
  private Collection<Url> result;
  protected PsiFile file;

  public OpenInBrowserRequest(@Nonnull PsiFile file) {
    this.file = file;
  }

  public OpenInBrowserRequest() {
  }

  @Nullable
  public static OpenInBrowserRequest create(@Nonnull final PsiElement element) {
    PsiFile psiFile = element.isValid() ? element.getContainingFile() : null;
    if (psiFile == null || psiFile.getVirtualFile() == null) {
      return null;
    }

    return new OpenInBrowserRequest(psiFile) {
      @Nonnull
      @Override
      public PsiElement getElement() {
        return element;
      }
    };
  }

  @Nonnull
  public PsiFile getFile() {
    return file;
  }

  @Nonnull
  public VirtualFile getVirtualFile() {
    return file.getVirtualFile();
  }

  @Nonnull
  public Project getProject() {
    return file.getProject();
  }

  @Nonnull
  public abstract PsiElement getElement();

  public void setResult(@Nonnull Collection<Url> result) {
    this.result = result;
  }

  @Nullable
  public Collection<Url> getResult() {
    return result;
  }
}