package com.intellij.tasks.context;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-01-03
 */
public class TestBreakpointType extends XLineBreakpointType<TestBreakpointProperties> {
  public TestBreakpointType() {
    super("test", "Test Breakpoint");
  }

  @Nullable
  @Override
  public TestBreakpointProperties createBreakpointProperties(@Nonnull VirtualFile virtualFile, int i) {
    return new TestBreakpointProperties();
  }
}
