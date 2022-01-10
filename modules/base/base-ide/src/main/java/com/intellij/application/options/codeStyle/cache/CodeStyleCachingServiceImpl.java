// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.cache;

import consulo.disposer.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.ObjectUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class CodeStyleCachingServiceImpl implements CodeStyleCachingService, Disposable {
  public final static int MAX_CACHE_SIZE = 100;

  private final static Key<CodeStyleCachedValueProvider> PROVIDER_KEY = Key.create("code.style.cached.value.provider");

  private final Map<String, FileData> myFileDataCache = new HashMap<>();

  private final Object CACHE_LOCK = new Object();

  private final PriorityQueue<FileData> myRemoveQueue = new PriorityQueue<>(MAX_CACHE_SIZE, Comparator.comparingLong(fileData -> fileData.lastRefTimeStamp));

  public CodeStyleCachingServiceImpl() {
  }

  @Override
  @Nullable
  public CodeStyleSettings tryGetSettings(@Nonnull PsiFile file) {
    CodeStyleCachedValueProvider provider = getOrCreateCachedValueProvider(file);
    return provider != null ? provider.tryGetSettings() : null;
  }

  @Override
  public void scheduleWhenSettingsComputed(@Nonnull PsiFile file, @Nonnull Runnable runnable) {
    CodeStyleCachedValueProvider provider = getOrCreateCachedValueProvider(file);
    if (provider != null) {
      provider.scheduleWhenComputed(runnable);
    }
    else {
      runnable.run();
    }
  }

  @Nullable
  private CodeStyleCachedValueProvider getOrCreateCachedValueProvider(@Nonnull PsiFile file) {
    synchronized (CACHE_LOCK) {
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile != null) {
        FileData fileData = getOrCreateFileData(getFileKey(virtualFile));
        CodeStyleCachedValueProvider provider = fileData.getUserData(PROVIDER_KEY);
        if (provider == null || provider.isExpired()) {
          provider = new CodeStyleCachedValueProvider(file);
          fileData.putUserData(PROVIDER_KEY, provider);
        }
        return provider;
      }
      return null;
    }
  }

  private void clearCache() {
    synchronized (CACHE_LOCK) {
      myFileDataCache.values().forEach(fileData -> {
        ObjectUtil.consumeIfNotNull(fileData.getUserData(PROVIDER_KEY), provider -> provider.cancelComputation());
      });
      myFileDataCache.clear();
      myRemoveQueue.clear();
    }
  }


  @Override
  @Nullable
  public UserDataHolder getDataHolder(@Nonnull VirtualFile virtualFile) {
    return getOrCreateFileData(getFileKey(virtualFile));
  }

  @Nonnull
  private synchronized FileData getOrCreateFileData(@Nonnull String path) {
    if (myFileDataCache.containsKey(path)) {
      final FileData fileData = myFileDataCache.get(path);
      fileData.update();
      return fileData;
    }
    FileData newData = new FileData();
    if (myFileDataCache.size() >= MAX_CACHE_SIZE) {
      FileData fileData = myRemoveQueue.poll();
      if (fileData != null) {
        myFileDataCache.values().remove(fileData);
      }
    }
    myFileDataCache.put(path, newData);
    myRemoveQueue.add(newData);
    return newData;
  }

  @Nonnull
  private static String getFileKey(VirtualFile file) {
    return file.getUrl();
  }

  @Override
  public void dispose() {
    clearCache();
  }

  private static final class FileData extends UserDataHolderBase {
    private long lastRefTimeStamp;

    private FileData() {
      update();
    }

    void update() {
      lastRefTimeStamp = System.currentTimeMillis();
    }
  }
}
