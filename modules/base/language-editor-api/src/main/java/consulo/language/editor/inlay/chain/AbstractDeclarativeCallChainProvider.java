// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay.chain;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.language.editor.inlay.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.project.Project;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractDeclarativeCallChainProvider<DotQualifiedExpression extends PsiElement, ExpressionType, TypeComputationContext>
    implements DeclarativeInlayHintsProvider {

    @Override
    public DeclarativeInlayHintsCollector createCollector(PsiFile file, Editor editor) {
        if (!isAvailable(file, editor)) {
            return null;
        }
        return new Collector(file);
    }

    protected static class ExpressionWithType<T> {
        public final PsiElement expression;
        public final T type;

        public ExpressionWithType(PsiElement expression, T type) {
            this.expression = expression;
            this.type = type;
        }
    }

    protected class Collector implements DeclarativeInlayHintsCollector.SharedBypassCollector {
        private final PsiFile file;

        public Collector(PsiFile file) {
            this.file = file;
        }

        @RequiredReadAction
        @Override
        public void collectFromElement(PsiElement element, DeclarativeInlayTreeSink sink) {
            DotQualifiedExpression top = ObjectUtil.tryCast(element, getDotQualifiedClass());
            if (top == null || getParentDotQualifiedExpression(top) != null) {
                return;
            }

            TypeComputationContext context = getTypeComputationContext(top);
            boolean someTypeIsUnknown = false;
            List<ExpressionWithType<ExpressionType>> chain = new ArrayList<>();

            PsiElement current = top;
            while (true) {
                PsiElement down = skipParenthesesAndPostfixOperatorsDown(current);
                DotQualifiedExpression dot = ObjectUtil.tryCast(down, getDotQualifiedClass());
                current = (dot != null) ? getReceiver(dot) : null;
                if (current == null) {
                    break;
                }
                if (current.getNextSibling() instanceof PsiWhiteSpace && current.getNextSibling().getText().contains("\n")) {
                    ExpressionType type = getType(current, context);
                    if (type == null) {
                        someTypeIsUnknown = true;
                        break;
                    }
                    chain.add(new ExpressionWithType<>(current, type));
                }
            }

            if (someTypeIsUnknown) {
                return;
            }

            List<ExpressionWithType<ExpressionType>> filtered = new ArrayList<>();
            for (int i = 0; i < chain.size(); i++) {
                ExpressionWithType<ExpressionType> currentWithType = chain.get(i);
                ExpressionWithType<ExpressionType> prevWithType = (i > 0) ? chain.get(i - 1) : null;
                if (prevWithType == null) {
                    PsiElement down = skipParenthesesAndPostfixOperatorsDown(currentWithType.expression);
                    if (getDotQualifiedClass().isInstance(down)) {
                        filtered.add(currentWithType);
                    }
                }
                else {
                    PsiElement prevDown = skipParenthesesAndPostfixOperatorsDown(prevWithType.expression);
                    if (!Objects.equals(currentWithType.type, prevWithType.type)
                        || !getDotQualifiedClass().isInstance(prevDown)) {
                        filtered.add(currentWithType);
                    }
                }
            }

            if (isChainUnacceptable(filtered)) {
                return;
            }

            long distinctCount = filtered.stream().map(e -> e.type).distinct().count();
            if (distinctCount < uniqueTypeCount()) {
                return;
            }

            for (ExpressionWithType<ExpressionType> et : filtered) {
                PsiElement expr = et.expression;
                ExpressionType type = et.type;
                sink.addPresentation(
                    new DeclarativeInlayPosition.InlineInlayPosition(expr.getTextRange().getEndOffset(), true),
                    getHintFormat(),
                    builder -> buildTree(type, expr, file.getProject(), context, builder)
                );
            }
        }
    }

    protected HintFormat getHintFormat() {
        return HintFormat.DEFAULT;
    }

    protected boolean isAvailable(@Nonnull PsiFile file, @Nonnull Editor editor) {
        return true;
    }

    protected abstract void buildTree(ExpressionType type,
                                      PsiElement expression,
                                      Project project,
                                      TypeComputationContext context,
                                      DeclarativePresentationTreeBuilder treeBuilder);

    protected abstract ExpressionType getType(PsiElement element, TypeComputationContext context);

    protected abstract Class<DotQualifiedExpression> getDotQualifiedClass();

    protected abstract boolean isChainUnacceptable(List<ExpressionWithType<ExpressionType>> chain);

    protected abstract PsiElement getReceiver(DotQualifiedExpression expr);

    protected abstract DotQualifiedExpression getParentDotQualifiedExpression(DotQualifiedExpression expr);

    protected abstract PsiElement skipParenthesesAndPostfixOperatorsDown(PsiElement element);

    protected abstract TypeComputationContext getTypeComputationContext(DotQualifiedExpression topmostDotQualifiedExpression);

    protected int uniqueTypeCount() {
        return 2;
    }
}
