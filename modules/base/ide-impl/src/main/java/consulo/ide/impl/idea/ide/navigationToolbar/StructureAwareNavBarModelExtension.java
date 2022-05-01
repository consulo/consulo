/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.idea.ide.navigationToolbar;

import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.language.editor.structureView.PsiTreeElementBase;
import consulo.application.ui.UISettings;
import consulo.fileEditor.structureView.tree.NodeProvider;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.language.Language;
import consulo.ide.impl.idea.lang.LanguageStructureViewBuilder;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.application.util.function.Processor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ref.SoftReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * from kotlin
 */
public abstract class StructureAwareNavBarModelExtension extends AbstractNavBarModelExtension {
  @Nullable
  private SoftReference<PsiFile> currentFile;
  @Nullable
  private SoftReference<StructureViewModel> currentFileStructure;
  private long currentFileModCount = -1;

  @Nonnull
  protected abstract Language getLanguage();

  @Nonnull
  protected List<NodeProvider<?>> getApplicableNodeProviders() {
    return Collections.emptyList();
  }

  protected boolean acceptParentFromModel(PsiElement psiElement) {
    return true;
  }

  @Override
  @RequiredReadAction
  public PsiElement getLeafElement(@Nonnull DataContext dataContext) {
    if (UISettings.getInstance().getShowMembersInNavigationBar()) {
      PsiFile psiFile = dataContext.getData(CommonDataKeys.PSI_FILE);
      Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
      if (psiFile == null || editor == null) return null;
      PsiElement psiElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
      if (psiElement != null && psiElement.getLanguage() == getLanguage()) {
        StructureViewModel model = buildStructureViewModel(psiFile, editor);
        if (model != null) {
          Object currentEditorElement = model.getCurrentEditorElement();
          if (currentEditorElement instanceof PsiElement) {
            return ((PsiElement)currentEditorElement).getOriginalElement();
          }
        }
      }
    }
    return null;
  }

  @Override
  @RequiredReadAction
  public boolean processChildren(Object object, Object rootElement, Processor<Object> processor) {
    if (UISettings.getInstance().getShowMembersInNavigationBar()) {
      if (object instanceof PsiElement) {
        if (((PsiElement)object).getLanguage() == getLanguage()) {
          StructureViewModel model = buildStructureViewModel(((PsiElement)object).getContainingFile(), null);
          if (model != null) {
            return processStructureViewChildren(model.getRoot(), object, processor);
          }
        }
      }
    }
    return super.processChildren(object, rootElement, processor);
  }

  @Nullable
  @Override
  @RequiredReadAction
  public PsiElement getParent(@Nonnull PsiElement psiElement) {
    if (psiElement.getLanguage() == getLanguage()) {
      PsiFile containingFile = psiElement.getContainingFile();
      if (containingFile == null) {
        return null;
      }

      StructureViewModel model = buildStructureViewModel(containingFile, null);
      if (model != null) {
        PsiElement parentInModel = findParentInModel(model.getRoot(), psiElement);
        if(acceptParentFromModel(parentInModel)) {
          return parentInModel;
        }
      }
    }
    return super.getParent(psiElement);
  }

  @Override
  public boolean normalizeChildren() {
    return false;
  }

  @Nullable
  @RequiredReadAction
  private PsiElement findParentInModel(StructureViewTreeElement root, PsiElement psiElement) {
    for (TreeElement child : childrenFromNodeAndProviders(root)) {
      if(child instanceof StructureViewTreeElement) {
        if(Comparing.equal(((StructureViewTreeElement)child).getValue(), psiElement)) {
          return ObjectUtil.tryCast(root.getValue(), PsiElement.class);
        }
        PsiElement target = findParentInModel((StructureViewTreeElement)child, psiElement);
        if(target != null) {
          return target;
        }
      }
    }

    return null;
  }

  @RequiredReadAction
  private boolean processStructureViewChildren(StructureViewTreeElement parent, Object object, Processor<Object> processor) {
    List<TreeElement> children = childrenFromNodeAndProviders(parent);

    boolean result = true;
    if (Comparing.equal(parent.getValue(), object)) {
      for (TreeElement child : children) {
        if (child instanceof StructureViewTreeElement) {
          if (!processor.process(((StructureViewTreeElement)child).getValue())) {
            result = false;
          }
        }
      }

    }
    else {
      for (TreeElement child : children) {
        if (child instanceof StructureViewTreeElement) {
          if (!processStructureViewChildren((StructureViewTreeElement)child, object, processor)) {
            result = false;
          }
        }
      }
    }
    return result;
  }

  @RequiredReadAction
  private List<TreeElement> childrenFromNodeAndProviders(StructureViewTreeElement parent) {
    List<TreeElement> children;
    if (parent instanceof PsiTreeElementBase) {
      children = ((PsiTreeElementBase)parent).getChildrenWithoutCustomRegions();
    }
    else {
      children = Arrays.asList(parent.getChildren());
    }

    List<TreeElement> fromProviders = getApplicableNodeProviders().stream().flatMap(nodeProvider -> nodeProvider.provideNodes(parent).stream()).collect(Collectors.toList());
    return ContainerUtil.concat(children, fromProviders);
  }

  @Nullable
  private StructureViewModel buildStructureViewModel(PsiFile file, @Nullable Editor editor) {
    if (Comparing.equal(SoftReference.deref(currentFile), file) && currentFileModCount == file.getModificationStamp()) {
      StructureViewModel model = SoftReference.deref(currentFileStructure);
      if (model != null) {
        return model;
      }
    }

    StructureViewBuilder builder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file);
    if (builder instanceof TreeBasedStructureViewBuilder) {
      StructureViewModel model = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(editor);

      currentFile = new SoftReference<>(file);
      currentFileStructure = new SoftReference<>(model);
      currentFileModCount = file.getModificationStamp();
      return model;
    }

    return null;
  }
}
