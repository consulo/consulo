/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.fileEditor.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.fileEditor.*;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@Singleton
@ServiceImpl
@State(name = "FileEditorProviderManager", storages = @Storage(value = "fileEditorProviderManager.xml", roamingType = RoamingType.DISABLED))
public final class FileEditorProviderManagerImpl extends FileEditorProviderManager implements PersistentStateComponent<FileEditorProviderManagerState> {

  private final Application myApplication;

  private FileEditorProviderManagerState myState = new FileEditorProviderManagerState();

  @Inject
  public FileEditorProviderManagerImpl(Application application) {
    myApplication = application;
  }

  @Override
  @Nonnull
  public FileEditorProvider[] getProviders(@Nonnull final Project project, @Nonnull final VirtualFile file) {
    // Collect all possible editors
    List<FileEditorProvider> sharedProviders = new ArrayList<>();
    boolean doNotShowTextEditor = false;
    for (final FileEditorProvider provider : myApplication.getExtensionList(FileEditorProvider.class)) {
      ThrowableComputable<Boolean, RuntimeException> action = () -> {
        if (DumbService.isDumb(project) && !DumbService.isDumbAware(provider)) {
          return false;
        }
        return provider.accept(project, file);
      };
      if (AccessRule.read(action)) {
        sharedProviders.add(provider);
        doNotShowTextEditor |= provider.getPolicy() == FileEditorPolicy.HIDE_DEFAULT_EDITOR;
      }
    }

    // Throw out default editors provider if necessary
    if (doNotShowTextEditor) {
      ContainerUtil.retainAll(sharedProviders, provider -> !(provider instanceof TextEditorProvider));
    }

    // Sort editors according policies
    Collections.sort(sharedProviders, MyComparator.ourInstance);

    return sharedProviders.toArray(new FileEditorProvider[sharedProviders.size()]);
  }

  @Override
  @Nullable
  public FileEditorProvider getProvider(@Nonnull String editorTypeId) {
    for (FileEditorProvider provider : myApplication.getExtensionList(FileEditorProvider.class)) {
      if (provider.getEditorTypeId().equals(editorTypeId)) {
        return provider;
      }
    }
    return null;
  }

  @Nonnull
  @Override
  public FileEditorProviderManagerState getState() {
    return myState;
  }

  @Override
  public void loadState(FileEditorProviderManagerState state) {
    XmlSerializerUtil.copyBean(this, myState);
  }

  public void providerSelected(FileEditorComposite composite) {
    if (!(composite instanceof FileEditorWithProviderComposite)) return;
    FileEditorProvider[] providers = ((FileEditorWithProviderComposite)composite).getProviders();
    if (providers.length < 2) return;
    myState.getSelectedProviders().put(computeKey(providers), composite.getSelectedEditorWithProvider().getProvider().getEditorTypeId());
  }

  private static String computeKey(FileEditorProvider[] providers) {
    return StringUtil.join(ContainerUtil.map(providers, FileEditorProvider::getEditorTypeId), ",");
  }

  @Nullable
 public FileEditorProvider getSelectedFileEditorProvider(EditorHistoryManager editorHistoryManager, VirtualFile file, FileEditorProvider[] providers) {
    FileEditorProvider provider = editorHistoryManager.getSelectedProvider(file);
    if (provider != null || providers.length < 2) {
      return provider;
    }
    String id = myState.getSelectedProviders().get(computeKey(providers));
    return id == null ? null : getProvider(id);
  }


  private static final class MyComparator implements Comparator<FileEditorProvider> {
    public static final MyComparator ourInstance = new MyComparator();

    private static double getWeight(FileEditorProvider provider) {
      return provider instanceof WeighedFileEditorProvider ? ((WeighedFileEditorProvider)provider).getWeight() : Double.MAX_VALUE;
    }

    @Override
    public int compare(FileEditorProvider provider1, FileEditorProvider provider2) {
      final int i1 = provider1.getPolicy().ordinal();
      final int i2 = provider2.getPolicy().ordinal();
      if (i1 != i2) return i1 - i2;
      final double value = getWeight(provider1) - getWeight(provider2);
      return value > 0 ? 1 : value < 0 ? -1 : 0;
    }
  }
}
