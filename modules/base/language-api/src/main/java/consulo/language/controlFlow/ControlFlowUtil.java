/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.controlFlow;

import consulo.logging.Logger;
import consulo.application.progress.ProgressManager;
import consulo.language.psi.PsiElement;
import consulo.application.util.function.Processor;
import consulo.util.collection.primitive.ints.IntStack;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.function.Function;

/**
 * @author oleg
 */
public class ControlFlowUtil {
  private static final Logger LOG = Logger.getInstance(ControlFlowUtil.class);

  private ControlFlowUtil() {
  }

  public static int[] postOrder(Instruction[] flow) {
    final int length = flow.length;
    int[] result = new int[length];
    boolean[] visited = new boolean[length];
    Arrays.fill(visited, false);
    final IntStack stack = new IntStack(length);

    int N = 0;
    for (int i = 0; i < length; i++) { //graph might not be connected
      if (!visited[i]) {
        visited[i] = true;
        stack.clear();
        stack.push(i);

        while (!stack.isEmpty()) {
          final int num = stack.pop();
          result[N++] = num;
          for (Instruction succ : flow[num].allSucc()) {
            final int succNum = succ.num();
            if (!visited[succNum]) {
              visited[succNum] = true;
              stack.push(succNum);
            }
          }
        }
      }
    }
    LOG.assertTrue(N == length);
    return result;
  }

  public static int findInstructionNumberByElement(final Instruction[] flow, final PsiElement element){
    for (int i=0;i<flow.length;i++) {
      // Check if canceled
      ProgressManager.checkCanceled();

      if (element == flow[i].getElement()){
        return i;
      }
    }
    return -1;
  }

  /**
   * Process control flow graph in depth first order
   */
  public static boolean process(final Instruction[] flow, final int start, final Processor<Instruction> processor){
    final int length = flow.length;
    boolean[] visited = new boolean[length];
    Arrays.fill(visited, false);

    final IntStack stack = new IntStack(length);
    stack.push(start);

    while (!stack.isEmpty()) {
      ProgressManager.checkCanceled();
      final int num = stack.pop();
      final Instruction instruction = flow[num];
      if (!processor.process(instruction)){
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

  /**
   * Iterates over write instructions in CFG with reversed order
   */
  public static void iteratePrev(final int startInstruction,
                                 @Nonnull final Instruction[] instructions,
                                 @Nonnull final Function<Instruction, Operation> closure) {
    final IntStack stack = new IntStack(instructions.length);
    final boolean[] visited = new boolean[instructions.length];

    stack.push(startInstruction);
    while (!stack.isEmpty()) {
      ProgressManager.checkCanceled();
      final int num = stack.pop();
      final Instruction instr = instructions[num];
      final Operation nextOperation = closure.apply(instr);
      // Just ignore previous instructions for current node and move further
      if (nextOperation == Operation.CONTINUE) {
        continue;
      }
      // STOP iteration
      if (nextOperation == Operation.BREAK) {
        break;
      }
      // If we are here, we should process previous nodes in natural way
      assert nextOperation == Operation.NEXT;
      for (Instruction pred : instr.allPred()) {
        final int predNum = pred.num();
        if (!visited[predNum]) {
          visited[predNum] = true;
          stack.push(predNum);
        }
      }
    }
  }

  public static enum Operation {
    /**
     * CONTINUE is used to ignore previous elements processing for the node, however it doesn't stop the iteration process
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