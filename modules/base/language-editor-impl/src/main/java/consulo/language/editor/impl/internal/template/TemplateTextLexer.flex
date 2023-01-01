/* It's an automatically generated code. Do not modify it. */
package consulo.language.editor.impl.internal.template;

import consulo.language.ast.IElementType;
import consulo.language.lexer.LexerBase;

%%

%public
%unicode
%class TemplateTextLexer
%extends LexerBase
%function advanceImpl
%type IElementType
%eof{  return;
%eof}

ALPHA=[A-Za-z_]
DIGIT=[0-9]
VARIABLE="$"({ALPHA}|{DIGIT})+"$"

%%

<YYINITIAL> "$""$" { return TemplateTokenType.ESCAPE_DOLLAR; }
<YYINITIAL> {VARIABLE} { return TemplateTokenType.VARIABLE; }
<YYINITIAL> [^] { return TemplateTokenType.TEXT; }
