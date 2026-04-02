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
package consulo.module.ui;

import consulo.content.bundle.*;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ComboBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.model.MutableListModel;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2020-01-20
 */
public final class BundleBoxBuilder {
  public static BundleBoxBuilder create(@Nullable Disposable uiDisposable) {
    return create(null, uiDisposable);
  }

  public static BundleBoxBuilder create(@Nullable SdkModel sdkModel, @Nullable Disposable uiDisposable) {
    return new BundleBoxBuilder(sdkModel, uiDisposable);
  }

  private final @Nullable SdkModel mySdkModel;

  private final @Nullable Disposable myUIDisposable;

  private boolean myWithNoneItem;

  private LocalizeValue myNoneItemName = ProjectLocalize.sdkComboBoxItem();

  private Image myNoneItemImage;

  private Predicate<SdkTypeId> mySdkFilter = sdkTypeId -> true;

  private BundleBoxBuilder(@Nullable SdkModel sdkModel, @Nullable Disposable uiDisposable) {
    mySdkModel = sdkModel;
    myUIDisposable = uiDisposable;
  }

  public BundleBoxBuilder withSdkTypeFilterByClass(Class<? extends SdkTypeId> clazz) {
    mySdkFilter = sdkTypeId -> clazz.isAssignableFrom(sdkTypeId.getClass());
    return this;
  }

  public BundleBoxBuilder withSdkTypeFilter(Predicate<SdkTypeId> sdkFilter) {
    mySdkFilter = sdkFilter;
    return this;
  }

  public BundleBoxBuilder withSdkTypeFilterBySet(Set<? extends SdkType> sdkTypes) {
    mySdkFilter = sdkTypes::contains;
    return this;
  }

  public BundleBoxBuilder withSdkTypeFilterByType(SdkType sdkType) {
    return withSdkTypeFilterBySet(Collections.singleton(sdkType));
  }

  public BundleBoxBuilder withNoneItem() {
    myWithNoneItem = true;
    return this;
  }

  public BundleBoxBuilder withNoneItem(LocalizeValue noneItemName) {
    myWithNoneItem = true;
    myNoneItemName = noneItemName;
    return this;
  }

  public BundleBoxBuilder withNoneItemImage(Image noneItemImage) {
    myNoneItemImage = noneItemImage;
    return this;
  }

  public BundleBoxBuilder withNoneItem(LocalizeValue noneItemName, Image noneItemImage) {
    myWithNoneItem = true;
    myNoneItemName = noneItemName;
    myNoneItemImage = noneItemImage;
    return this;
  }

  private static SdkModel effectiveModel(@Nullable SdkModel sdkModel) {
    if (sdkModel == null) {
      return SdkModelFactory.getInstance().getOrCreateModel();
    }
    else {
      return sdkModel;
    }
  }

  @RequiredUIAccess
  public BundleBox build() {
    final SdkModel sdkModel = effectiveModel(mySdkModel);

    BundleBox box = new BundleBox(sdkModel,
        mySdkFilter,
        myWithNoneItem ? myNoneItemName : LocalizeValue.empty(),
        myWithNoneItem ? myNoneItemImage : null
    );

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
