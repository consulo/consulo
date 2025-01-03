/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.controlFlow.base;

import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.controlFlow.TransparentInstruction;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class DetachedInstructionImpl extends InstructionBaseImpl {

    private final AtomicInteger myNum = new AtomicInteger(-1);

    public DetachedInstructionImpl(@Nullable PsiElement element) {
        super(element);
    }

    @Override
    public int num() {
        return myNum.get();
    }

    public final void addToInstructions(@Nonnull ControlFlowBuilder builder) {
        assert !(this instanceof TransparentInstruction);
        builder.instructions.add(this);
        updateNum(builder.instructionCount++);
    }

    public final void addTransparentNode(@Nonnull ControlFlowBuilder builder) {
        assert this instanceof TransparentInstruction;
        updateNum(builder.transparentInstructionCount++);
        builder.addNodeAndCheckPending(this);
    }

    public final void addNode(@Nonnull ControlFlowBuilder builder) {
        assert !(this instanceof TransparentInstruction);
        updateNum(builder.instructionCount++);
        builder.addNodeAndCheckPending(this);
    }

    public void updateNum(int newNum) {
        assert myNum.get() == -1;
        myNum.set(newNum);
    }
}
