// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.wrapper.impl;

import consulo.execution.debug.stream.wrapper.*;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vitaliy.Bibaev
 */
public class StreamChainImpl implements StreamChain {
    private final QualifierExpression myQualifierExpression;
    private final List<? extends IntermediateStreamCall> myIntermediateCalls;
    private final TerminatorStreamCall myTerminator;
    private final PsiElement myContext;

    public StreamChainImpl(@Nonnull QualifierExpression qualifierExpression,
                           @Nonnull List<? extends IntermediateStreamCall> intermediateCalls,
                           @Nonnull TerminatorStreamCall terminator,
                           @Nonnull PsiElement context) {
        myQualifierExpression = qualifierExpression;
        myIntermediateCalls = intermediateCalls;
        myTerminator = terminator;
        myContext = context;
    }

    @Override
    public @Nonnull QualifierExpression getQualifierExpression() {
        return myQualifierExpression;
    }

    @Override
    public @Nonnull List<IntermediateStreamCall> getIntermediateCalls() {
        return Collections.unmodifiableList(myIntermediateCalls);
    }

    @Override
    public @Nonnull StreamCall getCall(int index) {
        if (0 <= index && index < length()) {
            return doGetCall(index);
        }

        throw new IndexOutOfBoundsException("Call index out of bound: " + index);
    }

    @Override
    public @Nonnull TerminatorStreamCall getTerminationCall() {
        return myTerminator;
    }

    @Override
    public @Nonnull String getText() {
        List<StreamCall> list = new ArrayList<>(myIntermediateCalls);
        list.add(myTerminator);

        StringBuilder builder = new StringBuilder();
        builder.append(myQualifierExpression.getText()).append("\n").append(".");

        Iterator<StreamCall> iterator = list.iterator();
        while (iterator.hasNext()) {
            MethodCall call = iterator.next();
            String args = args2Text(call.getArguments());
            builder.append(call.getName()).append(call.getGenericArguments()).append(args);
            if (iterator.hasNext()) {
                builder.append("\n").append(".");
            }
        }

        return builder.toString();
    }


    @Override
    public @Nonnull String getCompactText() {
        StringBuilder builder = new StringBuilder();
        builder.append(myQualifierExpression.getText().replaceAll("\\s+", ""));

        List<StreamCall> list = new ArrayList<>(myIntermediateCalls);
        list.add(myTerminator);

        for (StreamCall call : list) {
            builder.append(" -> ").append(call.getName()).append(call.getGenericArguments());
        }

        return builder.toString();
    }

    @Override
    public int length() {
        return 1 + myIntermediateCalls.size();
    }

    @Override
    public @Nonnull PsiElement getContext() {
        return myContext;
    }

    private StreamCall doGetCall(int index) {
        if (index < myIntermediateCalls.size()) {
            return myIntermediateCalls.get(index);
        }

        return myTerminator;
    }

    private static @Nonnull String args2Text(@Nonnull List<CallArgument> args) {
        return "(" + args.stream().map(CallArgument::getText).collect(Collectors.joining(", ")) + ")";
    }
}
