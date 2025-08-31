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
package consulo.execution.debug.impl.internal.breakpoint;

import consulo.execution.debug.breakpoint.SuspendPolicy;
import consulo.execution.debug.breakpoint.XBreakpointProperties;
import consulo.execution.debug.breakpoint.XLineBreakpoint;
import consulo.execution.debug.breakpoint.XLineBreakpointType;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@Tag("line-breakpoint")
public class LineBreakpointState<P extends XBreakpointProperties> extends BreakpointState<XLineBreakpoint<P>, P, XLineBreakpointType<P>> {
  private String myFileUrl;
  private int myLine;
  private boolean myTemporary;

  public LineBreakpointState() {
  }

  public LineBreakpointState(boolean enabled, String typeId, String fileUrl, int line, boolean temporary,
                             long timeStamp, SuspendPolicy suspendPolicy) {
    super(enabled, typeId, timeStamp, suspendPolicy);
    myFileUrl = fileUrl;
    myLine = line;
    myTemporary = temporary;
  }

  @Tag("url")
  public String getFileUrl() {
    return myFileUrl;
  }

  public void setFileUrl(String fileUrl) {
    myFileUrl = fileUrl;
  }

  @Tag("line")
  public int getLine() {
    return myLine;
  }

  public void setLine(int line) {
    myLine = line;
  }

  public boolean isTemporary() {
    return myTemporary;
  }

  public void setTemporary(boolean temporary) {
    myTemporary = temporary;
  }

  @Override
  public XBreakpointBase<XLineBreakpoint<P>,P, ?> createBreakpoint(@Nonnull XLineBreakpointType<P> type, @Nonnull XBreakpointManagerImpl breakpointManager) {
    return new XLineBreakpointImpl<>(type, breakpointManager, this);
  }
}
