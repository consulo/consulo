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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import consulo.logging.Logger;
import consulo.roots.OrderEntryWithTracking;
import consulo.roots.impl.ModuleRootLayerImpl;
import consulo.roots.orderEntry.OrderEntryType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * @author dsl
 */
public abstract class LibraryOrderEntryBaseImpl extends OrderEntryBaseImpl implements OrderEntryWithTracking {
  public static final Logger LOGGER = Logger.getInstance(LibraryOrderEntryBaseImpl.class);

  protected final ProjectRootManagerImpl myProjectRootManagerImpl;
  @Nonnull
  protected DependencyScope myScope = DependencyScope.COMPILE;
  @Nullable
  private RootProvider myCurrentlySubscribedRootProvider = null;

  public LibraryOrderEntryBaseImpl(@Nonnull OrderEntryType<?> provider, @Nonnull ModuleRootLayerImpl rootModel,
                                   @Nonnull ProjectRootManagerImpl instanceImpl) {
    super(provider, rootModel);
    myProjectRootManagerImpl = instanceImpl;
  }

  public final void init() {
    updateFromRootProviderAndSubscribe();
  }

  @Nullable
  @Override
  public Object getEqualObject() {
    return null;
  }

  @Override
  @Nonnull
  public VirtualFile[] getFiles(@Nonnull OrderRootType type) {
    RootProvider rootProvider = getRootProvider();
    return rootProvider != null ? rootProvider.getFiles(type) : VirtualFile.EMPTY_ARRAY;  }

  @Override
  @Nonnull
  public String[] getUrls(@Nonnull OrderRootType type) {
    LOGGER.assertTrue(!getRootModel().getModule().isDisposed());
    RootProvider rootProvider = getRootProvider();
    return rootProvider == null ? ArrayUtil.EMPTY_STRING_ARRAY : rootProvider.getUrls(type);

  }

  @Nullable
  protected abstract RootProvider getRootProvider();

  @Override
  @Nonnull
  public final Module getOwnerModule() {
    return getRootModel().getModule();
  }

  @Override
  public boolean isEquivalentTo(@Nonnull OrderEntry other) {
    LOGGER.assertTrue(this instanceof LibraryOrderEntry);

    LibraryOrderEntry libraryOrderEntry1 = (LibraryOrderEntry)this;
    LibraryOrderEntry libraryOrderEntry2 = (LibraryOrderEntry)other;
    boolean equal = Comparing.equal(libraryOrderEntry1.getLibraryName(), libraryOrderEntry2.getLibraryName()) &&
                    Comparing.equal(libraryOrderEntry1.getLibraryLevel(), libraryOrderEntry2.getLibraryLevel());
    if (!equal) return false;

    Library library1 = libraryOrderEntry1.getLibrary();
    Library library2 = libraryOrderEntry2.getLibrary();
    if (library1 != null && library2 != null) {
      if (!Arrays.equals(((LibraryEx)library1).getExcludedRootUrls(), ((LibraryEx)library2).getExcludedRootUrls())) {
        return false;
      }
    }
    final List<OrderRootType> allTypes = OrderRootType.getAllTypes();
    for (OrderRootType type : allTypes) {
      final String[] orderedRootUrls1 = getUrls(type);
      final String[] orderedRootUrls2 = other.getUrls(type);
      if (!Arrays.equals(orderedRootUrls1, orderedRootUrls2)) {
        return false;
      }
    }
    return true;
  }

  protected void updateFromRootProviderAndSubscribe() {
    getRootModel().makeExternalChange(new Runnable() {
      @Override
      public void run() {
        resubscribe(getRootProvider());
      }
    });
  }

  private void resubscribe(RootProvider wrapper) {
    unsubscribe();
    subscribe(wrapper);
  }

  private void subscribe(@Nullable RootProvider wrapper) {
    if (wrapper != null) {
      myProjectRootManagerImpl.subscribeToRootProvider(this, wrapper);
    }
    myCurrentlySubscribedRootProvider = wrapper;
  }


  private void unsubscribe() {
    if (myCurrentlySubscribedRootProvider != null) {
      myProjectRootManagerImpl.unsubscribeFromRootProvider(this, myCurrentlySubscribedRootProvider);
    }
    myCurrentlySubscribedRootProvider = null;
  }

  @Override
  public void dispose() {
    unsubscribe();
    super.dispose();
  }
}
