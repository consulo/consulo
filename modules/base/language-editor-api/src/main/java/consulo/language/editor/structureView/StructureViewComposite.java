/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.language.editor.structureView;

import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.structureView.StructureView;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.navigation.ItemPresentation;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author cdr
 */
public class StructureViewComposite implements StructureView {

  private final StructureViewDescriptor[] myStructureViews;

  public static class StructureViewDescriptor {
    public final String title;
    public final StructureViewModel structureModel;
    public final StructureView structureView;
    public final Image icon;

    public StructureViewDescriptor(String title, @Nonnull StructureView structureView, Image icon) {
      this.title = title;
      this.structureModel = structureView.getTreeModel();
      this.structureView = structureView;
      this.icon = icon;
    }

    public StructureViewDescriptor(String title, @Nonnull StructureViewModel structureModel, Image icon) {
      this.title = title;
      this.structureModel = structureModel;
      this.structureView = null;
      this.icon = icon;
    }
  }

  public StructureViewComposite(@Nonnull StructureViewDescriptor... views) {
    myStructureViews = views;
    for (StructureViewDescriptor descriptor : views) {
      Disposer.register(this, descriptor.structureView);
    }
  }

  public boolean isOutdated() {
    return false;
  }

  @Nullable
  public StructureView getSelectedStructureView() {
    StructureViewDescriptor descriptor = ArrayUtil.getFirstElement(myStructureViews);
    return descriptor == null ? null : descriptor.structureView;
  }

  @Override
  public FileEditor getFileEditor() {
    StructureView view = getSelectedStructureView();
    return view == null ? null : view.getFileEditor();
  }

  @Override
  public boolean navigateToSelectedElement(final boolean requestFocus) {
    StructureView view = getSelectedStructureView();
    return view != null && view.navigateToSelectedElement(requestFocus);
  }

  @Override
  public JComponent getComponent() {
    StructureView view = getSelectedStructureView();
    return view == null ? null : view.getComponent();
  }

  @Override
  public void dispose() {
  }

  @Override
  public void centerSelectedRow() {
    StructureView view = getSelectedStructureView();
    if (view != null) view.centerSelectedRow();
  }

  @Override
  public void restoreState() {
    for (StructureViewDescriptor descriptor : myStructureViews) {
      descriptor.structureView.restoreState();
    }
  }

  @Override
  public void storeState() {
    for (StructureViewDescriptor descriptor : myStructureViews) {
      descriptor.structureView.storeState();
    }
  }

  @Nonnull
  public StructureViewDescriptor[] getStructureViews() {
    return myStructureViews;
  }

  @Override
  @Nonnull
  public StructureViewModel getTreeModel() {
    StructureView view = getSelectedStructureView();
    if (view != null) return view.getTreeModel();
    class M extends TextEditorBasedStructureViewModel implements StructureViewTreeElement, ItemPresentation {
      M() {
        super(null, null);
      }

      @Nonnull
      @Override
      public StructureViewTreeElement getRoot() {
        return this;
      }

      @Override
      public Object getValue() {
        return null;
      }

      @Nonnull
      @Override
      public ItemPresentation getPresentation() {
        return this;
      }

      @Nonnull
      @Override
      public TreeElement[] getChildren() {
        return EMPTY_ARRAY;
      }

      @Nullable
      @Override
      public String getPresentableText() {
        return null;
      }

      @Nullable
      @Override
      public String getLocationString() {
        return null;
      }

      @Nullable
      @Override
      public Image getIcon() {
        return null;
      }

      @Override
      public void navigate(boolean requestFocus) {
      }

      @Override
      public boolean canNavigate() {
        return false;
      }

      @Override
      public boolean canNavigateToSource() {
        return false;
      }
    }
    return new M();
  }
}
