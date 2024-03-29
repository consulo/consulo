/* It's an automatically generated code. Do not modify it. */
package consulo.ide.impl.idea.ide.fileTemplates.impl;

import com.intellij.lexer.LexerBase;
import consulo.ide.impl.psi.tree.IElementType;

%%

%unicode
%public
%class FileTemplateTextLexer
%extends LexerBase
%function advanceImpl
%type IElementType
%eof{  return;
%eof}

ALPHA=[A-Za-z_]
DIGIT=[0-9]
MACRO="$"({ALPHA}|{DIGIT})+|"$""{"({ALPHA}|{DIGIT})+"}"
DIRECTIVE="#"{ALPHA}+

%%

<YYINITIAL> {MACRO} { return FileTemplateTokenType.MACRO; }
<YYINITIAL> {DIRECTIVE} { return FileTemplateTokenType.DIRECTIVE; }
<YYINITIAL> [^] { return FileTemplateTokenType.TEXT; }
