// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.structureView;

import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.structureView.StructureView;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.language.util.LanguageUtil;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.JBIterable;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UnknownFileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author peter
 */
public abstract class TemplateLanguageStructureViewBuilder extends TreeBasedStructureViewBuilder {

  @Nonnull
  public static TemplateLanguageStructureViewBuilder create(@Nonnull PsiFile psiFile, @Nullable BiFunction<? super PsiFile, ? super Editor, ? extends StructureViewModel> modelFactory) {
    return new TemplateLanguageStructureViewBuilder(psiFile) {
      @Override
      protected TreeBasedStructureViewBuilder createMainBuilder(@Nonnull PsiFile psi) {
        return modelFactory == null ? null : new TreeBasedStructureViewBuilder() {
          @Override
          public boolean isRootNodeShown() {
            return false;
          }

          @Nonnull
          @Override
          public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
            return modelFactory.apply(psi, editor);
          }
        };
      }
    };
  }

  private final VirtualFile myVirtualFile;
  private final Project myProject;

  protected TemplateLanguageStructureViewBuilder(PsiElement psiElement) {
    myProject = psiElement.getProject();
    myVirtualFile = psiElement.getContainingFile().getVirtualFile();
  }

  @Override
  public boolean isRootNodeShown() {
    return false;
  }

  @Override
  @Nonnull
  public StructureView createStructureView(FileEditor fileEditor, @Nonnull Project project) {
    List<StructureViewComposite.StructureViewDescriptor> viewDescriptors = new ArrayList<>();
    VirtualFile file = fileEditor == null ? null : fileEditor.getFile();
    PsiFile psiFile = file == null || !file.isValid() ? null : PsiManager.getInstance(project).findFile(file);
    List<Language> languages = getLanguages(psiFile).toList();
    for (Language language : languages) {
      StructureViewBuilder builder = getBuilder(ObjectUtil.notNull(psiFile), language);
      if (builder == null) continue;
      StructureView structureView = builder.createStructureView(fileEditor, project);
      String title = language.getDisplayName().get();
      Image icon = ObjectUtil.notNull(LanguageUtil.getLanguageFileType(language), UnknownFileType.INSTANCE).getIcon();
      viewDescriptors.add(new StructureViewComposite.StructureViewDescriptor(title, structureView, icon));
    }
    StructureViewComposite.StructureViewDescriptor[] array = viewDescriptors.toArray(new StructureViewComposite.StructureViewDescriptor[0]);
    return new StructureViewComposite(array) {
      @Override
      public boolean isOutdated() {
        VirtualFile file = fileEditor == null ? null : fileEditor.getFile();
        PsiFile psiFile = file == null || !file.isValid() ? null : PsiManager.getInstance(project).findFile(file);
        List<Language> newLanguages = getLanguages(psiFile).toList();
        if (!Comparing.equal(languages, newLanguages)) return true;
        if (psiFile == null) return true;
        FileViewProvider viewProvider = psiFile.getViewProvider();
        Language baseLanguage = viewProvider.getBaseLanguage();
        StructureViewDescriptor[] views = getStructureViews();
        boolean hasMainView = views.length > 0 && Comparing.equal(views[0].title, baseLanguage.getDisplayName());
        JBIterable<Language> newAcceptedLanguages =
                JBIterable.from(newLanguages).filter(o -> o == baseLanguage && hasMainView || o != baseLanguage && isAcceptableBaseLanguageFile(viewProvider.getPsi(o)));
        return views.length != newAcceptedLanguages.size();
      }
    };
  }

  @Override
  @Nonnull
  public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
    List<StructureViewComposite.StructureViewDescriptor> viewDescriptors = new ArrayList<>();
    PsiFile psiFile = ObjectUtil.notNull(PsiManager.getInstance(myProject).findFile(myVirtualFile));
    for (Language language : getLanguages(psiFile)) {
      StructureViewBuilder builder = getBuilder(psiFile, language);
      if (!(builder instanceof TreeBasedStructureViewBuilder)) continue;
      StructureViewModel model = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(editor);
      String title = language.getDisplayName().get();
      Image icon = ObjectUtil.notNull(LanguageUtil.getLanguageFileType(language), UnknownFileType.INSTANCE).getIcon();
      viewDescriptors.add(new StructureViewComposite.StructureViewDescriptor(title, model, icon));
    }
    return new StructureViewCompositeModel(psiFile, editor, viewDescriptors);
  }

  @Nonnull
  private static JBIterable<Language> getLanguages(@Nullable PsiFile psiFile) {
    if (psiFile == null) return JBIterable.empty();
    FileViewProvider provider = psiFile.getViewProvider();

    Language baseLanguage = provider.getBaseLanguage();
    Language dataLanguage = provider instanceof TemplateLanguageFileViewProvider ? ((TemplateLanguageFileViewProvider)provider).getTemplateDataLanguage() : null;
    return JBIterable.of(baseLanguage).append(dataLanguage).append(JBIterable.from(provider.getLanguages()).filter(o -> o != baseLanguage && o != dataLanguage));
  }

  @Nullable
  private StructureViewBuilder getBuilder(@Nonnull PsiFile psiFile, @Nonnull Language language) {
    FileViewProvider viewProvider = psiFile.getViewProvider();
    Language baseLanguage = viewProvider.getBaseLanguage();
    PsiFile psi = viewProvider.getPsi(language);
    if (psi == null) return null;
    if (language == baseLanguage) return createMainBuilder(psi);
    if (!isAcceptableBaseLanguageFile(psi)) return null;
    return PsiStructureViewFactory.createBuilderForFile(psi);
  }

  protected boolean isAcceptableBaseLanguageFile(PsiFile dataFile) {
    return true;
  }

  @Nullable
  protected abstract TreeBasedStructureViewBuilder createMainBuilder(@Nonnull PsiFile psi);
}
