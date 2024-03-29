/* It's an automatically generated code. Do not modify it. */
package consulo.language.editor.template.macro;

import consulo.language.ast.TokenType;
import consulo.language.ast.IElementType;
import consulo.language.lexer.LexerBase;

%%

%unicode
%public
%class MacroLexer
%extends LexerBase
%function advanceImpl
%type IElementType
%eof{  return;
%eof}

IDENTIFIER=[:jletter:] [:jletterdigit:]*
WHITE_SPACE_CHAR=[\ \n\r\t\f]
STRING_LITERAL=\"([^\\\"\r\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ESCAPE_SEQUENCE=\\[^\r\n]

%%

{IDENTIFIER} { return MacroTokenType.IDENTIFIER; }
{WHITE_SPACE_CHAR}+ { return MacroTokenType.WHITE_SPACE; }
{STRING_LITERAL} { return MacroTokenType.STRING_LITERAL; }
"(" { return MacroTokenType.LPAREN; }
")" { return MacroTokenType.RPAREN; }
"," { return MacroTokenType.COMMA; }
"=" { return MacroTokenType.EQ; }
. { return TokenType.BAD_CHARACTER; }
