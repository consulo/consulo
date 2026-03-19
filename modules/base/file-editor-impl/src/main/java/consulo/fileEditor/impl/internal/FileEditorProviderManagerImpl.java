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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
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
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
  @RequiredReadAction
  public FileEditorProvider[] getProviders(Project project, VirtualFile file) {
    // Collect all possible editors
    List<FileEditorProvider> sharedProviders = new ArrayList<>();
    boolean doNotShowTextEditor = false;
    boolean projectIsDumb = DumbService.isDumb(project);
    for (FileEditorProvider provider : myApplication.getExtensionList(FileEditorProvider.class)) {
      ThrowableComputable<Boolean, RuntimeException> action = () -> {
        if (projectIsDumb && !DumbService.isDumbAware(provider)) {
          return false;
        }
        return provider.accept(project, file);
      };
      if (action.get()) {
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
  public @Nullable FileEditorProvider getProvider(String editorTypeId) {
    for (FileEditorProvider provider : myApplication.getExtensionList(FileEditorProvider.class)) {
      if (provider.getEditorTypeId().equals(editorTypeId)) {
        return provider;
      }
    }
    return null;
  }

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

  public @Nullable FileEditorProvider getSelectedFileEditorProvider(EditorHistoryManager editorHistoryManager, VirtualFile file, FileEditorProvider[] providers) {
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
      int i1 = provider1.getPolicy().ordinal();
      int i2 = provider2.getPolicy().ordinal();
      if (i1 != i2) return i1 - i2;
      double value = getWeight(provider1) - getWeight(provider2);
      return value > 0 ? 1 : value < 0 ? -1 : 0;
    }
  }
}
