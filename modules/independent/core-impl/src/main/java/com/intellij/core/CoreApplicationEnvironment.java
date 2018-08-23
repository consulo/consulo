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
package com.intellij.core;

import com.intellij.codeInsight.folding.CodeFoldingSettings;
import com.intellij.concurrency.Job;
import com.intellij.concurrency.JobLauncher;
import com.intellij.lang.*;
import com.intellij.lang.impl.PsiBuilderFactoryImpl;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockApplicationEx;
import com.intellij.mock.MockFileDocumentManagerImpl;
import com.intellij.mock.MockReferenceProvidersRegistry;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.impl.CoreCommandProcessor;
import com.intellij.openapi.components.ExtensionAreas;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeExtension;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.util.ClassExtension;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.KeyedExtensionCollector;
import com.intellij.openapi.util.StaticGetter;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.impl.CoreVirtualFilePointerManager;
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl;
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.PsiReferenceServiceImpl;
import com.intellij.psi.impl.meta.MetaRegistry;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.meta.MetaDataRegistrar;
import com.intellij.psi.stubs.CoreStubTreeLoader;
import com.intellij.psi.stubs.StubTreeLoader;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import consulo.psi.tree.ASTCompositeFactory;
import consulo.psi.tree.ASTLazyFactory;
import consulo.psi.tree.ASTLeafFactory;
import consulo.psi.tree.impl.DefaultASTCompositeFactory;
import consulo.psi.tree.impl.DefaultASTLazyFactory;
import consulo.psi.tree.impl.DefaultASTLeafFactory;
import org.picocontainer.MutablePicoContainer;

import javax.annotation.Nonnull;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author yole
 */
public class CoreApplicationEnvironment {
  private final CoreFileTypeRegistry myFileTypeRegistry;
  protected final MockApplication myApplication;
  private final CoreLocalFileSystem myLocalFileSystem;
  protected final VirtualFileSystem myJarFileSystem;
  @Nonnull
  private final Disposable myParentDisposable;

  public CoreApplicationEnvironment(@Nonnull Disposable parentDisposable) {
    myParentDisposable = parentDisposable;

    myFileTypeRegistry = new CoreFileTypeRegistry();

    myApplication = createApplication(myParentDisposable);
    ApplicationManager.setApplication(myApplication,
                                      new StaticGetter<>(myFileTypeRegistry),
                                      myParentDisposable);
    myLocalFileSystem = createLocalFileSystem();
    myJarFileSystem = createJarFileSystem();

    Extensions.registerAreaClass(ExtensionAreas.PROJECT, null);

    final MutablePicoContainer appContainer = myApplication.getPicoContainer();
    registerComponentInstance(appContainer, FileDocumentManager.class, new MockFileDocumentManagerImpl(DocumentImpl::new, null));

    VirtualFileSystem[] fs = {myLocalFileSystem, myJarFileSystem};
    VirtualFileManagerImpl virtualFileManager = new VirtualFileManagerImpl(myApplication, fs);
    registerComponentInstance(appContainer, VirtualFileManager.class, virtualFileManager);

    registerApplicationExtensionPoint(ASTLazyFactory.EP.getExtensionPointName(), ASTLazyFactory.class);
    registerApplicationExtensionPoint(ASTLeafFactory.EP.getExtensionPointName(), ASTLeafFactory.class);
    registerApplicationExtensionPoint(ASTCompositeFactory.EP.getExtensionPointName(), ASTCompositeFactory.class);

    addExtension(ASTLazyFactory.EP.getExtensionPointName(), new DefaultASTLazyFactory());
    addExtension(ASTLeafFactory.EP.getExtensionPointName(), new DefaultASTLeafFactory());
    addExtension(ASTCompositeFactory.EP.getExtensionPointName(), new DefaultASTCompositeFactory());

    registerApplicationService(EncodingManager.class, new CoreEncodingRegistry());
    registerApplicationService(VirtualFilePointerManager.class, createVirtualFilePointerManager());
    registerApplicationService(PsiBuilderFactory.class, new PsiBuilderFactoryImpl());
    registerApplicationService(ReferenceProvidersRegistry.class, new MockReferenceProvidersRegistry());
    registerApplicationService(StubTreeLoader.class, new CoreStubTreeLoader());
    registerApplicationService(PsiReferenceService.class, new PsiReferenceServiceImpl());
    registerApplicationService(MetaDataRegistrar.class, new MetaRegistry());

    registerApplicationService(ProgressManager.class, createProgressIndicatorProvider());

    registerApplicationService(JobLauncher.class, createJobLauncher());
    registerApplicationService(CodeFoldingSettings.class, new CodeFoldingSettings());
    registerApplicationService(CommandProcessor.class, new CoreCommandProcessor());
    myApplication.registerService(ApplicationInfo.class, ApplicationInfoImpl.class);
  }

  public <T> void registerApplicationService(@Nonnull Class<T> serviceInterface, @Nonnull T serviceImplementation) {
    myApplication.registerService(serviceInterface, serviceImplementation);
  }

  @Nonnull
  protected VirtualFilePointerManager createVirtualFilePointerManager() {
    return new CoreVirtualFilePointerManager();
  }

  @Nonnull
  protected MockApplication createApplication(@Nonnull Disposable parentDisposable) {
    return new MockApplicationEx(parentDisposable);
  }

