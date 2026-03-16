package consulo.webBrowser;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.io.Url;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

public abstract class OpenInBrowserRequest {
    private Collection<Url> result;
    protected PsiFile file;

    public OpenInBrowserRequest(PsiFile file) {
        this.file = file;
    }

    public OpenInBrowserRequest() {
    }

    @Nullable
    public static OpenInBrowserRequest create(final PsiElement element) {
        PsiFile psiFile = element.isValid() ? element.getContainingFile() : null;
        if (psiFile == null || psiFile.getVirtualFile() == null) {
            return null;
        }

        return new OpenInBrowserRequest(psiFile) {
            
            @Override
            public PsiElement getElement() {
                return element;
            }
        };
    }

    
    public PsiFile getFile() {
        return file;
    }

    
    public VirtualFile getVirtualFile() {
        return file.getVirtualFile();
    }

    
    public Project getProject() {
        return file.getProject();
    }

    
    public abstract PsiElement getElement();

    public void setResult(Collection<Url> result) {
        this.result = result;
    }

    @Nullable
    public Collection<Url> getResult() {
        return result;
    }
}