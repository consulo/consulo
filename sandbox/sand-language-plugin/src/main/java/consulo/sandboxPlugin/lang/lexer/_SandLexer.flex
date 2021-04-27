package consulo.sandboxPlugin.lang.lexer;

import java.util.*;
import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import consulo.sandboxPlugin.lang.psi.SandTokens;

%%

%public
%class SandLexer
%extends LexerBase
%unicode
%function advanceImpl
%type IElementType
%eof{  return;
%eof}


%state MACRO
%state MACRO_ENTERED
%state MACRO_EXPRESSION

DIGIT=[0-9]
LETTER=[a-z]|[A-Z]
WHITE_SPACE=[ \n\r\t\f]+
SINGLE_LINE_COMMENT="/""/"[^\r\n]*
MULTI_LINE_STYLE_COMMENT=("/*"[^"*"]{COMMENT_TAIL})|"/*"

COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
CHARACTER_LITERAL="'"([^\\\'\r\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
STRING_LITERAL=\"([^\\\"\r\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ESCAPE_SEQUENCE=\\[^\r\n]


IDENTIFIER=[:jletter:] [:jletterdigit:]*

%%

<YYINITIAL>
{
  "class"                   { return SandTokens.CLASS_KEYWORD; }
  {SINGLE_LINE_COMMENT}     { return SandTokens.LINE_COMMENT; }
  {IDENTIFIER}              { return SandTokens.IDENTIFIER; }
  {WHITE_SPACE}             { return SandTokens.WHITE_SPACE; }
  .                         { return SandTokens.BAD_CHARACTER; }
}
