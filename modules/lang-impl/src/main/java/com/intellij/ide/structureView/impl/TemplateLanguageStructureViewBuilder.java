/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.structureView.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.StructureViewWrapperImpl;
import com.intellij.ide.structureView.*;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public abstract class TemplateLanguageStructureViewBuilder implements StructureViewBuilder {
  private final VirtualFile myVirtualFile;
  private final Project myProject;
  private final PsiTreeChangeAdapter myPsiTreeChangeAdapter;
  private Language myTemplateDataLanguage;
  private StructureViewComposite.StructureViewDescriptor myBaseStructureViewDescriptor;
  private FileEditor myFileEditor;
  private StructureViewComposite myStructureViewComposite;
  private int myBaseLanguageViewDescriptorIndex;

  protected TemplateLanguageStructureViewBuilder(PsiElement psiElement) {
    myVirtualFile = psiElement.getContainingFile().getVirtualFile();
    myProject = psiElement.getProject();

    myPsiTreeChangeAdapter = new PsiTreeChangeAdapter() {
      @Override
      public void childAdded(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      @Override
      public void childMoved(@NotNull PsiTreeChangeEvent event) {
        childrenChanged(event);
      }

      final Alarm myAlarm = new Alarm();
      @Override
      public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        myAlarm.cancelAllRequests();
        myAlarm.addRequest(new Runnable(){
          @Override
          public void run() {
            if (myProject.isDisposed()) return;
            if (myBaseStructureViewDescriptor != null && ((StructureViewComponent)myBaseStructureViewDescriptor.structureView).getTree() == null) return;
            if (!myVirtualFile.isValid()) return;
            ApplicationManager.getApplication().runReadAction(new Runnable(){
              @Override
              public void run() {
                final TemplateLanguageFileViewProvider provider = getViewProvider();
                if (provider == null) return;

                StructureViewWrapper structureViewWrapper = StructureViewFactoryEx.getInstanceEx(myProject).getStructureViewWrapper();
                if (structureViewWrapper == null) return;

                Language baseLanguage = provider.getTemplateDataLanguage();
                if (baseLanguage == myTemplateDataLanguage
                    && (myBaseStructureViewDescriptor == null || isPsiValid(myBaseStructureViewDescriptor))) {
                  updateBaseLanguageView();
                }
                else {
                  myTemplateDataLanguage = baseLanguage;
                  ((StructureViewWrapperImpl)structureViewWrapper).rebuild();
                }
              }
            });
          }
        }, 300, ModalityState.NON_MODAL);
      }
    };
    final TemplateLanguageFileViewProvider provider = getViewProvider();
    assert provider != null;
    myTemplateDataLanguage = provider.getTemplateDataLanguage();
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(myPsiTreeChangeAdapter);
  }

  private static boolean isPsiValid(@NotNull StructureViewComposite.StructureViewDescriptor baseStructureViewDescriptor) {
    final StructureViewComponent view = (StructureViewComponent)baseStructureViewDescriptor.structureView;
    if (view.isDisposed()) return false;

    final Object root = view.getTreeStructure().getRootElement();
    if (root instanceof StructureViewComponent.StructureViewTreeElementWrapper) {
      final TreeElement value = ((StructureViewComponent.StructureViewTreeElementWrapper)root).getValue();
      if (value instanceof StructureViewTreeElement) {
        final Object psi = ((StructureViewTreeElement)value).getValue();
        if (psi instanceof PsiElement) {
          return ((PsiElement)psi).isValid();
        }
      }
    }
    return true;
  }

  @Nullable
  private TemplateLanguageFileViewProvider getViewProvider() {
    final FileViewProvider provider = PsiManager.getInstance(myProject).findViewProvider(myVirtualFile);
    return provider instanceof TemplateLanguageFileViewProvider ? (TemplateLanguageFileViewProvider)provider : null;
  }

  private void updateBaseLanguageView() {
    if (myBaseStructureViewDescriptor == null || !myProject.isOpen()) return;
    final StructureViewComponent view = (StructureViewComponent)myBaseStructureViewDescriptor.structureView;
    if (view.isDisposed()) return;

    StructureViewState state = view.getState();
    List<PsiAnchor> expanded = collectAnchors(state.getExpandedElements());
    List<PsiAnchor> selected = collectAnchors(state.getSelectedElements());
    updateTemplateDataFileView();

    if (view.isDisposed()) return;

    for (PsiAnchor pointer : expanded) {
      PsiElement element = pointer.retrieve();
      if (element != null) {
        view.expandPathToElement(element);
      }
    }
    for (PsiAnchor pointer : selected) {
      PsiElement element = pointer.retrieve();
      if (element != null) {
        view.addSelectionPathTo(element);
      }
    }
  }

  private static List<PsiAnchor> collectAnchors(final Object[] expandedElements) {
    List<PsiAnchor> expanded = new ArrayList<PsiAnchor>(expandedElements == null ? 0 : expandedElements.length);
    if (expandedElements != null) {
      for (Object element : expandedElements) {
        if (element instanceof PsiElement && ((PsiElement) element).isValid()) {
          expanded.add(PsiAnchor.create((PsiElement)element));
        }
      }
    }
    return expanded;
  }

  private void removeBaseLanguageListener() {
    PsiManager.getInstance(myProject).removePsiTreeChangeListener(myPsiTreeChangeAdapter);
  }

  @Override
  @NotNull
  public StructureView createStructureView(FileEditor fileEditor, Project project) {
    myFileEditor = fileEditor;
    List<StructureViewComposite.StructureViewDescriptor> viewDescriptors = new ArrayList<StructureViewComposite.StructureViewDescriptor>();
    final TemplateLanguageFileViewProvider provider = getViewProvider();
    assert provider != null;

    final StructureViewComposite.StructureViewDescriptor structureViewDescriptor = createMainView(fileEditor, provider.getPsi(provider.getBaseLanguage()));
    if (structureViewDescriptor != null) viewDescriptors.add(structureViewDescriptor);

    myBaseLanguageViewDescriptorIndex = -1;
    final Language dataLanguage = provider.getTemplateDataLanguage();

    updateTemplateDataFileView();
    if (myBaseStructureViewDescriptor != null) {
      viewDescriptors.add(myBaseStructureViewDescriptor);
      myBaseLanguageViewDescriptorIndex = viewDescriptors.size() - 1;
    }

    for (final Language language : provider.getLanguages()) {
      if (language != dataLanguage && language != provider.getBaseLanguage()) {
        ContainerUtil.addIfNotNull(createBaseLanguageStructureView(fileEditor, language), viewDescriptors);
      }
    }

    StructureViewComposite.StructureViewDescriptor[] array = viewDescriptors.toArray(new StructureViewComposite.StructureViewDescriptor[viewDescriptors.size()]);
    myStructureViewComposite = new StructureViewComposite(array){
      @Override
      public void dispose() {
        removeBaseLanguageListener();
        super.dispose();
      }
    };
    return myStructureViewComposite;
  }

  protected abstract StructureViewComposite.StructureViewDescriptor createMainView(FileEditor fileEditor, PsiFile mainFile);

  @Nullable
  private StructureViewComposite.StructureViewDescriptor createBaseLanguageStructureView(final FileEditor fileEditor, final Language language) {
    if (!myVirtualFile.isValid()) return null;

    final TemplateLanguageFileViewProvider viewProvider = getViewProvider();
    if (viewProvider == null) return null;

    final PsiFile dataFile = viewProvider.getPsi(language);
    if (dataFile == null || !isAcceptableBaseLanguageFile(dataFile)) return null;

    final PsiStructureViewFactory factory = LanguageStructureViewBuilder.INSTANCE.forLanguage(language);
    if (factory == null) return null;

    final StructureViewBuilder builder = factory.getStructureViewBuilder(dataFile);
    if (builder == null) return null;

    StructureView structureView = builder.createStructureView(fileEditor, myProject);
    return new StructureViewComposite.StructureViewDescriptor(IdeBundle.message("tab.structureview.baselanguage.view", language.getDisplayName()), structureView, findFileType(language).getIcon());
  }

  protected boolean isAcceptableBaseLanguageFile(PsiFile dataFile) {
    return true;
  }

  private void updateTemplateDataFileView() {
    final TemplateLanguageFileViewProvider provider = getViewProvider();
    final Language newDataLanguage = provider == null ? null : provider.getTemplateDataLanguage();

    if (myBaseStructureViewDescriptor != null) {
      if (myTemplateDataLanguage == newDataLanguage) return;

      Disposer.dispose(myBaseStructureViewDescriptor.structureView);
    }

    if (newDataLanguage != null) {
      myBaseStructureViewDescriptor = createBaseLanguageStructureView(myFileEditor, newDataLanguage);
      if (myStructureViewComposite != null) {
        myStructureViewComposite.setStructureView(myBaseLanguageViewDescriptorIndex, myBaseStructureViewDescriptor);
      }
    }
  }

  @NotNull
  private static FileType findFileType(final Language language) {
    FileType[] registeredFileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
    for (FileType fileType : registeredFileTypes) {
      if (fileType instanceof LanguageFileType && ((LanguageFileType)fileType).getLanguage() == language) {
        return fileType;
      }
    }
    return UnknownFileType.INSTANCE;
  }
}
