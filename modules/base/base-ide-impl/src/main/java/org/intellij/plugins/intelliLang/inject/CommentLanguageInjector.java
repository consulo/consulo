package org.intellij.plugins.intelliLang.inject;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import consulo.psi.injection.AbstractLanguageInjectionSupport;
import consulo.psi.injection.LanguageInjectionSupport;
import org.intellij.plugins.intelliLang.inject.config.BaseInjection;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.util.ArrayUtil;

/**
 * @author gregsh
 */
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


  public CommentLanguageInjector() {
    List<LanguageInjectionSupport> supports = new ArrayList<LanguageInjectionSupport>(InjectorUtils.getActiveInjectionSupports());
    supports.add(myInjectorSupport);
    mySupports = ArrayUtil.toObjectArray(supports, LanguageInjectionSupport.class);
  }

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
