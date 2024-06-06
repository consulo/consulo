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

package consulo.ide.impl.psi.injection;

import consulo.configurable.Configurable;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.intelliLang.Configuration;
import consulo.ide.impl.intelliLang.inject.InjectorUtils;
import consulo.ide.impl.intelliLang.inject.config.BaseInjection;
import consulo.ide.impl.intelliLang.inject.config.ui.BaseInjectionPanel;
import consulo.language.Language;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.project.Project;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogBuilder;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;
import consulo.util.lang.ref.Ref;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Gregory.Shrago
 */
public abstract class AbstractLanguageInjectionSupport extends LanguageInjectionSupport {

  @Override
  public boolean isApplicableTo(PsiLanguageInjectionHost host) {
    return false;
  }

  @Override
  public boolean useDefaultInjector(final PsiLanguageInjectionHost host) {
    return false;
  }

  @Nullable
  @Override
  public BaseInjection findCommentInjection(@Nonnull PsiElement host, @Nullable Ref<PsiElement> commentRef) {
    return InjectorUtils.findCommentInjection(host, "comment", commentRef);
  }

  @Override
  public boolean addInjectionInPlace(final Language language, final PsiLanguageInjectionHost psiElement) {
    return false;
  }

  @Override
  public boolean removeInjectionInPlace(final PsiLanguageInjectionHost psiElement) {
    return false;
  }

  @Override
  public boolean editInjectionInPlace(final PsiLanguageInjectionHost psiElement) {
    return false;
  }

  @Override
  public BaseInjection createInjection(final Element element) {
    return new BaseInjection(getId());
  }

  @Override
  public void setupPresentation(final BaseInjection injection, final SimpleColoredText presentation, final boolean isSelected) {
    presentation.append(injection.getDisplayName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }

  @Override
  public Configurable[] createSettings(final Project project, final Configuration configuration) {
    return new Configurable[0];
  }

  @Override
  public AnAction[] createAddActions(final Project project, final Consumer<BaseInjection> consumer) {
    return new AnAction[] { createDefaultAddAction(project, consumer, this) };
  }

  @Override
  public AnAction createEditAction(final Project project, final Supplier<BaseInjection> producer) {
    return createDefaultEditAction(project, producer);
  }

  public static AnAction createDefaultEditAction(final Project project, final Supplier<BaseInjection> producer) {
    return new AnAction() {
      @Override
      public void actionPerformed(AnActionEvent e) {
        final BaseInjection originalInjection = producer.get();
        final BaseInjection newInjection = showDefaultInjectionUI(project, originalInjection.copy());
        if (newInjection != null) {
          originalInjection.copyFrom(newInjection);
        }
      }
    };
  }

  public static AnAction createDefaultAddAction(final Project project,
                                                final Consumer<BaseInjection> consumer,
                                                final AbstractLanguageInjectionSupport support) {
    Image icon = FileTypeManager.getInstance().getFileTypeByExtension(support.getId()).getIcon();
    return new AnAction("Generic "+ StringUtil.capitalize(support.getId()), null, icon) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        final BaseInjection injection = new BaseInjection(support.getId());
        injection.setDisplayName("New "+ StringUtil.capitalize(support.getId())+" Injection");
        final BaseInjection newInjection = showDefaultInjectionUI(project, injection);
        if (newInjection != null) {
          consumer.accept(injection);
        }
      }
    };
  }

  @Nullable
  protected static BaseInjection showDefaultInjectionUI(final Project project, BaseInjection injection) {
    final BaseInjectionPanel panel = new BaseInjectionPanel(injection, project);
    panel.reset();
    final DialogBuilder builder = new DialogBuilder(project);
    LanguageInjectionSupport support = InjectorUtils.findInjectionSupport(injection.getSupportId());
    if (support != null && support instanceof AbstractLanguageInjectionSupport) {
      builder.setHelpId(((AbstractLanguageInjectionSupport)support).getHelpId());
    }
    builder.addOkAction();
    builder.addCancelAction();
    builder.setDimensionServiceKey("#consulo.ide.impl.intelliLang.inject.config.ui.BaseInjectionDialog");
    builder.setCenterPanel(panel.getComponent());
    builder.setTitle("Language Injection Settings");
    builder.setOkOperation(new Runnable() {
      @Override
      public void run() {
        try {
          panel.apply();
          builder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
        }
        catch (Exception e) {
          final Throwable cause = e.getCause();
          final String message = e.getMessage() + (cause != null? "\n  "+cause.getMessage():"");
          Messages.showErrorDialog(project, message, "Unable to Save");
        }
      }
    });
    if (builder.show() == DialogWrapper.OK_EXIT_CODE) {
      return injection;
    }
    return null;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof LanguageInjectionSupport && getId().equals(((LanguageInjectionSupport)obj).getId());
  }

  @Nullable
  public String getHelpId() {
    return null;
  }
}
