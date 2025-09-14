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
package consulo.language.editor.todo.impl.internal.node;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.ide.todo.HighlightedRegionProvider;
import consulo.ide.impl.idea.ide.todo.TodoFileDirAndModuleComparator;
import consulo.ide.impl.idea.ide.todo.TodoTreeBuilder;
import consulo.ide.impl.idea.ide.todo.TodoTreeStructure;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.PsiPackageManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.PackageElement;
import consulo.project.ui.view.tree.PackageElementNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.HighlightedRegion;
import consulo.ui.ex.tree.PresentationData;
import consulo.usage.UsageTreeColors;
import consulo.usage.UsageTreeColorsScheme;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

public final class TodoPackageNode extends PackageElementNode implements HighlightedRegionProvider {
  private static final Logger LOG = Logger.getInstance(TodoPackageNode.class);

  private final ArrayList<HighlightedRegion> myHighlightedRegions;
  private final TodoTreeBuilder myBuilder;
  @Nullable private final String myPresentationName;

  public TodoPackageNode(@Nonnull Project project,
                         PackageElement element,
                         TodoTreeBuilder builder) {
    this(project, element, builder,null);
  }

  public TodoPackageNode(@Nonnull Project project,
                         PackageElement element,
                         TodoTreeBuilder builder,
                         @Nullable String name) {
    super(project, element, ViewSettings.DEFAULT);
    myBuilder = builder;
    myHighlightedRegions = new ArrayList<HighlightedRegion>(2);
    if (element != null && name == null){
      PsiPackage aPackage = element.getPackage();
      myPresentationName = aPackage.getName();
    }
    else {
      myPresentationName = name;
    }
  }


  @Override
  public ArrayList<HighlightedRegion> getHighlightedRegions() {
    return myHighlightedRegions;
  }

  @Override
  @RequiredUIAccess
  protected void update(PresentationData data) {
    super.update(data);
    PackageElement packageElement = getValue();

    try {
      if (packageElement == null || !packageElement.getPackage().isValid()) {
        setValue(null);
        return;
      }

      int fileCount = getFileCount(packageElement);
      if (fileCount == 0){
        setValue(null);
        return;
      }

      PsiPackage aPackage = packageElement.getPackage();
      String newName;
      if (getStructure().areFlattenPackages()) {
        newName = aPackage.getQualifiedName();
      }
      else {
        newName = myPresentationName != null ? myPresentationName : "";
      }

      int nameEndOffset = newName.length();
      int todoItemCount = getTodoItemCount(packageElement);
      newName = IdeLocalize.nodeTodoGroup(todoItemCount).get();

      myHighlightedRegions.clear();

      TextAttributes textAttributes = new TextAttributes();
      ColorValue newColor = null;

      if (CopyPasteManager.getInstance().isCutElement(packageElement)) {
        newColor = CopyPasteManager.CUT_COLOR;
      }
      textAttributes.setForegroundColor(newColor);
      myHighlightedRegions.add(new HighlightedRegion(0, nameEndOffset, textAttributes));

      EditorColorsScheme colorsScheme = UsageTreeColorsScheme.getInstance().getScheme();
      myHighlightedRegions.add(
        new HighlightedRegion(nameEndOffset, newName.length(), colorsScheme.getAttributes(UsageTreeColors.NUMBER_OF_USAGES)));

      data.setPresentableText(newName);
    }
    catch (IndexNotReadyException e) {
      LOG.info(e);
      data.setPresentableText("N/A");
    }
  }

  @Override
  public void apply(@Nonnull Map<String, String> info) {
    info.put("toDoFileCount", String.valueOf(getFileCount(getValue())));
    info.put("toDoItemCount", String.valueOf(getTodoItemCount(getValue())));
  }

  private int getFileCount(PackageElement packageElement) {
    int count = 0;
    if (getSettings().isFlattenPackages()) {
      PsiPackage aPackage = packageElement.getPackage();
      Module module = packageElement.getModule();
      GlobalSearchScope scope =
        module != null ? GlobalSearchScope.moduleScope(module) : GlobalSearchScope.projectScope(aPackage.getProject());
      PsiDirectory[] directories = aPackage.getDirectories(scope);
      for (PsiDirectory directory : directories) {
        Iterator<PsiFile> iterator = myBuilder.getFilesUnderDirectory(directory);
        while (iterator.hasNext()) {
          PsiFile psiFile = iterator.next();
          if (getStructure().accept(psiFile)) count++;
        }
      }
    }
    else {
      Iterator<PsiFile> iterator = getFiles(packageElement);
      while (iterator.hasNext()) {
        PsiFile psiFile = iterator.next();
        if (getStructure().accept(psiFile)) {
          count++;
        }
      }
    }
    return count;
  }

