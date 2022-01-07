/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.psi.injection;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.ui.SimpleColoredText;
import com.intellij.util.Consumer;
import consulo.util.dataholder.Key;
import org.intellij.plugins.intelliLang.Configuration;
import org.intellij.plugins.intelliLang.inject.InjectedLanguage;
import org.intellij.plugins.intelliLang.inject.LanguageInjectionConfigBean;
import org.intellij.plugins.intelliLang.inject.config.BaseInjection;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Gregory.Shrago
 */
public abstract class LanguageInjectionSupport {
  public static final ExtensionPointName<LanguageInjectionSupport> EP_NAME = ExtensionPointName.create("com.intellij.injectionLanguageSupport");
  public static final ExtensionPointName<LanguageInjectionConfigBean> CONFIG_EP_NAME = ExtensionPointName.create("com.intellij.injectionConfig");


  public static Key<InjectedLanguage> TEMPORARY_INJECTED_LANGUAGE = Key.create("TEMPORARY_INJECTED_LANGUAGE");
  public static Key<LanguageInjectionSupport> INJECTOR_SUPPORT = Key.create("INJECTOR_SUPPORT");
  public static Key<LanguageInjectionSupport> SETTINGS_EDITOR = Key.create("SETTINGS_EDITOR");

  @Nonnull
  public abstract String getId();

  @Nonnull
  public abstract Class[] getPatternClasses();

  public abstract boolean isApplicableTo(PsiLanguageInjectionHost host);

  public abstract boolean useDefaultInjector(PsiLanguageInjectionHost host);

  @Nullable
  public abstract BaseInjection findCommentInjection(@Nonnull PsiElement host, @Nullable Ref<PsiElement> commentRef);

  public abstract boolean addInjectionInPlace(final Language language, final PsiLanguageInjectionHost psiElement);

  public abstract boolean removeInjectionInPlace(final PsiLanguageInjectionHost psiElement);

  public boolean removeInjection(final PsiElement psiElement) {
    return psiElement instanceof PsiLanguageInjectionHost && removeInjectionInPlace((PsiLanguageInjectionHost)psiElement);
  }

  public abstract boolean editInjectionInPlace(final PsiLanguageInjectionHost psiElement);

  public abstract BaseInjection createInjection(final Element element);

  public abstract void setupPresentation(final BaseInjection injection, final SimpleColoredText presentation, final boolean isSelected);

  public abstract Configurable[] createSettings(final Project project, final Configuration configuration);

  public abstract AnAction[] createAddActions(final Project project, final Consumer<BaseInjection> consumer);

  public abstract AnAction createEditAction(final Project project, final Factory<BaseInjection> producer);
}
