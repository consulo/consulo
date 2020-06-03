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
package com.intellij.testFramework;

import com.intellij.history.LocalHistory;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.idea.ApplicationStarter;
import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.command.undo.UndoManager;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.impl.ModuleManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.project.impl.TooManyProjectLeakedException;
import com.intellij.openapi.startup.StartupManager;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.DocumentCommitThread;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageManagerImpl;
import com.intellij.util.indexing.IndexableSetContributor;
import com.intellij.util.ui.UIUtil;
import consulo.ui.UIAccess;
import junit.framework.TestCase;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yole
 */
public abstract class PlatformTestCase extends UsefulTestCase implements DataProvider {
  public static final Logger LOGGER = Logger.getInstance(PlatformTestCase.class);

  public static final String TEST_DIR_PREFIX = "idea_test_";

  protected static ApplicationStarter ourApplication;
  protected ProjectManagerEx myProjectManager;
  protected Project myProject;
  protected Module myModule;
  protected static final Collection<File> myFilesToDelete = new HashSet<File>();
  protected boolean myAssertionsInTestDetected;
  public static Thread ourTestThread;
  private static TestCase ourTestCase = null;
  public static final long DEFAULT_TEST_TIME = 300L;
  public static long ourTestTime = DEFAULT_TEST_TIME;
  private EditorListenerTracker myEditorListenerTracker;
  private ThreadTracker myThreadTracker;

  private static Set<VirtualFile> ourEternallyLivingFilesCache;

  protected static long getTimeRequired() {
    return DEFAULT_TEST_TIME;
  }

  protected void initApplication() throws Exception {
    boolean firstTime = ourApplication == null;

    ourApplication = ApplicationStarter.getInstance();
    // ourApplication.setDataProvider(this);

    if (firstTime) {
      cleanPersistedVFSContent();
    }
  }

  private static void cleanPersistedVFSContent() {
    ((PersistentFSImpl)PersistentFS.getInstance()).cleanPersistedContents();
  }

  @Override
  protected CodeStyleSettings getCurrentCodeStyleSettings() {
    if (CodeStyleSchemes.getInstance().getCurrentScheme() == null) return new CodeStyleSettings();
    return CodeStyleSettingsManager.getSettings(getProject());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    if (ourTestCase != null) {
      String message = "Previous test " + ourTestCase + " hasn't called tearDown(). Probably overridden without super call.";
      ourTestCase = null;
      fail(message);
    }

    LOGGER.info(getClass().getName() + ".setUp()");

    initApplication();

    myEditorListenerTracker = new EditorListenerTracker();
    myThreadTracker = new ThreadTracker();

    setUpProject();

    storeSettings();
    ourTestCase = this;
    if (myProject != null) {
      ProjectManagerEx.getInstanceEx().openTestProject(myProject);
      CodeStyleSettingsManager.getInstance(myProject).setTemporarySettings(new CodeStyleSettings());
      InjectedLanguageManagerImpl.pushInjectors(getProject());
    }

    DocumentCommitThread.getInstance().clearQueue();
    UIUtil.dispatchAllInvocationEvents();
  }

  public Project getProject() {
    return myProject;
  }

  public final PsiManager getPsiManager() {
    return PsiManager.getInstance(myProject);
  }

  public Module getModule() {
    return myModule;
  }

  protected void setUpProject() throws Exception {
    myProjectManager = ProjectManagerEx.getInstanceEx();
    assertNotNull("Cannot instantiate ProjectManager component", myProjectManager);

    File tempProjectDir = getTempProjectDir();

    myProject = doCreateProject(tempProjectDir);
    myProjectManager.openTestProject(myProject);
    LocalFileSystem.getInstance().refreshIoFiles(myFilesToDelete);

    setUpModule();

    LightPlatformTestCase.clearUncommittedDocuments(getProject());

    runStartupActivities();
  }

  protected Project doCreateProject(File projectDir) throws Exception {
    return createProject(projectDir, getClass().getName() + "." + getName());
  }

  @Nonnull
  public static Project createProject(File projectDir, String creationPlace) {
    try {
      Project project = ProjectManagerEx.getInstanceEx().newProject(FileUtil.getNameWithoutExtension(projectDir), projectDir.getPath(), false, false);
      assert project != null;

      project.putUserData(CREATION_PLACE, creationPlace);
      return project;
    }
    catch (TooManyProjectLeakedException e) {
      StringBuilder leakers = new StringBuilder();
      leakers.append("Too many projects leaked: \n");
      for (Project project : e.getLeakedProjects()) {
        String presentableString = getCreationPlace(project);
        leakers.append(presentableString);
        leakers.append("\n");
      }

      fail(leakers.toString());
      return null;
    }
  }

