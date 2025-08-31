/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package consulo.language.dataFlow;

import consulo.application.progress.ProgressManager;
import consulo.component.util.graph.DFSTBuilder;
import consulo.language.controlFlow.ControlFlowUtil;
import consulo.language.controlFlow.Instruction;
import consulo.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class DFAEngine<E> {
    private static final Logger LOG = Logger.getInstance(DFAEngine.class);
    private static final long TIME_LIMIT = 1_000_000_000L; // In nanoseconds, 1_000_000_000 = 1 sec

    private final Instruction[] myFlow;

    private final DfaInstance<E> myDfa;
    private final Semilattice<E> mySemilattice;

    public DFAEngine(Instruction[] flow,
                     DfaInstance<E> dfa,
                     Semilattice<E> semilattice) {
        myFlow = flow;
        myDfa = dfa;
        mySemilattice = semilattice;
    }

    public List<E> performDFA() throws DFALimitExceededException {
        ArrayList<E> info = new ArrayList<>(myFlow.length);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Performing DFA\n" + "Instance: " + myDfa + " Semilattice: " + mySemilattice + "\nCon");
        }

// initializing dfa
        E initial = myDfa.initial();
        int length = myFlow.length;
        for (int i = 0; i < length; i++) {
            info.add(i, initial);
        }

// Count limit for loops
        int limit = getIterationLimit();
        long startTime = System.nanoTime();
        DFSTBuilder<Instruction> dfsTBuilder = new DFSTBuilder<>(ControlFlowUtil.createGraph(myFlow));

        int[] instructionNumToNNumber = new int[myFlow.length];
        for (int i = 0; i < myFlow.length; ++i) {
            instructionNumToNNumber[dfsTBuilder.getNodeByNNumber(i).num()] = i;
        }
        int[] lastUpdate = new int[length];
        int count = 0;

        List<Instruction> instructionsWithBackEdges = new ArrayList<>();
        for (Collection<Instruction> component : dfsTBuilder.getComponents()) {
            List<Instruction> sortedInstructions = new ArrayList<>(component);
            // component returns its instructions using getNodeByTNumber
            // unfortunately its ordering is not suitable for dataflow goals because
            // it does not start order in a SCC from entry nodes
            // so We should resort nodes in a SCC by NNumber
            sortedInstructions.sort(Comparator.comparingInt(it -> instructionNumToNNumber[it.num()]));
            instructionsWithBackEdges.clear();
            for (Instruction instruction : sortedInstructions) {
                applyTransferFunction(info, instruction);
                if (instruction.allPred().stream().anyMatch(predecessor ->
                    instructionNumToNNumber[predecessor.num()] > instructionNumToNNumber[instruction.num()])) {
                    instructionsWithBackEdges.add(instruction);
                }
            }

            int iteration = 0;
            while (true) {
                ++iteration;
                int currentIteration = iteration;
                boolean anyUpdates = false;
                for (Instruction instruction : instructionsWithBackEdges) {
                    if (applyTransferFunction(info, instruction)) {
                        lastUpdate[instruction.num()] = currentIteration;
                        anyUpdates = true;
                        count++;
                    }
                }
                if (!anyUpdates) {
                    break;
                }
                for (Instruction instruction : sortedInstructions) {
                    if (instruction.allPred().stream().anyMatch(it -> lastUpdate[it.num()] == currentIteration)
                        && applyTransferFunction(info, instruction)) {
                        lastUpdate[instruction.num()] = currentIteration;
                        count++;
                    }
                }

                if (count > limit || (System.nanoTime() - startTime) > TIME_LIMIT) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Iteration count exceeded on worklist");
                    }
                    throw new DFALimitExceededException("Iteration count exceeded on worklist");
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Done in: " + (System.nanoTime() - startTime) / 10e6 + "ms. Ratio: " + count / length);
        }
        return info;
    }

    private boolean applyTransferFunction(List<E> info, Instruction currentInstruction) {
        ProgressManager.checkCanceled();
        int currentNumber = currentInstruction.num();
        E oldE = info.get(currentNumber);
        E joinedE = join(currentInstruction, info);
        E newE = myDfa.fun(joinedE, currentInstruction);
        if (!mySemilattice.eq(newE, oldE)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Number: " + currentNumber + " old: " + oldE.toString() + " new: " + newE.toString());
            }
            info.set(currentNumber, newE);
            return true;
        }
        return false;
    }


    /**
     * Count limit for dfa number of iterations.
     * Every node in dfa should be processed <= pred times * 2
     * Multiplier 2 is because of cycles.
     */
    private int getIterationLimit() {
        int allPred = myFlow.length;
        for (Instruction instruction : myFlow) {
            allPred += instruction.allPred().size();
        }
        return allPred * 2;
    }

    private E join(Instruction instruction, List<? extends E> info) {
        Iterable<? extends Instruction> prev = instruction.allPred();
        ArrayList<E> prevInfos = new ArrayList<>();
        for (Instruction i : prev) {
            prevInfos.add(info.get(i.num()));
        }
        return mySemilattice.join(prevInfos);
    }
}