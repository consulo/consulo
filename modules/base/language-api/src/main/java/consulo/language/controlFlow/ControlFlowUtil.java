// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.controlFlow;

import consulo.application.progress.ProgressManager;
import consulo.component.util.graph.Graph;
import consulo.language.psi.PsiElement;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ControlFlowUtil {
    private ControlFlowUtil() {
    }

    public static @Nonnull Graph<Instruction> createGraph(@Nonnull Instruction[] flow) {
        return new Graph<Instruction>() {
            private final @Nonnull List<Instruction> myList = Arrays.asList(flow);

            @Override
            public @Nonnull Collection<Instruction> getNodes() {
                return myList;
            }

            @Override
            public @Nonnull Iterator<Instruction> getIn(Instruction n) {
                return n.allPred().iterator();
            }

            @Override
            public @Nonnull Iterator<Instruction> getOut(Instruction n) {
                return n.allSucc().iterator();
            }
        };
    }

    public static int findInstructionNumberByElement(final Instruction[] flow, final PsiElement element) {
        for (int i = 0; i < flow.length; i++) {
            // Check if canceled
            ProgressManager.checkCanceled();

            if (element == flow[i].getElement()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Process control flow graph in depth first order
     */
    public static boolean process(final Instruction[] flow, final int start, final Predicate<? super Instruction> processor) {
        final int length = flow.length;
        boolean[] visited = new boolean[length];
        Arrays.fill(visited, false);

        @SuppressWarnings("SSBasedInspection")
        IntArrayList stack = new IntArrayList(length);
        stack.push(start);

        while (!stack.isEmpty()) {
            ProgressManager.checkCanceled();
            final int num = stack.popInt();
            final Instruction instruction = flow[num];
            if (!processor.test(instruction)) {
                return false;
            }
            for (Instruction succ : instruction.allSucc()) {
                final int succNum = succ.num();
                if (!visited[succNum]) {
                    visited[succNum] = true;
                    stack.push(succNum);
                }
            }
        }
        return true;
    }

    public static void iteratePrev(final int startInstruction,
                                   @Nonnull final Instruction[] instructions,
                                   final @Nonnull Function<? super Instruction, Operation> closure) {
        iterate(startInstruction, instructions, closure, true);
    }

    /**
     * Iterates over write instructions in CFG with reversed order
     */
    public static void iterate(final int startInstruction,
                               @Nonnull final Instruction[] instructions,
                               final @Nonnull Function<? super Instruction, Operation> closure,
                               boolean prev) {
        //noinspection SSBasedInspection
        final IntArrayList stack = new IntArrayList(instructions.length);
        final boolean[] visited = new boolean[instructions.length];

        stack.push(startInstruction);
        while (!stack.isEmpty()) {
            ProgressManager.checkCanceled();
            final int num = stack.popInt();
            final Instruction instr = instructions[num];
            final Operation nextOperation = closure.apply(instr);
            // Just ignore previous instructions for the current node and move further
            if (nextOperation == Operation.CONTINUE) {
                continue;
            }
            // STOP iteration
            if (nextOperation == Operation.BREAK) {
                break;
            }
            // If we are here, we should process previous nodes in natural way
            assert nextOperation == Operation.NEXT;
            Collection<Instruction> nextToProcess = prev ? instr.allPred() : instr.allSucc();
            for (Instruction pred : nextToProcess) {
                final int predNum = pred.num();
                if (!visited[predNum]) {
                    visited[predNum] = true;
                    stack.push(predNum);
                }
            }
        }
    }

    public enum Operation {
        /**
         * CONTINUE is used to ignore previous/next elements processing for the node, however it doesn't stop the iteration process
         */
        CONTINUE,
        /**
         * BREAK is used to stop iteration process
         */
        BREAK,
        /**
         * NEXT is used to indicate that iteration should be continued in natural way
         */
        NEXT
    }
}