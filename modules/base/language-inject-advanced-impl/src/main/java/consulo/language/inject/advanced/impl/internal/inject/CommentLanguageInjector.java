package consulo.language.inject.advanced.impl.internal.inject;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.inject.MultiHostInjector;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.inject.advanced.AbstractLanguageInjectionSupport;
import consulo.language.inject.advanced.BaseInjection;
import consulo.language.inject.advanced.InjectorUtils;
import consulo.language.inject.advanced.LanguageInjectionSupport;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gregsh
 */
@ExtensionImpl
public class CommentLanguageInjector implements MultiHostInjector {

  private final LanguageInjectionSupport[] mySupports;
  private final LanguageInjectionSupport myInjectorSupport = new AbstractLanguageInjectionSupport() {
    @Nonnull
    @Override
    public String getId() {
      return "comment";
    }

    @Override
    public boolean isApplicableTo(PsiLanguageInjectionHost host) {
      return true;
    }

    @Nonnull
    @Override
    public Class[] getPatternClasses() {
      return ArrayUtil.EMPTY_CLASS_ARRAY;
    }
  };

  @Inject
  public CommentLanguageInjector() {
    List<LanguageInjectionSupport> supports = new ArrayList<LanguageInjectionSupport>(InjectorUtils.getActiveInjectionSupports());
    supports.add(myInjectorSupport);
    mySupports = ArrayUtil.toObjectArray(supports, LanguageInjectionSupport.class);
  }

  @Nonnull
  @Override
  public Class<? extends PsiElement> getElementClass() {
    return PsiLanguageInjectionHost.class;
  }

  @Override
  public void injectLanguages(@Nonnull final MultiHostRegistrar registrar, @Nonnull final PsiElement context) {
    if (!(context instanceof PsiLanguageInjectionHost) || context instanceof PsiComment) return;
    if (!((PsiLanguageInjectionHost)context).isValidHost()) return;
    PsiLanguageInjectionHost host = (PsiLanguageInjectionHost)context;

    boolean applicableFound = false;
    for (LanguageInjectionSupport support : mySupports) {
      if (!support.isApplicableTo(host)) continue;
      if (support == myInjectorSupport && applicableFound) continue;
      applicableFound = true;

      BaseInjection injection = support.findCommentInjection(host, null);
      if (injection == null) continue;
      if (!InjectorUtils.registerInjectionSimple(host, injection, support, registrar)) continue;
      return;
    }
  }
}
