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
package consulo.sandboxPlugin.lang.psi;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Guard conditions of declarations inside {@code #if}/{@code #else}/{@code #end} blocks.
 * A condition is a conjunction of flag literals in structural order, e.g. {@code "A&!B"};
 * the empty string means unconditional. Conditions are derived from the AST when stubs are
 * built and evaluated against a flag environment at query and resolve time.
 */
public final class SandConditions {
    private SandConditions() {
    }

    public static String conditionOf(PsiElement element) {
        List<String> literals = new ArrayList<>();
        PsiElement current = element;
        PsiElement parent = current.getParent();
        while (parent != null && !(parent instanceof PsiFile)) {
            ASTNode parentNode = parent.getNode();
            if (parentNode != null && parentNode.getElementType() == SandElements.CONDITIONAL_BLOCK) {
                String name = flagName(parentNode);
                if (name != null) {
                    boolean inElse = isAfterElse(parentNode, current.getNode());
                    literals.add(0, (inElse ? "!" : "") + name);
                }
            }
            current = parent;
            parent = parent.getParent();
        }
        return String.join("&", literals);
    }

    public static boolean matches(String condition, Set<String> environment) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        for (String literal : condition.split("&")) {
            if (literal.startsWith("!")) {
                if (environment.contains(literal.substring(1))) {
                    return false;
                }
            }
            else if (!environment.contains(literal)) {
                return false;
            }
        }
        return true;
    }

    private static String flagName(ASTNode conditionalBlock) {
        ASTNode ifDirective = conditionalBlock.findChildByType(SandElements.IF_DIRECTIVE);
        if (ifDirective == null) {
            return null;
        }
        ASTNode identifier = ifDirective.findChildByType(SandTokens.IDENTIFIER);
        return identifier == null ? null : identifier.getText();
    }

    private static boolean isAfterElse(ASTNode conditionalBlock, ASTNode child) {
        ASTNode elseDirective = conditionalBlock.findChildByType(SandElements.ELSE_DIRECTIVE);
        if (elseDirective == null) {
            return false;
        }
        return child.getStartOffset() > elseDirective.getStartOffset();
    }
}
