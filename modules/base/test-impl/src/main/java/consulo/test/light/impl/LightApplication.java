/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import com.intellij.concurrency.JobLauncher;
import com.intellij.ide.UiActivityMonitor;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.treeView.TreeAnchorizer;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.impl.PsiBuilderFactoryImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.impl.ModalityStateEx;
import com.intellij.openapi.components.ExtensionAreas;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.extensions.impl.UndefinedPluginDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.psi.LanguageSubstitutors;
import com.intellij.ui.ExpandableItemsHandlerFactory;
import com.intellij.ui.TreeUIHelper;
import com.intellij.util.KeyedLazyInstanceEP;
import consulo.annotations.RequiredReadAction;
import consulo.annotations.RequiredWriteAction;
import consulo.application.options.PathMacrosService;
import consulo.extensions.ExtensionExtender;
import consulo.injecting.InjectingContainerBuilder;
import consulo.psi.tree.ASTCompositeFactory;
import consulo.psi.tree.ASTLazyFactory;
import consulo.psi.tree.ASTLeafFactory;
import consulo.psi.tree.impl.DefaultASTCompositeFactory;
import consulo.psi.tree.impl.DefaultASTLazyFactory;
import consulo.psi.tree.impl.DefaultASTLeafFactory;
import consulo.ui.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.awt.*;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplication extends ComponentManagerImpl implements Application {
  private final Disposable myLastDisposable;

  private ModalityState myNoneModalityState;

  public LightApplication(Disposable lastDisposable) {
    super(null, "LightApplication", ExtensionAreas.APPLICATION);
    myLastDisposable = lastDisposable;

    ApplicationManager.setApplication(this, myLastDisposable);
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(Application.class).to(this);
  }

  @Override
  protected void registerExtensionPointsAndExtensions(ExtensionsAreaImpl area) {
    registerExtensionPoint(area, ASTLazyFactory.EP.getExtensionPointName(), ASTLazyFactory.class);
    registerExtension(area, ASTLazyFactory.EP.getExtensionPointName(), new DefaultASTLazyFactory());

    registerExtensionPoint(area, ASTLeafFactory.EP.getExtensionPointName(), ASTLeafFactory.class);
    registerExtension(area, ASTLeafFactory.EP.getExtensionPointName(), new DefaultASTLeafFactory());

    registerExtensionPoint(area, ASTCompositeFactory.EP.getExtensionPointName(), ASTCompositeFactory.class);
    registerExtension(area, ASTCompositeFactory.EP.getExtensionPointName(), new DefaultASTCompositeFactory());

    registerExtensionPoint(area, LanguageParserDefinitions.INSTANCE.getExtensionPointName(), LanguageExtensionPoint.class);
    registerExtensionPoint(area, LanguageSubstitutors.INSTANCE.getExtensionPointName(), LanguageExtensionPoint.class);

    registerExtensionPoint(area, PathMacroFilter.EP_NAME, PathMacroFilter.class);
    registerExtensionPoint(area, ExtensionExtender.EP_NAME, KeyedLazyInstanceEP.class);
  }

  private <T> void registerExtension(ExtensionsAreaImpl area, ExtensionPointName<T> extensionPointName, T value) {
    ExtensionPointImpl<T> point = (ExtensionPointImpl<T>)area.getExtensionPoint(extensionPointName);
    point.registerExtensionAdapter(new SimpleInstanceComponentAdapter<>(value));
  }

  private void registerExtensionPoint(ExtensionsAreaImpl area, ExtensionPointName name, Class aClass) {
    ExtensionPoint.Kind kind = aClass.isInterface() || (aClass.getModifiers() & Modifier.ABSTRACT) != 0 ? ExtensionPoint.Kind.INTERFACE : ExtensionPoint.Kind.BEAN_CLASS;
    area.registerExtensionPoint(name.getName(), aClass.getName(), new UndefinedPluginDescriptor(), kind);
  }

  @Override
  protected void registerServices(InjectingContainerBuilder builder) {
    builder.bind(PsiBuilderFactory.class).to(PsiBuilderFactoryImpl.class);
    builder.bind(FileTypeRegistry.class).to(LightFileTypeRegistry.class);
    builder.bind(FileDocumentManager.class).to(LightFileDocumentManager.class);
    builder.bind(JobLauncher.class).to(LightJobLauncher.class);
    builder.bind(EncodingManager.class).to(LightEncodingManager.class);
    builder.bind(PathMacrosService.class).to(LightPathMacrosService.class);
    builder.bind(PathMacros.class).to(LightPathMacros.class);
    builder.bind(UISettings.class);
    builder.bind(ExpandableItemsHandlerFactory.class).to(LightExpandableItemsHandlerFactory.class);
    builder.bind(TreeUIHelper.class).to(LightTreeUIHelper.class);
    builder.bind(UiActivityMonitor.class).to(LightUiActivityMonitor.class);
    builder.bind(TreeAnchorizer.class).to(TreeAnchorizer.class);
  }

  @Override
  public void runReadAction(@Nonnull Runnable action) {
    action.run();
  }

  @Override
  public <T> T runReadAction(@Nonnull Computable<T> computation) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void runWriteAction(@Nonnull Runnable action) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public <T> T runWriteAction(@Nonnull Computable<T> computation) {
    throw new UnsupportedOperationException();
  }

  @RequiredReadAction
  @Override
  public boolean hasWriteAction(@Nonnull Class<?> actionClass) {
    throw new UnsupportedOperationException();
  }

  @RequiredReadAction
  @Override
  public void assertReadAccessAllowed() {

  }

  @RequiredWriteAction
  @Override
  public void assertWriteAccessAllowed() {

  }

  @RequiredUIAccess
  @Override
  public void assertIsDispatchThread() {

  }

  @Override
  public void addApplicationListener(@Nonnull ApplicationListener listener) {

  }

  @Override
  public void addApplicationListener(@Nonnull ApplicationListener listener, @Nonnull Disposable parent) {

  }

  @Override
  public void removeApplicationListener(@Nonnull ApplicationListener listener) {

  }

  @RequiredUIAccess
  @Override
  public void saveAll() {

  }

  @Override
  public void saveSettings() {

  }

  @Override
  public void exit() {

  }

  @Override
  public boolean isReadAccessAllowed() {
    return true;
  }

  @Override
  public boolean isDispatchThread() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWriteThread() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull Condition expired) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState state, @Nonnull Condition expired) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public ModalityState getCurrentModalityState() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public ModalityState getModalityStateForComponent(@Nonnull Component c) {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getDefaultModalityState() {
    return getNoneModalityState();
  }

  @Nonnull
  @Override
  public ModalityState getNoneModalityState() {
    if (myNoneModalityState == null) {
      myNoneModalityState = new ModalityStateEx() {
        @Override
        public boolean dominates(@Nonnull ModalityState anotherState) {
          return false;
        }

        @Override
        public String toString() {
          return "NONE";
        }
      };
    }
    return myNoneModalityState;
  }

  @Nonnull
  @Override
  public ModalityState getAnyModalityState() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getStartTime() {
    return 0;
  }

  @RequiredUIAccess
  @Override
  public long getIdleTime() {
    return 0;
  }

  @Override
  public boolean isHeadlessEnvironment() {
    return true;
  }

  @Nonnull
  @Override
  public Future<?> executeOnPooledThread(@Nonnull Runnable action) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T> Future<T> executeOnPooledThread(@Nonnull Callable<T> action) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDisposeInProgress() {
    return false;
  }

  @Override
  public boolean isRestartCapable() {
    return false;
  }

  @Override
  public void restart() {

  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public AccessToken acquireReadActionLock() {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AccessToken acquireWriteActionLock(@Nonnull Class marker) {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }

  @RequiredUIAccess
  @Override
  public <T, E extends Throwable> T runWriteAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T, E extends Throwable> T runReadAction(@Nonnull ThrowableComputable<T, E> computation) throws E {
    throw new UnsupportedOperationException();
  }
}
