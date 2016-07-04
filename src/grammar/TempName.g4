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
	| target ASS expr						#assExpr
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
    | QUOTE CHAR QUOTE						#charExpr
    | DQUOTE STRING DQUOTE					#stringExpr
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
BEGIN:   B E G I N ;
BOOLEAN: B O O L E A N ;
CHAR:    C H A R ;
INTEGER: I N T E G E R ;
DO:      D O ;
ELSE:    E L S E ;
END:     E N D ;
EXIT:    E X I T ;
FALSE:   F A L S E ;
FUNC:    F U N C T I O N ;
IF:      I F ;
IN:      I N ;
THEN:    T H E N ;
NOT:     N O T ;
OR:      O R ;
OUT:     O U T ;
PRINT: 	 P R I N T;
PROC:    P R O C E D U R E ;
PROGRAM: P R O G R A M ;
READ:    R E A D ;
START:	 S T A R T ;
STRING:  S T R I N G ;
TRUE:    T R U E ;
VAR:     V A R ;
WHILE:   W H I L E ;

ASS:    ':=';
COLON:  ':';
COMMA:  ',';
DOT:    '.';
DQUOTE: '"';
EQ:     '==';
GE:     '>=';
GT:     '>';
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

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];

// Skipped token types
WS: [ \t\r\n]+ -> skip;

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