  @Nonnull
  protected JobLauncher createJobLauncher() {
    return new JobLauncher() {
      @Override
      public <T> boolean invokeConcurrentlyUnderProgress(@Nonnull List<T> things,
                                                         ProgressIndicator progress,
                                                         boolean runInReadAction,
                                                         boolean failFastOnAcquireReadAction,
                                                         @Nonnull Processor<? super T> thingProcessor) {
        for (T thing : things) {
          if (!thingProcessor.process(thing))
            return false;
        }
        return true;
      }

      @Nonnull
      @Override
      public Job<Void> submitToJobThread(@Nonnull Runnable action, Consumer<Future> onDoneCallback) {
        action.run();
        if (onDoneCallback != null)
          onDoneCallback.consume(new Future() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
              return false;
            }

            @Override
            public boolean isCancelled() {
              return false;
            }

            @Override
            public boolean isDone() {
              return true;
            }

            @Override
            public Object get() {
              return null;
            }

            @Override
            public Object get(long timeout, @Nonnull TimeUnit unit) {
              return null;
            }
          });
        return Job.NULL_JOB;
      }
    };
  }

  @Nonnull
  protected ProgressManager createProgressIndicatorProvider() {
    return new CoreProgressManager();
  }

  @Nonnull
  protected VirtualFileSystem createJarFileSystem() {
    return new CoreJarFileSystem();
  }

  @Nonnull
  protected CoreLocalFileSystem createLocalFileSystem() {
    return new CoreLocalFileSystem();
  }

  @Nonnull
  public MockApplication getApplication() {
    return myApplication;
  }

  @Nonnull
  public Disposable getParentDisposable() {
    return myParentDisposable;
  }

  public <T> void registerApplicationComponent(@Nonnull Class<T> interfaceClass, @Nonnull T implementation) {
    registerComponentInstance(myApplication.getPicoContainer(), interfaceClass, implementation);
  }

  public void registerFileType(@Nonnull FileType fileType, @Nonnull String extension) {
    myFileTypeRegistry.registerFileType(fileType, extension);
  }

  public void registerParserDefinition(@Nonnull ParserDefinition definition) {
    addExplicitExtension(LanguageParserDefinitions.INSTANCE, definition.getFileNodeType().getLanguage(), definition);
  }

  public static <T> void registerComponentInstance(@Nonnull MutablePicoContainer container, @Nonnull Class<T> key, @Nonnull T implementation) {
    container.unregisterComponent(key);
    container.registerComponentInstance(key, implementation);
  }

  public <T> void addExplicitExtension(@Nonnull LanguageExtension<T> instance, @Nonnull Language language, @Nonnull T object) {
    doAddExplicitExtension(instance, language, object);
  }

  public void registerParserDefinition(@Nonnull Language language, @Nonnull ParserDefinition parserDefinition) {
    addExplicitExtension(LanguageParserDefinitions.INSTANCE, language, parserDefinition);
  }

  public <T> void addExplicitExtension(@Nonnull final FileTypeExtension<T> instance, @Nonnull final FileType fileType, @Nonnull final T object) {
    doAddExplicitExtension(instance, fileType, object);
  }

  private <T,U> void doAddExplicitExtension(@Nonnull final KeyedExtensionCollector<T,U> instance, @Nonnull final U key, @Nonnull final T object) {
    instance.addExplicitExtension(key, object);
    Disposer.register(myParentDisposable, new Disposable() {
      @Override
      public void dispose() {
        instance.removeExplicitExtension(key, object);
      }
    });
  }

  public <T> void addExplicitExtension(@Nonnull final ClassExtension<T> instance, @Nonnull final Class aClass, @Nonnull final T object) {
    doAddExplicitExtension(instance, aClass, object);
  }

  public <T> void addExtension(@Nonnull ExtensionPointName<T> name, @Nonnull final T extension) {
    final ExtensionPoint<T> extensionPoint = Extensions.getRootArea().getExtensionPoint(name);
    extensionPoint.registerExtension(extension);
    Disposer.register(myParentDisposable, new Disposable() {
      @Override
      public void dispose() {
        extensionPoint.unregisterExtension(extension);
      }
    });
  }


  public static <T> void registerExtensionPoint(@Nonnull ExtensionsArea area,
                                                @Nonnull ExtensionPointName<T> extensionPointName,
                                                @Nonnull Class<? extends T> aClass) {
    final String name = extensionPointName.getName();
    registerExtensionPoint(area, name, aClass);
  }

  public static <T> void registerExtensionPoint(@Nonnull ExtensionsArea area, @Nonnull String name, @Nonnull Class<? extends T> aClass) {
    if (!area.hasExtensionPoint(name)) {
      ExtensionPoint.Kind kind = aClass.isInterface() || (aClass.getModifiers() & Modifier.ABSTRACT) != 0 ? ExtensionPoint.Kind.INTERFACE : ExtensionPoint.Kind.BEAN_CLASS;
      area.registerExtensionPoint(name, aClass.getName(), kind);
    }
  }

  public static <T> void registerApplicationExtensionPoint(@Nonnull ExtensionPointName<T> extensionPointName, @Nonnull Class<? extends T> aClass) {
    registerExtensionPoint(Extensions.getRootArea(), extensionPointName, aClass);
  }

  @Nonnull
  public CoreLocalFileSystem getLocalFileSystem() {
    return myLocalFileSystem;
  }

  @Nonnull
  public VirtualFileSystem getJarFileSystem() {
    return myJarFileSystem;
  }
}
