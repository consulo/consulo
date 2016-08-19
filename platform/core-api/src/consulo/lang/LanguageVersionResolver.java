package consulo.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 18:05/30.05.13
 */
public interface LanguageVersionResolver<T extends Language> {
  LanguageVersionResolver DEFAULT = new LanguageVersionResolver() {
    @NotNull
    @Override
    public LanguageVersion getLanguageVersion(@NotNull Language language, @Nullable PsiElement element) {
      final LanguageVersion[] versions = language.getVersions();
      for (LanguageVersion version : versions) {
        if(version instanceof LanguageVersionWithDefinition && ((LanguageVersionWithDefinition)version).isMyElement(element)) {
          return version;
        }
      }
      return versions[0];
    }

    @Override
    public LanguageVersion getLanguageVersion(@NotNull Language language, @Nullable Project project, @Nullable VirtualFile virtualFile) {
      final LanguageVersion[] versions = language.getVersions();
      for (LanguageVersion version : versions) {
        if(version instanceof LanguageVersionWithDefinition && ((LanguageVersionWithDefinition)version).isMyFile(project, virtualFile)) {
          return version;
        }
      }
      return versions[0];
    }
  };
  @NotNull
  LanguageVersion<T> getLanguageVersion(@NotNull Language language, @Nullable PsiElement element);

  LanguageVersion<T> getLanguageVersion(@NotNull Language language, @Nullable Project project, @Nullable VirtualFile virtualFile);
}
