/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.xdebugger;

import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public abstract class XDebuggerTestCase extends PlatformTestCase {
  public static final MyLineBreakpointType MY_LINE_BREAKPOINT_TYPE = new MyLineBreakpointType();
  protected static final MySimpleBreakpointType MY_SIMPLE_BREAKPOINT_TYPE = new MySimpleBreakpointType();

  protected XDebuggerTestCase() {
  }

  @Override
  protected void initApplication() throws Exception {
    super.initApplication();
    final ExtensionPoint<XBreakpointType> point = getBreakpointTypes();
    point.registerExtension(MY_LINE_BREAKPOINT_TYPE);
    point.registerExtension(MY_SIMPLE_BREAKPOINT_TYPE);
  }

  private static ExtensionPoint<XBreakpointType> getBreakpointTypes() {
    return Extensions.getRootArea().getExtensionPoint(XBreakpointType.EXTENSION_POINT_NAME);
  }

  @Override
  protected void tearDown() throws Exception {
    getBreakpointTypes().unregisterExtension(MY_LINE_BREAKPOINT_TYPE);
    getBreakpointTypes().unregisterExtension(MY_SIMPLE_BREAKPOINT_TYPE);
    super.tearDown();
  }

  public static class MyLineBreakpointType extends XLineBreakpointType<MyBreakpointProperties> {
    public MyLineBreakpointType() {
      super("testLine", "239");
    }

    @Override
    public MyBreakpointProperties createBreakpointProperties(@NotNull final VirtualFile file, final int line) {
      return null;
    }

    @Override
    public MyBreakpointProperties createProperties() {
      return new MyBreakpointProperties();
    }
  }

  public static class MySimpleBreakpointType extends XBreakpointType<XBreakpoint<MyBreakpointProperties>,MyBreakpointProperties> {
    public MySimpleBreakpointType() {
      super("test", "239");
    }

    @Override
    public String getDisplayText(final XBreakpoint<MyBreakpointProperties> breakpoint) {
      return "";
    }

    @Override
    public MyBreakpointProperties createProperties() {
      return new MyBreakpointProperties();
    }

    @Override
    public XBreakpoint<MyBreakpointProperties> createDefaultBreakpoint(@NotNull XBreakpointCreator<MyBreakpointProperties> creator) {
      final XBreakpoint<MyBreakpointProperties> breakpoint = creator.createBreakpoint(new MyBreakpointProperties("default"));
      breakpoint.setEnabled(true);
      return breakpoint;
    }
  }

  protected static class MyBreakpointProperties extends XBreakpointProperties<MyBreakpointProperties> {
    @Attribute("option")
    public String myOption;

    public MyBreakpointProperties() {
    }

    public MyBreakpointProperties(final String option) {
      myOption = option;
    }

    @Override
    public MyBreakpointProperties getState() {
      return this;
    }

    @Override
    public void loadState(final MyBreakpointProperties state) {
      myOption = state.myOption;
    }
  }
}
