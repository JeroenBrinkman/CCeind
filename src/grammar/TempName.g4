grammar TempName;

program
	: START expr EOF
	;


/** Target of an assignment. */
target 
    : ID               #idTarget
    ;
   

/** Expression. */
expr: type ID (ASS expr)? 					#declExpr
	| <assoc= right >target ASS expr		#assExpr
    | IF expr THEN expr (ELSE expr)? 		#ifExpr
    | WHILE expr DO expr             		#whileExpr
    | READ LPAR ID (COMMA ID)* RPAR			#readExpr
    | PRINT LPAR expr (COMMA expr)* RPAR	#printExpr
    | prfOp expr        					#prfExpr
    | expr multOp expr  					#multExpr
    | expr plusOp expr  					#plusExpr
    | expr compOp expr  					#compExpr
    | expr boolOp expr  	   		 		#boolExpr
    | LPAR expr RPAR    			  		#parExpr
    | LBRACE (expr SEMI)* expr SEMI RBRACE 	#blockExpr
    | ID                					#idExpr
    | NUM               					#numExpr
    | TRUE              					#trueExpr
    | FALSE             					#falseExpr
    | CHR									#charExpr
    | STR 									#stringExpr
    ;

/** Prefix operator. */
prfOp: MINUS | NOT;

/** Multiplicative operator. */
multOp: STAR | SLASH | MODULO;

/** Additive operator. */
plusOp: PLUS | MINUS;

/** Boolean operator. */
//TODO appart?
boolOp: AND | OR;

/** Comparison operator. */
compOp: LE | LT | GE | GT | EQ | NE;

/** Data type. */
type: INTEGER  #intType
    | BOOLEAN  #boolType
    | CHAR 	   #charType
    | STRING   #stringType
    ;
    
// Keywords
AND:     A N D;
BOOLEAN: B O O L E A N ;
CHAR:    C H A R ;
INTEGER: I N T E G E R ;
DO:      D O ;
ELSE:    E L S E ;
FALSE:   F A L S E ;
IF:      I F ;
THEN:    T H E N ;
NOT:     N O T ;
OR:      O R ;
OUT:     O U T ;
PRINT: 	 P R I N T;
READ:    R E A D ;
START:	 S T A R T ;
STRING:  S T R I N G ;
TRUE:    T R U E ;
WHILE:   W H I L E ;

ASS:    ':=';
COLON:  ':';
COMMA:  ',';
DOT:    '.';
DQUOTE: '"';
EQ:     '==';
GE:     '>=';
GT:     '>';
HTAG:	'#';
LE:     '<=';
LBRACE: '{';
LPAR:   '(';
LT:     '<';
MINUS:  '-';
MODULO:	'%';
NE:     '<>';
PLUS:   '+';
QUOTE:  '\'';
RBRACE: '}';
RPAR:   ')';
SEMI:   ';';
SLASH:  '/';
STAR:   '*';

// Content-bearing token types
ID: LETTER (LETTER | DIGIT)*;
NUM: DIGIT (DIGIT)*;
STR: DQUOTE .*? DQUOTE;
CHR: QUOTE LETTER QUOTE;

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];

// Skipped token types
WS: [ \t\r\n]+ -> skip;
COMMENT: HTAG .*? HTAG -> skip;

fragment A: [aA];
fragment B: [bB];
fragment C: [cC];
fragment D: [dD];
fragment E: [eE];
fragment F: [fF];
fragment G: [gG];
fragment H: [hH];
fragment I: [iI];
fragment J: [jJ];
fragment K: [kK];
fragment L: [lL];
fragment M: [mM];
fragment N: [nN];
fragment O: [oO];
fragment P: [pP];
fragment Q: [qQ];
fragment R: [rR];
fragment S: [sS];
fragment T: [tT];
fragment U: [uU];
fragment V: [vV];
fragment W: [wW];
fragment X: [xX];
fragment Y: [yY];
fragment Z: [zZ];