  @Nonnull
  public static String getCreationPlace(@Nonnull Project project) {
    String place = project.getUserData(CREATION_PLACE);
    Object base;
    try {
      base = project.isDisposed() ? "" : project.getBaseDir();
    }
    catch (Exception e) {
      base = " (" + e + " while getting base dir)";
    }
    return project.toString() + (place != null ? place : "") + base;
  }

  protected void runStartupActivities() {
    final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(myProject);
    startupManager.runStartupActivities(UIAccess.get());
    startupManager.runPostStartupActivities(UIAccess.get());
  }

  protected File getTempProjectDir() throws IOException {
    return createTempDirectory();
  }

  protected void setUpModule() {
    new WriteCommandAction.Simple(getProject()) {
      @Override
      protected void run() throws Throwable {
        myModule = createMainModule();
      }
    }.execute().throwException();
  }

  protected Module createMainModule() throws IOException {
    return createModule(myProject.getName());
  }

  protected Module createModule(@NonNls final String moduleName) {
    return doCreateRealModule(moduleName);
  }

  protected Module doCreateRealModule(final String moduleName) {
    return doCreateRealModuleIn(moduleName, myProject);
  }

  protected static Module doCreateRealModuleIn(final String moduleName, final Project project) {
    final VirtualFile baseDir = project.getBaseDir();
    assertNotNull(baseDir);
    final File moduleFile = new File(baseDir.getPath().replace('/', File.separatorChar), moduleName);
    FileUtil.createIfDoesntExist(moduleFile);
    myFilesToDelete.add(moduleFile);
    return WriteAction.compute(() -> {
      final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleFile);
      Module module = ModuleManager.getInstance(project).newModule(moduleName, virtualFile.getPath());
      module.getModuleDir();
      return module;
    });
  }

  public static void cleanupApplicationCaches(Project project) {
    if (project != null && !project.isDisposed()) {
      UndoManagerImpl globalInstance = (UndoManagerImpl)UndoManager.getGlobalInstance();
      if (globalInstance != null) {
        globalInstance.dropHistoryInTests();
      }
      ((UndoManagerImpl)UndoManager.getInstance(project)).dropHistoryInTests();

      ((PsiManagerEx)PsiManager.getInstance(project)).getFileManager().cleanupForNextTest();
    }

    LocalFileSystemImpl localFileSystem = (LocalFileSystemImpl)LocalFileSystem.getInstance();
    if (localFileSystem != null) {
      localFileSystem.cleanupForNextTest();
    }

    LocalHistory.getInstance().cleanupForNextTest();
  }

  private static Set<VirtualFile> eternallyLivingFiles() {
    if (ourEternallyLivingFilesCache != null) {
      return ourEternallyLivingFilesCache;
    }

    Set<VirtualFile> survivors = new HashSet<VirtualFile>();

    for (IndexableSetContributor provider : IndexableSetContributor.EP_NAME.getExtensions()) {
      for (VirtualFile file : provider.getAdditionalRootsToIndex()) {
        registerSurvivor(survivors, file);
      }
    }

    ourEternallyLivingFilesCache = survivors;
    return survivors;
  }

  public static void addSurvivingFiles(@Nonnull Collection<VirtualFile> files) {
    for (VirtualFile each : files) {
      registerSurvivor(eternallyLivingFiles(), each);
    }
  }

  private static void registerSurvivor(Set<VirtualFile> survivors, VirtualFile file) {
    addSubTree(file, survivors);
    while (file != null && survivors.add(file)) {
      file = file.getParent();
    }
  }

  private static void addSubTree(VirtualFile root, Set<VirtualFile> to) {
    if (root instanceof VirtualDirectoryImpl) {
      for (VirtualFile child : ((VirtualDirectoryImpl)root).getCachedChildren()) {
        if (child instanceof VirtualDirectoryImpl) {
          to.add(child);
          addSubTree(child, to);
        }
      }
    }
  }

  @Override
  protected void tearDown() throws Exception {
    CompositeException result = new CompositeException();
    if (myProject != null) {
      try {
        LightPlatformTestCase.doTearDown(getProject(), ourApplication, false);
      }
      catch (Throwable e) {
        result.add(e);
      }
    }

    try {
      checkForSettingsDamage();
    }
    catch (Throwable e) {
      result.add(e);
    }
    try {
      Project project = getProject();
      disposeProject(result);

      if (project != null) {
        try {
          InjectedLanguageManagerImpl.checkInjectorsAreDisposed(project);
        }
        catch (AssertionError e) {
          result.add(e);
        }
      }
      try {
        for (final File fileToDelete : myFilesToDelete) {
          delete(fileToDelete);
        }
        LocalFileSystem.getInstance().refreshIoFiles(myFilesToDelete);
      }
      catch (Throwable e) {
        result.add(e);
      }

      try {
        super.tearDown();
      }
      catch (Throwable e) {
        result.add(e);
      }

      //cleanTheWorld();
      try {
        myEditorListenerTracker.checkListenersLeak();
      }
      catch (AssertionError error) {
        result.add(error);
      }
      try {
        myThreadTracker.checkLeak();
      }
      catch (AssertionError error) {
        result.add(error);
      }
      try {
        LightPlatformTestCase.checkEditorsReleased();
      }
      catch (Throwable error) {
        result.add(error);
      }
      //if (directoryIndex != null) {
      //  directoryIndex.assertAncestorConsistent();
      //}
    }
    finally {
      myProjectManager = null;
      myProject = null;
      myModule = null;
      myFilesToDelete.clear();
      myEditorListenerTracker = null;
      myThreadTracker = null;
      ourTestCase = null;
    }
    if (!result.isEmpty()) throw result;
  }

  private void disposeProject(@Nonnull CompositeException result) /* throws nothing */ {
    try {
      DocumentCommitThread.getInstance().clearQueue();
      UIUtil.dispatchAllInvocationEvents();
    }
    catch (Exception e) {
      result.add(e);
    }
    try {
      if (myProject != null) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            Disposer.dispose(myProject);
            ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
            if (projectManager instanceof ProjectManagerImpl) {
              Collection<Project> projectsStillOpen = projectManager.closeTestProject(myProject);
              if (!projectsStillOpen.isEmpty()) {
                Project project = projectsStillOpen.iterator().next();
                projectsStillOpen.clear();
                throw new AssertionError("Test project is not disposed: " + project + ";\n created in: " + getCreationPlace(project));
              }
            }
          }
        });
      }
    }
    catch (Exception e) {
      result.add(e);
    }
    finally {
      if (myProject != null) {
        try {
          PsiDocumentManager documentManager = myProject.getComponent(PsiDocumentManager.class);
          if (documentManager != null) {
            EditorFactory.getInstance().getEventMulticaster().removeDocumentListener((DocumentListener)documentManager);
          }
        }
        catch (Exception ignored) {

        }
        myProject = null;
      }
    }
  }

  protected void resetAllFields() {
    resetClassFields(getClass());
  }

  @Override
  protected final <T extends Disposable> T disposeOnTearDown(T disposable) {
    Disposer.register(myProject, disposable);
    return disposable;
  }

  private void resetClassFields(final Class<?> aClass) {
    try {
      clearDeclaredFields(this, aClass);
    }
    catch (IllegalAccessException e) {
      LOGGER.error(e);
    }

    if (aClass == PlatformTestCase.class) return;
    resetClassFields(aClass.getSuperclass());
  }

  private String getFullName() {
    return getClass().getName() + "." + getName();
  }

  private void delete(File file) {
    boolean b = FileUtil.delete(file);
    if (!b && file.exists() && !myAssertionsInTestDetected) {
      fail("Can't delete " + file.getAbsolutePath() + " in " + getFullName());
    }
  }

  protected void simulateProjectOpen() {
    ModuleManagerImpl mm = (ModuleManagerImpl)ModuleManager.getInstance(myProject);
    StartupManagerImpl sm = (StartupManagerImpl)StartupManager.getInstance(myProject);

    mm.projectOpened();
    sm.runStartupActivities(UIAccess.get());
    // extra init for libraries
    sm.runPostStartupActivities(UIAccess.get());
  }

  @Override
  public void runBare() throws Throwable {
    if (!shouldRunTest()) return;

    try {
      runBareImpl();
    }
    finally {
      try {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
          @Override
          public void run() {
            cleanupApplicationCaches(getProject());
            resetAllFields();
          }
        }, ModalityState.NON_MODAL);
      }
      catch (Throwable e) {
        // Ignore
      }
    }
  }

  private void runBareImpl() throws Throwable {
    final Throwable[] throwables = new Throwable[1];
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        ourTestThread = Thread.currentThread();
        ourTestTime = getTimeRequired();
        try {
          try {
            setUp();
          }
          catch (Throwable e) {
            CompositeException result = new CompositeException(e);
            disposeProject(result);
            throw result;
          }
          try {
            myAssertionsInTestDetected = true;
            runTest();
            myAssertionsInTestDetected = false;
          }
          catch (Throwable e) {
            throwables[0] = e;
            throw e;
          }
          finally {
            tearDown();
          }
        }
        catch (Throwable throwable) {
          if (throwables[0] == null) {  // report tearDown() problems if only no exceptions thrown from runTest()
            throwables[0] = throwable;
          }
        }
        finally {
          ourTestThread = null;
        }
      }
    };

    runBareRunnable(runnable);

    if (throwables[0] != null) {
      throw throwables[0];
    }

    // just to make sure all deferred Runnable's to finish
    waitForAllLaters();

    /*
    if (++LEAK_WALKS % 1 == 0) {
      LeakHunter.checkLeak(ApplicationManager.getApplication(), ProjectImpl.class, new Processor<ProjectImpl>() {
        @Override
        public boolean process(ProjectImpl project) {
          return !project.isDefault() && !LightPlatformTestCase.isLight(project);
        }
      });
    }
    */
  }

  private static int LEAK_WALKS;

  private static void waitForAllLaters() throws InterruptedException, InvocationTargetException {
    for (int i = 0; i < 3; i++) {
      SwingUtilities.invokeAndWait(EmptyRunnable.getInstance());
    }
  }

  protected void runBareRunnable(Runnable runnable) throws Throwable {
    SwingUtilities.invokeAndWait(runnable);
  }

  protected boolean isRunInWriteAction() {
    return true;
  }

  @Override
  protected void invokeTestRunnable(final Runnable runnable) throws Exception {
    final Exception[] e = new Exception[1];
    Runnable runnable1 = new Runnable() {
      @Override
      public void run() {
        try {
          if (ApplicationManager.getApplication().isDispatchThread() && isRunInWriteAction()) {
            ApplicationManager.getApplication().runWriteAction(runnable);
          }
          else {
            runnable.run();
          }
        }
        catch (Exception e1) {
          e[0] = e1;
        }
      }
    };

    if (annotatedWith(WrapInCommand.class)) {
      CommandProcessor.getInstance().executeCommand(myProject, runnable1, "", null);
    }
    else {
      runnable1.run();
    }

    if (e[0] != null) {
      throw e[0];
    }
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    return myProject == null ? null : new TestDataProvider(myProject).getData(dataId);
  }

  public static File createTempDir(@NonNls final String prefix) throws IOException {
    return createTempDir(prefix, true);
  }

  public static File createTempDir(@NonNls final String prefix, final boolean refresh) throws IOException {
    final File tempDirectory = FileUtilRt.createTempDirectory(TEST_DIR_PREFIX + prefix, null, false);
    myFilesToDelete.add(tempDirectory);
    if (refresh) {
      getVirtualFile(tempDirectory);
    }
    return tempDirectory;
  }

  @Nullable
  protected static VirtualFile getVirtualFile(final File file) {
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }

  protected File createTempDirectory() throws IOException {
    return createTempDir(getTestName(true));
  }

  protected File createTempDirectory(final boolean refresh) throws IOException {
    return createTempDir(getTestName(true), refresh);
  }

  protected File createTempFile(String name, @Nullable String text) throws IOException {
    File directory = createTempDirectory();
    File file = new File(directory, name);
    if (!file.createNewFile()) {
      throw new IOException("Can't create " + file);
    }
    if (text != null) {
      FileUtil.writeToFile(file, text);
    }
    return file;
  }

  public static void setContentOnDisk(File file, byte[] bom, String content, Charset charset) throws IOException {
    FileOutputStream stream = new FileOutputStream(file);
    if (bom != null) {
      stream.write(bom);
    }
    OutputStreamWriter writer = new OutputStreamWriter(stream, charset);
    try {
      writer.write(content);
    }
    finally {
      writer.close();
    }
  }

  public static VirtualFile createTempFile(@NonNls String ext, @javax.annotation.Nullable byte[] bom, @NonNls String content, Charset charset) throws IOException {
    File temp = FileUtil.createTempFile("copy", "." + ext);
    setContentOnDisk(temp, bom, content, charset);

    myFilesToDelete.add(temp);
    final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(temp);
    assert file != null : temp;
    return file;
  }

  @javax.annotation.Nullable
  protected PsiFile getPsiFile(final Document document) {
    return PsiDocumentManager.getInstance(getProject()).getPsiFile(document);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  public @interface WrapInCommand {
  }

  protected static VirtualFile createChildData(@Nonnull final VirtualFile dir, @Nonnull @NonNls final String name) {
    try {
      return WriteAction.compute(() -> dir.createChildData(null, name));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static VirtualFile createChildDirectory(@Nonnull final VirtualFile dir, @Nonnull @NonNls final String name) {
    try {
      return WriteAction.compute(() -> dir.createChildDirectory(null, name));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void delete(@Nonnull final VirtualFile file) {
    WriteAction.run(() -> {
      try {
        file.delete(null);
      }
      catch (IOException e) {
        fail();
      }
    });
  }

  protected static void rename(@Nonnull final VirtualFile vFile1, @Nonnull final String newName) {
    new WriteCommandAction.Simple(null) {
      @Override
      protected void run() throws Throwable {
        vFile1.rename(this, newName);
      }
    }.execute().throwException();
  }
}
