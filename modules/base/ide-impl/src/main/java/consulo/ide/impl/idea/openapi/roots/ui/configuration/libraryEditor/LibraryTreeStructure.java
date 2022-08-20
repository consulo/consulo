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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.content.OrderRootType;
import consulo.content.library.ui.LibraryEditor;
import consulo.content.library.ui.LibraryRootsComponentDescriptor;
import consulo.content.library.ui.OrderRootTypePresentation;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.project.ProjectBundle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LibraryTreeStructure extends AbstractTreeStructure {
  private final NodeDescriptor myRootElementDescriptor;
  private final LibraryRootsComponent myParentEditor;
  private final LibraryRootsComponentDescriptor myComponentDescriptor;

  public LibraryTreeStructure(LibraryRootsComponent parentElement, LibraryRootsComponentDescriptor componentDescriptor) {
    myParentEditor = parentElement;
    myComponentDescriptor = componentDescriptor;
    myRootElementDescriptor = new NodeDescriptor(null) {
      @RequiredUIAccess
      @Override
      public boolean update() {
        myName = ProjectBundle.message("library.root.node");
        return false;
      }

      @Override
      public Object getElement() {
        return this;
      }
    };
  }

  @Nonnull
  @Override
  public Object getRootElement() {
    return myRootElementDescriptor;
  }

  @Nonnull
  @Override
  public Object[] getChildElements(@Nonnull Object element) {
    final LibraryEditor libraryEditor = myParentEditor.getLibraryEditor();
    if (element == myRootElementDescriptor) {
      ArrayList<LibraryTableTreeContentElement> elements = new ArrayList<>(3);
      for (OrderRootType type : myComponentDescriptor.getRootTypes()) {
        final String[] urls = libraryEditor.getUrls(type);
        if (urls.length > 0) {
          OrderRootTypePresentation presentation = myComponentDescriptor.getRootTypePresentation(type);
          if (presentation == null) {
            presentation = getDefaultPresentation(type);
          }
          elements.add(new OrderRootTypeElement(myRootElementDescriptor, type, presentation.getNodeText(), presentation.getIcon()));
        }
      }
      return elements.toArray();
    }

    if (element instanceof OrderRootTypeElement) {
      OrderRootTypeElement rootTypeElement = (OrderRootTypeElement)element;
      OrderRootType orderRootType = rootTypeElement.getOrderRootType();
      ArrayList<ItemElement> items = new ArrayList<>();
      final String[] urls = libraryEditor.getUrls(orderRootType).clone();
      Arrays.sort(urls, LibraryRootsComponent.ourUrlComparator);
      for (String url : urls) {
        items.add(new ItemElement(rootTypeElement, url, orderRootType, libraryEditor.isJarDirectory(url, orderRootType), libraryEditor.isValid(url, orderRootType)));
      }
      return items.toArray();
    }

    if (element instanceof ItemElement) {
      ItemElement itemElement = (ItemElement)element;
      List<String> excludedUrls = new ArrayList<>();
      for (String excludedUrl : libraryEditor.getExcludedRootUrls()) {
        if (VirtualFileUtil.isEqualOrAncestor(itemElement.getUrl(), excludedUrl)) {
          excludedUrls.add(excludedUrl);
        }
      }
      ExcludedRootElement[] items = new ExcludedRootElement[excludedUrls.size()];
      Collections.sort(excludedUrls, LibraryRootsComponent.ourUrlComparator);
      for (int i = 0; i < excludedUrls.size(); i++) {
        items[i] = new ExcludedRootElement(itemElement, itemElement.getUrl(), excludedUrls.get(i));
      }
      return items;
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  private static OrderRootTypePresentation getDefaultPresentation(OrderRootType type) {
    final OrderRootTypeUIFactory factory = OrderRootTypeUIFactory.forOrderType(type);
    return new OrderRootTypePresentation(factory.getNodeText(), factory.getIcon());
  }

  @Override
  public void commit() {
  }

  @Override
  public boolean hasSomethingToCommit() {
    return false;
  }

  @Override
  public Object getParentElement(@Nonnull Object element) {
    return ((NodeDescriptor)element).getParentDescriptor();
  }

  @Override
  @Nonnull
  public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
    return (NodeDescriptor)element;
  }
}
