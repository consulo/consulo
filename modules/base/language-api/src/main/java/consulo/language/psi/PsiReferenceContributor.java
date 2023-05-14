package consulo.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Via implementing this extension it's possible to provide references ({@link PsiReference}) to
 * PSI elements which support that. Such known elements include: XML tags and attribute values, Java/Python/Javascript
 * literal expressions, comments etc. The reference contributors are run once per project and are able to
 * register reference providers for specific locations. See {@link PsiReferenceRegistrar} for more details.
 * <p>
 * The contributed references may then be obtained via
 * {@link PsiReferenceService#getReferences(PsiElement, PsiReferenceService.Hints)},
 * which is the preferred way.
 * Some elements return them from {@link PsiElement#getReferences()} directly though, but one should not rely on that
 * behavior since it may be changed in the future.
 * <p>
 * Note that, if you're implementing a custom language, it won't by default support references registered through PsiReferenceContributor.
 * If you want to support that, you need to call
 * {@link ReferenceProvidersRegistry#getReferencesFromProviders(PsiElement)} from your implementation
 * of PsiElement.getReferences().
 * <p>
 * The alternative way to register {@link PsiReferenceProvider} is by using {@link PsiReferenceProviderBean}.
 *
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PsiReferenceContributor implements LanguageExtension {
  private static final ExtensionPointCacheKey<PsiReferenceContributor, ByLanguageValue<List<PsiReferenceContributor>>> KEY =
          ExtensionPointCacheKey.create("PsiReferenceContributor", LanguageOneToMany.build(true));

  @Nonnull
  public static List<PsiReferenceContributor> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(PsiReferenceContributor.class).getOrBuildCache(KEY).requiredGet(language);
  }

  public abstract void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar);
}