  public int getTodoItemCount(PackageElement packageElement) {
    int count = 0;
    if (getSettings().isFlattenPackages()){
        PsiPackage aPackage = packageElement.getPackage();
        Module module = packageElement.getModule();
        GlobalSearchScope scope = module != null ? GlobalSearchScope.moduleScope(module) : GlobalSearchScope.projectScope(aPackage.getProject());
        PsiDirectory[] directories = aPackage.getDirectories(scope);
        for (PsiDirectory directory : directories) {
          Iterator<PsiFile> iterator = myBuilder.getFilesUnderDirectory(directory);
          while(iterator.hasNext()){
            PsiFile psiFile = iterator.next();
            count+=getStructure().getTodoItemCount(psiFile);
          }
        }
      } else {
        Iterator<PsiFile> iterator = getFiles(packageElement);
        while(iterator.hasNext()){
          PsiFile psiFile = iterator.next();
          count+=getStructure().getTodoItemCount(psiFile);
        }
      }
    return count;
  }

  private TodoTreeStructure getStructure() {
    return myBuilder.getTodoTreeStructure();
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Collection<AbstractTreeNode> getChildren() {
    ArrayList<AbstractTreeNode> children = new ArrayList<AbstractTreeNode>();
    Project project = getProject();
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    PsiPackage psiPackage = getValue().getPackage();
    Module module = getValue().getModule();
    if (!getStructure().getIsFlattenPackages() || psiPackage == null) {
      Iterator<PsiFile> iterator = getFiles(getValue());
      while (iterator.hasNext()) {
        PsiFile psiFile = iterator.next();
        Module psiFileModule = projectFileIndex.getModuleForFile(psiFile.getVirtualFile());
        //group by module
        if (module != null && psiFileModule != null && !module.equals(psiFileModule)){
          continue;
        }
        // Add files
        PsiDirectory containingDirectory = psiFile.getContainingDirectory();
        TodoFileNode todoFileNode = new TodoFileNode(project, psiFile, myBuilder, false);
        if (ArrayUtil.find(psiPackage.getDirectories(), containingDirectory) > -1 && !children.contains(todoFileNode)) {
          children.add(todoFileNode);
          continue;
        }
        // Add packages
        PsiDirectory _dir = psiFile.getContainingDirectory();
        while (_dir != null) {
          PsiDirectory parentDirectory = _dir.getParentDirectory();
          if (parentDirectory != null){
            PsiPackage _package = PsiPackageManager.getInstance(_dir.getProject()).findAnyPackage(_dir);
            if (_package != null && _package.getParentPackage() != null && psiPackage.equals(_package.getParentPackage())) {
              GlobalSearchScope scope = module != null ? GlobalSearchScope.moduleScope(module) : GlobalSearchScope.projectScope(project);
              _package = TodoTreeHelper.findNonEmptyPackage(_package, module, project, myBuilder, scope); //compact empty middle packages
              String name = _package.getParentPackage().equals(psiPackage)
                                  ? null //non compacted
                                  : _package.getQualifiedName().substring(psiPackage.getQualifiedName().length() + 1);
              TodoPackageNode todoPackageNode = new TodoPackageNode(project, new PackageElement(module, _package, false), myBuilder, name);
              if (!children.contains(todoPackageNode)) {
                children.add(todoPackageNode);
                break;
              }
            }
          }
          _dir = parentDirectory;
        }
      }
    }
    else { // flatten packages
      Iterator<PsiFile> iterator = getFiles(getValue());
      while (iterator.hasNext()) {
        PsiFile psiFile = iterator.next();
         //group by module
        Module psiFileModule = projectFileIndex.getModuleForFile(psiFile.getVirtualFile());
        if (module != null && psiFileModule != null && !module.equals(psiFileModule)){
          continue;
        }
        PsiDirectory _dir = psiFile.getContainingDirectory();
        // Add files
        TodoFileNode todoFileNode = new TodoFileNode(getProject(), psiFile, myBuilder, false);
        if (ArrayUtil.find(psiPackage.getDirectories(), _dir) > -1 && !children.contains(todoFileNode)) {
          children.add(todoFileNode);
          continue;
        }
      }
    }
    Collections.sort(children, TodoFileDirAndModuleComparator.INSTANCE);
    return children;
  }

  /**
   * @return read-only iterator of all valid PSI files that can have T.O.D.O items
   *         and which are located under specified <code>psiDirctory</code>.
   */
  public Iterator<PsiFile> getFiles(PackageElement packageElement) {
    ArrayList<PsiFile> psiFileList = new ArrayList<PsiFile>();
    GlobalSearchScope scope = packageElement.getModule() != null ? GlobalSearchScope.moduleScope(packageElement.getModule()) :
                              GlobalSearchScope.projectScope(myProject);
    PsiDirectory[] directories = packageElement.getPackage().getDirectories(scope);
    for (PsiDirectory directory : directories) {
      Iterator<PsiFile> files = myBuilder.getFiles(directory, false);
      for (;files.hasNext();) {
        psiFileList.add(files.next());
      }
    }
    return psiFileList.iterator();
  }

  @Override
  public int getWeight() {
    return 3;
  }
}

