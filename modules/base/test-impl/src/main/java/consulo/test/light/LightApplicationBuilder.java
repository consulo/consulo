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

import com.intellij.concurrency.JobLauncher;
import com.intellij.ide.UiActivityMonitor;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.treeView.TreeAnchorizer;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.impl.PsiBuilderFactoryImpl;
import consulo.disposer.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.psi.LanguageFileViewProviders;
import com.intellij.psi.LanguageSubstitutors;
import com.intellij.ui.ExpandableItemsHandlerFactory;
import com.intellij.ui.TreeUIHelper;
import com.intellij.util.KeyedLazyInstanceEP;
import consulo.application.options.PathMacrosService;
import consulo.extensions.ExtensionExtender;
import consulo.injecting.InjectingContainerBuilder;
import consulo.lang.LanguageVersionDefines;
import consulo.lang.LanguageVersionResolvers;
import consulo.psi.tree.ASTCompositeFactory;
import consulo.psi.tree.ASTLazyFactory;
import consulo.psi.tree.ASTLeafFactory;
import consulo.psi.tree.impl.DefaultASTCompositeFactory;
import consulo.psi.tree.impl.DefaultASTLazyFactory;
import consulo.psi.tree.impl.DefaultASTLeafFactory;
import consulo.test.light.impl.*;

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

      registerExtensionPoint(area, LanguageParserDefinitions.INSTANCE.getExtensionPointName(), LanguageExtensionPoint.class);
      registerExtensionPoint(area, LanguageSubstitutors.INSTANCE.getExtensionPointName(), LanguageExtensionPoint.class);
      registerExtensionPoint(area, LanguageVersionResolvers.INSTANCE.getExtensionPointName(), LanguageExtensionPoint.class);
      registerExtensionPoint(area, LanguageVersionDefines.INSTANCE.getExtensionPointName(), LanguageExtensionPoint.class);
      registerExtensionPoint(area, LanguageFileViewProviders.INSTANCE.getExtensionPointName(), LanguageExtensionPoint.class);

      registerExtensionPoint(area, PathMacroFilter.EP_NAME, PathMacroFilter.class);
      registerExtensionPoint(area, ExtensionExtender.EP_NAME, KeyedLazyInstanceEP.class);
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
  public static LightApplicationBuilder create(@Nonnull Disposable rootDisposable, @Nonnull DefaultRegistrator registrator ) {
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
