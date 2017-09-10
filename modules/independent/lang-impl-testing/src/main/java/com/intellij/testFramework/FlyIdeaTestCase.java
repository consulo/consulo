package com.intellij.testFramework;

import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public abstract class FlyIdeaTestCase extends TestCase {
  private Disposable myRootDisposable;
  private File myTempDir;

  @Override
  protected void setUp() throws Exception {
    myRootDisposable = Disposer.newDisposable();
    new CoreApplicationEnvironment(myRootDisposable);
  }

  public File getTempDir() throws IOException {
    if (myTempDir == null) {
      myTempDir = FileUtil.createTempDirectory(getName(), getClass().getName(), false);
    }

    return myTempDir;
  }

  public Disposable getRootDisposable() {
    return myRootDisposable;
  }

  @Override
  protected void tearDown() throws Exception {
    if (myTempDir != null) {
      FileUtil.asyncDelete(myTempDir);
    }
    Disposer.dispose(myRootDisposable);
  }
}
