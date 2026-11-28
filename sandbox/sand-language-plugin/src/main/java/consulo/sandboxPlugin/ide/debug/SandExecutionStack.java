/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.debug;

import consulo.execution.debug.frame.HasThreadInterrupt;
import consulo.execution.debug.frame.HasThreadSuspend;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public class SandExecutionStack extends XExecutionStack implements HasThreadSuspend, HasThreadInterrupt {
    private final List<SandStackFrame> myFrames;
    private final boolean mySuspended;
    private final Consumer<String> myPrinter;

    public SandExecutionStack(String displayName, Image icon, List<SandStackFrame> frames, boolean suspended, Consumer<String> printer) {
        super(displayName, icon);
        myFrames = frames;
        mySuspended = suspended;
        myPrinter = printer;
    }

    @Override
    public boolean isSuspended() {
        return mySuspended;
    }

    @Override
    public void suspendThread() {
        myPrinter.accept("Freeze thread " + getDisplayName() + "\n");
    }

    @Override
    public void resumeThread() {
        myPrinter.accept("Resume thread " + getDisplayName() + "\n");
    }

    @Override
    public void interruptThread() {
        myPrinter.accept("Interrupt thread " + getDisplayName() + "\n");
    }

    @Override
    public @Nullable XStackFrame getTopFrame() {
        return myFrames.isEmpty() ? null : myFrames.get(0);
    }

    @Override
    public void computeStackFrames(XStackFrameContainer container) {
        container.addStackFrames(myFrames, true);
    }
}
