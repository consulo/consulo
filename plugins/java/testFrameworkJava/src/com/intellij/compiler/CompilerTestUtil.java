package com.intellij.compiler;

import com.intellij.compiler.impl.TranslatingCompilerFilesMonitor;
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration;
import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.impl.FileTypeManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectSdkTable;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectSdkTableImpl;
import com.intellij.openapi.projectRoots.impl.ProjectSdkTableImpl;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SystemProperties;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerOptions;
import org.jetbrains.jps.model.serialization.JDomSerializationUtil;
import org.jetbrains.jps.model.serialization.JpsGlobalLoader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class CompilerTestUtil {
  private CompilerTestUtil() {
  }

  public static void setupJavacForTests(Project project) {
    CompilerConfigurationImpl compilerConfiguration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(project);
    compilerConfiguration.projectOpened();
    compilerConfiguration.setDefaultCompiler(compilerConfiguration.getJavacCompiler());

    JpsJavaCompilerOptions javacSettings = JavacConfiguration.getOptions(project, JavacConfiguration.class);
    javacSettings.setTestsUseExternalCompiler(true);
  }

  public static void scanSourceRootsToRecompile(Project project) {
    // need this to emulate project opening
    final List<VirtualFile> roots = Arrays.asList(ProjectRootManager.getInstance(project).getContentSourceRoots());
    TranslatingCompilerFilesMonitor.getInstance().scanSourceContent(new TranslatingCompilerFilesMonitor.ProjectRef(project), roots, roots.size(), true);
  }

  public static void saveApplicationSettings() {
    try {
      ProjectSdkTableImpl table = (ProjectSdkTableImpl)ProjectSdkTable.getInstance();
      Element root = new Element("application");
      root.addContent(JDomSerializationUtil.createComponentElement(JpsGlobalLoader.SDK_TABLE_COMPONENT_NAME).addContent(table.getState().cloneContent()));
      saveApplicationComponent(root, ((ProjectSdkTableImpl)ProjectSdkTable.getInstance()).getExportFiles()[0]);

      FileTypeManagerImpl fileTypeManager = (FileTypeManagerImpl)FileTypeManager.getInstance();
      Element fileTypesComponent = JDomSerializationUtil.createComponentElement(fileTypeManager.getComponentName());
      fileTypeManager.writeExternal(fileTypesComponent);
      saveApplicationComponent(new Element("application").addContent(fileTypesComponent), PathManager.getOptionsFile(fileTypeManager));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    catch (WriteExternalException e) {
      throw new RuntimeException(e);
    }
  }

  private static void saveApplicationComponent(Element root, final File file) throws IOException {
    FileUtil.createParentDirs(file);
    JDOMUtil.writeDocument(new Document(root), file, SystemProperties.getLineSeparator());
  }

  public static void enableExternalCompiler(final Project project) {
    new WriteAction() {
      protected void run(final Result result) {
        CompilerWorkspaceConfiguration.getInstance(project).USE_COMPILE_SERVER = true;
        ApplicationManagerEx.getApplicationEx().doNotSave(false);
        JavaAwareProjectSdkTableImpl table = JavaAwareProjectSdkTableImpl.getInstanceEx();
        table.addSdk(table.getInternalJdk());
      }
    }.execute();
  }

  public static void disableExternalCompiler(final Project project) {
    new WriteAction() {
      protected void run(final Result result) {
        CompilerWorkspaceConfiguration.getInstance(project).USE_COMPILE_SERVER = false;
        ApplicationManagerEx.getApplicationEx().doNotSave(true);
        JavaAwareProjectSdkTableImpl table = JavaAwareProjectSdkTableImpl.getInstanceEx();
        table.removeSdk(table.getInternalJdk());
        BuildManager.getInstance().stopWatchingProject(project);
      }
    }.execute();
  }
}
