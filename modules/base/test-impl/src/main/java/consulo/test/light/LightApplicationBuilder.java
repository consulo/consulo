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
package consulo.test.light;

import consulo.application.Application;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.internal.concurrency.JobLauncher;
import consulo.application.macro.PathMacros;
import consulo.application.progress.ProgressManager;
import consulo.application.ui.UISettings;
import consulo.component.impl.extension.ExtensionsAreaImpl;
import consulo.component.store.impl.internal.PathMacrosService;
import consulo.disposer.Disposable;
import consulo.document.FileDocumentManager;
import consulo.injecting.InjectingContainerBuilder;
import consulo.language.impl.ast.ASTCompositeFactory;
import consulo.language.impl.ast.ASTLazyFactory;
import consulo.language.impl.ast.ASTLeafFactory;
import consulo.language.impl.internal.ast.DefaultASTCompositeFactory;
import consulo.language.impl.internal.ast.DefaultASTLazyFactory;
import consulo.language.impl.internal.ast.DefaultASTLeafFactory;
import consulo.language.impl.internal.parser.PsiBuilderFactoryImpl;
import consulo.language.parser.PsiBuilderFactory;
import consulo.test.light.impl.*;
import consulo.ui.ex.UiActivityMonitor;
import consulo.ui.ex.awt.ExpandableItemsHandlerFactory;
import consulo.ui.ex.awt.tree.TreeUIHelper;
import consulo.ui.ex.tree.TreeAnchorizer;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplicationBuilder {
  public static class DefaultRegistrator extends LightExtensionRegistrator {
    @Override
    public void registerExtensionPointsAndExtensions(@Nonnull ExtensionsAreaImpl area) {
      registerExtensionPoint(area, ASTLazyFactory.EP.getExtensionPointName(), ASTLazyFactory.class);
      registerExtension(area, ASTLazyFactory.EP.getExtensionPointName(), new DefaultASTLazyFactory());

      registerExtensionPoint(area, ASTLeafFactory.EP.getExtensionPointName(), ASTLeafFactory.class);
      registerExtension(area, ASTLeafFactory.EP.getExtensionPointName(), new DefaultASTLeafFactory());

      registerExtensionPoint(area, ASTCompositeFactory.EP.getExtensionPointName(), ASTCompositeFactory.class);
      registerExtension(area, ASTCompositeFactory.EP.getExtensionPointName(), new DefaultASTCompositeFactory());


    }

    @Override
    public void registerServices(@Nonnull InjectingContainerBuilder builder) {
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
      builder.bind(ProgressManager.class).to(CoreProgressManager.class);
    }
  }

  @Nonnull
  public static LightApplicationBuilder create(@Nonnull Disposable rootDisposable) {
    return create(rootDisposable, new DefaultRegistrator());
  }

  @Nonnull
  public static LightApplicationBuilder create(@Nonnull Disposable rootDisposable, @Nonnull DefaultRegistrator registrator) {
    return new LightApplicationBuilder(rootDisposable, registrator);
  }

  private final Disposable myRootDisposable;
  private final LightExtensionRegistrator myRegistrator;

  private LightApplicationBuilder(Disposable rootDisposable, LightExtensionRegistrator registrator) {
    myRootDisposable = rootDisposable;
    myRegistrator = registrator;
  }

  @Nonnull
  public Application build() {
    return new LightApplication(myRootDisposable, myRegistrator);
  }
}
