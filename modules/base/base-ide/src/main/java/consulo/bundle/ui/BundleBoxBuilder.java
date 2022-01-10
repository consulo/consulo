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
package consulo.bundle.ui;

import com.google.common.base.Predicates;
import consulo.disposer.Disposable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import consulo.ide.settings.impl.ProjectStructureSettingsUtil;
import consulo.ui.ComboBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.model.MutableListModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2020-01-20
 */
public final class BundleBoxBuilder {

  @Nonnull
  public static BundleBoxBuilder create(@Nullable Disposable uiDisposable) {
    return create(null, uiDisposable);
  }

  @Nonnull
  public static BundleBoxBuilder create(@Nullable SdkModel sdkModel, @Nullable Disposable uiDisposable) {
    return new BundleBoxBuilder(sdkModel, uiDisposable);
  }

  @Nullable
  private final SdkModel mySdkModel;

  @Nullable
  private final Disposable myUIDisposable;

  private boolean myWithNoneItem;

  private String myNoneItemName = ProjectBundle.message("sdk.combo.box.item");

  private Image myNoneItemImage;

  private Predicate<SdkTypeId> mySdkFilter = Predicates.alwaysTrue();

  private BundleBoxBuilder(@Nullable SdkModel sdkModel, @Nullable Disposable uiDisposable) {
    mySdkModel = sdkModel;
    myUIDisposable = uiDisposable;
  }

  @Nonnull
  public BundleBoxBuilder withSdkTypeFilterByClass(@Nonnull final Class<? extends SdkTypeId> clazz) {
    mySdkFilter = sdkTypeId -> clazz.isAssignableFrom(sdkTypeId.getClass());
    return this;
  }

  @Nonnull
  public BundleBoxBuilder withSdkTypeFilter(@Nonnull Predicate<SdkTypeId> sdkFilter) {
    mySdkFilter = sdkFilter;
    return this;
  }

  @Nonnull
  public BundleBoxBuilder withSdkTypeFilterBySet(@Nonnull final Set<? extends SdkType> sdkTypes) {
    mySdkFilter = sdkTypes::contains;
    return this;
  }

  @Nonnull
  public BundleBoxBuilder withSdkTypeFilterByType(@Nonnull final SdkType sdkType) {
    return withSdkTypeFilterBySet(Collections.singleton(sdkType));
  }

  @Nonnull
  public BundleBoxBuilder withNoneItem() {
    myWithNoneItem = true;
    return this;
  }

  @Nonnull
  public BundleBoxBuilder withNoneItem(@Nonnull String noneItemName) {
    myWithNoneItem = true;
    myNoneItemName = noneItemName;
    return this;
  }

  @Nonnull
  public BundleBoxBuilder withNoneItemImage(@Nonnull Image noneItemImage) {
    myNoneItemImage = noneItemImage;
    return this;
  }

  @Nonnull
  public BundleBoxBuilder withNoneItem(@Nonnull String noneItemName, @Nonnull Image noneItemImage) {
    myWithNoneItem = true;
    myNoneItemName = noneItemName;
    myNoneItemImage = noneItemImage;
    return this;
  }

  @Nonnull
  private static SdkModel effectiveModel(@Nullable SdkModel sdkModel) {
    if (sdkModel == null) {
      ProjectStructureSettingsUtil sdksSettingsUtil = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();

      return sdksSettingsUtil.getSdksModel();
    }
    else {
      return sdkModel;
    }
  }

  @Nonnull
  public BundleBox build() {
    final SdkModel sdkModel = effectiveModel(mySdkModel);

    BundleBox box = new BundleBox(sdkModel, mySdkFilter, myWithNoneItem ? myNoneItemName : null, myWithNoneItem ? myNoneItemImage : null);

    if (myUIDisposable != null) {
      sdkModel.addListener(new SdkModel.Listener() {
        @RequiredUIAccess
        @Override
        public void sdkAdded(Sdk sdk) {
          sdkChanged();
        }

        @RequiredUIAccess
        @Override
        public void sdkRemove(Sdk sdk) {
          sdkChanged();
        }

        @RequiredUIAccess
        @Override
        public void sdkChanged(Sdk sdk, String previousName) {
          sdkChanged();
        }

        @RequiredUIAccess
        @Override
        public void sdkHomeSelected(Sdk sdk, String newSdkHome) {
          sdkChanged();
        }

        @RequiredUIAccess
        private void sdkChanged() {
          UIAccess.current().give(this::sdkChangedImpl);
        }

        @RequiredUIAccess
        private void sdkChangedImpl() {
          ComboBox<BundleBox.BundleBoxItem> component = box.getComponent();

          MutableListModel<BundleBox.BundleBoxItem> listModel = (MutableListModel<BundleBox.BundleBoxItem>)component.getListModel();

          BundleBox.BundleBoxItem bundleBoxItem = component.getValue();

          List<BundleBox.BundleBoxItem> newItems = BundleBox.buildItems(sdkModel, mySdkFilter, myWithNoneItem);

          List<BundleBox.BundleBoxItem> bundleBoxItems = listModel.replaceAll(newItems);
          for (BundleBox.BundleBoxItem boxItem : bundleBoxItems) {
            if (boxItem instanceof BundleBox.CustomBundleBoxItem || boxItem instanceof BundleBox.ModuleExtensionBundleBoxItem) {
              listModel.add(boxItem);
            }
          }

          boolean selected = false;
          for (BundleBox.BundleBoxItem newItem : newItems) {
            if (newItem.equals(bundleBoxItem)) {
              component.setValue(newItem);
              selected = true;
              break;
            }
          }

          if (!selected) {
            if(bundleBoxItem == null || bundleBoxItem instanceof BundleBox.NullBundleBoxItem) {
              box.setSelectedNoneBundle();
            }
            else {
              box.setSelectedBundle(bundleBoxItem.getBundleName());
            }
          }
        }
      }, myUIDisposable);
    }

    return box;
  }
}
