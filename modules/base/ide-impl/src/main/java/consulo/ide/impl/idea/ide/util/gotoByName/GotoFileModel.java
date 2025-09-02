// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.application.dumb.DumbAware;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.ide.impl.idea.ide.actions.GotoFileItemProvider;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.localize.LocalizeValue;
import consulo.util.io.FileUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.util.lang.StringUtil;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ide.navigation.GotoFileContributor;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.util.collection.JBIterable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Collection;
import java.util.Comparator;

/**
 * Model for "Go to | File" action
 */
public class GotoFileModel extends FilteringGotoByModel<FileType> implements DumbAware, Comparator<Object> {
  private final int myMaxSize;

  public GotoFileModel(@Nonnull Project project) {
    super(project, project.getApplication().getExtensionList(GotoFileContributor.class));
    myMaxSize = project.getApplication().isUnitTestMode() ? Integer.MAX_VALUE : WindowManagerEx.getInstanceEx().getFrame(project).getSize().width;
  }

  public boolean isSlashlessMatchingEnabled() {
    return true;
  }

  @Nonnull
  @Override
  public ChooseByNameItemProvider getItemProvider(@Nullable PsiElement context) {
    return new GotoFileItemProvider(myProject, context, this);
  }

  @Override
  protected boolean acceptItem(NavigationItem item) {
    if (item instanceof PsiFile) {
      PsiFile file = (PsiFile)item;
      Collection<FileType> types = getFilterItems();
      // if language substitutors are used, PsiFile.getFileType() can be different from
      // PsiFile.getVirtualFile().getFileType()
      if (types != null) {
        if (types.contains(file.getFileType())) return true;
        VirtualFile vFile = file.getVirtualFile();
        return vFile != null && types.contains(vFile.getFileType());
      }
      return true;
    }
    else {
      return super.acceptItem(item);
    }
  }

  @Nullable
  @Override
  protected FileType filterValueFor(NavigationItem item) {
    return item instanceof PsiFile ? ((PsiFile)item).getFileType() : null;
  }

  @Override
  public String getPromptText() {
    return IdeLocalize.promptGotofileEnterFileName().get();
  }

  @Override
  public LocalizeValue getCheckBoxName() {
    return IdeLocalize.checkboxIncludeNonProjectFiles();
  }

  @Nonnull
  @Override
  public String getNotInMessage() {
    return "";
  }

  @Nonnull
  @Override
  public String getNotFoundMessage() {
    return IdeLocalize.labelNoFilesFound().get();
  }

  @Override
  public boolean loadInitialCheckBoxState() {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    return propertiesComponent.isTrueValue("GoToClass.toSaveIncludeLibraries") && propertiesComponent.isTrueValue("GoToFile.includeJavaFiles");
  }

  @Override
  public void saveInitialCheckBoxState(boolean state) {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    if (propertiesComponent.isTrueValue("GoToClass.toSaveIncludeLibraries")) {
      propertiesComponent.setValue("GoToFile.includeJavaFiles", Boolean.toString(state));
    }
  }

  @Nonnull
  @Override
  public PsiElementListCellRenderer getListCellRenderer() {
    return new GotoFileCellRenderer(myMaxSize) {
      @Nonnull
      @Override
      protected ItemMatchers getItemMatchers(@Nonnull JList list, @Nonnull Object value) {
        ItemMatchers defaultMatchers = super.getItemMatchers(list, value);
        if (!(value instanceof PsiFileSystemItem)) return defaultMatchers;

        return convertToFileItemMatchers(defaultMatchers, (PsiFileSystemItem)value, GotoFileModel.this);
      }
    };
  }

  @Override
  public boolean sameNamesForProjectAndLibraries() {
    return false;
  }

  @Override
  @Nullable
  public String getFullName(@Nonnull Object element) {
    return element instanceof PsiFileSystemItem ? getFullName(((PsiFileSystemItem)element).getVirtualFile()) : getElementName(element);
  }

  @Nullable
  public String getFullName(@Nonnull VirtualFile file) {
    VirtualFile root = getTopLevelRoot(file);
    return root != null ? GotoFileCellRenderer.getRelativePathFromRoot(file, root) : GotoFileCellRenderer.getRelativePath(file, myProject);
  }

  @Nullable
  public VirtualFile getTopLevelRoot(@Nonnull VirtualFile file) {
    VirtualFile root = getContentRoot(file);
    return root == null ? null : JBIterable.generate(root, r -> getContentRoot(r.getParent())).last();
  }

  private VirtualFile getContentRoot(@Nullable VirtualFile file) {
    return file == null ? null : GotoFileCellRenderer.getAnyRoot(file, myProject);
  }

  @Override
  @Nonnull
  public String[] getSeparators() {
    return new String[]{"/", "\\"};
  }

  @Override
  public String getHelpId() {
    return "procedures.navigating.goto.class";
  }

  @Override
  public boolean willOpenEditor() {
    return true;
  }

  @Nonnull
  @Override
  public String removeModelSpecificMarkup(@Nonnull String pattern) {
    if (pattern.endsWith("/") || pattern.endsWith("\\")) {
      return pattern.substring(0, pattern.length() - 1);
    }
    return pattern;
  }

  /**
   * Just to remove smartness from {@link ChooseByNameBase#calcSelectedIndex}
   */
  @Override
  public int compare(Object o1, Object o2) {
    return 0;
  }

  @Nonnull
  public static PsiElementListCellRenderer.ItemMatchers convertToFileItemMatchers(
    @Nonnull PsiElementListCellRenderer.ItemMatchers defaultMatchers,
    @Nonnull PsiFileSystemItem value,
    @Nonnull GotoFileModel model
  ) {
    String shortName = model.getElementName(value);
    String fullName = model.getFullName(value);
    if (shortName != null && fullName != null && defaultMatchers.nameMatcher instanceof MinusculeMatcher) {
      String sanitized = GotoFileItemProvider.getSanitizedPattern(((MinusculeMatcher)defaultMatchers.nameMatcher).getPattern(), model);
      for (int i = sanitized.lastIndexOf('/') + 1; i < sanitized.length() - 1; i++) {
        MinusculeMatcher nameMatcher = NameUtil.buildMatcher("*" + sanitized.substring(i), NameUtil.MatchingCaseSensitivity.NONE);
        if (nameMatcher.matches(shortName)) {
          String locationPattern = FileUtil.toSystemDependentName(StringUtil.trimEnd(sanitized.substring(0, i), "/"));
          return new PsiElementListCellRenderer.ItemMatchers(nameMatcher, GotoFileItemProvider.getQualifiedNameMatcher(locationPattern));
        }
      }
    }

    return defaultMatchers;
  }
}