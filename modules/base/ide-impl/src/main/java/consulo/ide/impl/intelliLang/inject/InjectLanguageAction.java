/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.intelliLang.inject;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.function.Processor;
import consulo.codeEditor.Editor;
import consulo.configurable.Configurable;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.FileContentUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.intelliLang.Configuration;
import consulo.ide.impl.intelliLang.references.InjectedReferencesContributor;
import consulo.ide.impl.psi.injection.LanguageInjectionSupport;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.Language;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.hint.QuestionAction;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.impl.internal.psi.PsiModificationTrackerImpl;
import consulo.language.inject.Injectable;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.ReferenceInjector;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
@IntentionMetaData(ignoreId = "platform.inject.language", fileExtensions = "txt", categories = "Language Injection")
public class InjectLanguageAction implements IntentionAction {
  public static final String LAST_INJECTED_LANGUAGE = "LAST_INJECTED_LANGUAGE";
  public static final Key<Processor<PsiLanguageInjectionHost>> FIX_KEY = Key.create("inject fix key");

  public static List<Injectable> getAllInjectables() {
    Language[] languages = InjectedLanguage.getAvailableLanguages();
    List<Injectable> list = new ArrayList<>();
    for (Language language : languages) {
      list.add(Injectable.fromLanguage(language));
    }
    list.addAll(ReferenceInjector.EXTENSION_POINT_NAME.getExtensionList());
    Collections.sort(list);
    return list;
  }

  @Override
  @Nonnull
  public String getText() {
    return "Inject Language/Reference";
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    final PsiLanguageInjectionHost host = findInjectionHost(editor, file);
    if (host == null) return false;
    final List<Pair<PsiElement, TextRange>> injectedPsi = InjectedLanguageManager.getInstance(project).getInjectedPsiFiles(host);
    if (injectedPsi == null || injectedPsi.isEmpty()) {
      return !InjectedReferencesContributor.isInjected(file.findReferenceAt(editor.getCaretModel().getOffset()));
    }
    return true;
  }

  @Nullable
  protected static PsiLanguageInjectionHost findInjectionHost(Editor editor, PsiFile file) {
    if (editor instanceof EditorWindow) return null;
    final int offset = editor.getCaretModel().getOffset();
    final PsiLanguageInjectionHost host = PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiLanguageInjectionHost.class, false);
    if (host == null) return null;
    return host.isValidHost() ? host : null;
  }

  @Override
  public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    doChooseLanguageToInject(editor, injectable -> {
      ApplicationManager.getApplication().runReadAction(() -> {
        if (!project.isDisposed()) {
          invokeImpl(project, editor, file, injectable);
        }
      });
      return false;
    });
  }

  public static void invokeImpl(Project project, Editor editor, final PsiFile file, Injectable injectable) {
    final PsiLanguageInjectionHost host = findInjectionHost(editor, file);
    if (host == null) return;
    if (defaultFunctionalityWorked(host, injectable.getId())) return;

    try {
      host.putUserData(FIX_KEY, null);
      Language language = injectable.toLanguage();
      for (LanguageInjectionSupport support : InjectorUtils.getActiveInjectionSupports()) {
        if (support.isApplicableTo(host) && support.addInjectionInPlace(language, host)) {
          return;
        }
      }
      if (TemporaryPlacesRegistry.getInstance(project).getLanguageInjectionSupport().addInjectionInPlace(language, host)) {
        final Processor<PsiLanguageInjectionHost> data = host.getUserData(FIX_KEY);
        String text = StringUtil.escapeXml(language.getDisplayName()) + " was temporarily injected.";
        if (data != null) {
          if (!ApplicationManager.getApplication().isUnitTestMode()) {
            final SmartPsiElementPointer<PsiLanguageInjectionHost> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(host);
            final TextRange range = host.getTextRange();
            HintManager.getInstance().showQuestionHint(editor, text +
                                                               "<br>Do you want to insert annotation? " +
                                                               KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS)),
                                                       range.getStartOffset(), range.getEndOffset(), new QuestionAction() {
                      @Override
                      public boolean execute() {
                        return data.process(pointer.getElement());
                      }
                    });
          }
        }
        else {
          HintManager.getInstance().showInformationHint(editor, text);
        }
      }
    }
    finally {
      if (injectable.getLanguage() != null) {    // no need for reference injection
        FileContentUtil.reparseFiles(project, Collections.<VirtualFile>emptyList(), true);
      }
      else {
        ((PsiModificationTrackerImpl)PsiManager.getInstance(project).getModificationTracker()).incCounter();
        DaemonCodeAnalyzer.getInstance(project).restart();
      }
    }
  }

  private static boolean defaultFunctionalityWorked(final PsiLanguageInjectionHost host, String id) {
    return Configuration.getProjectInstance(host.getProject()).setHostInjectionEnabled(host, Collections.singleton(id), true);
  }

  private static boolean doChooseLanguageToInject(Editor editor, final Processor<Injectable> onChosen) {
    final List<Injectable> injectables = getAllInjectables();

    final JList<Injectable> list = new JBList<>(injectables);
    list.setCellRenderer(new ColoredListCellRenderer<>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList<? extends Injectable> list, Injectable value, int index, boolean selected, boolean hasFocus) {
        setIcon(value.getIcon());
        append(value.getDisplayName());
        String description = value.getAdditionalDescription();
        if (description != null) {
          append(description, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
      }
    });
    JBPopup popup = new PopupChooserBuilder(list).setItemChoosenCallback(new Runnable() {
      @Override
      public void run() {
        Injectable value = (Injectable)list.getSelectedValue();
        if (value != null) {
          onChosen.process(value);
          PropertiesComponent.getInstance().setValue(LAST_INJECTED_LANGUAGE, value.getId());
        }
      }
    }).setFilteringEnabled(language -> ((Injectable)language).getDisplayName()).createPopup();
    final String lastInjected = PropertiesComponent.getInstance().getValue(LAST_INJECTED_LANGUAGE);
    if (lastInjected != null) {
      Injectable injectable = ContainerUtil.find(injectables, it -> lastInjected.equals(it.getId()));
      list.setSelectedValue(injectable, true);
    }
    editor.showPopupInBestPositionFor(popup);
    return true;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  public static boolean doEditConfigurable(final Project project, final Configurable configurable) {
    return true; //ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
  }
}
