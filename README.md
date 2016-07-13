# CCeind


TempName language compiler. For more information about the language see the report.

####################################Setup Overview#################################################
All of the necesary files should be contained in this zip. The iloc package is a one to one copy 
of the iloc version on blackboard. Updating iloc versions can be done be replacing it over this
package and recompiling.
 
If you want to generate the Tree walkers and visitors from the ANTLR grammar the ANTLR4.4 library
should be added to the classpath.

If you want to run the testfiles the JUNIT library should be added to the classpath.

####################################Compiling######################################################

To compile a program the TempNameCompiler should be executed. The TempName Compiler takes 2 input 
arguments, using the compiler incorrectly prints instructions on the standard output.

The first argument should be the file name, or the path to the file you want to compile.

The second argument is the mode, there are 2 modes: RUN and FILE.

The RUN mode compiles the program and immediatly executes the generated iloc program. Whereas the 
FILE mode compiles the program and stores it in <filename>.iloc.

USAGE : TempNameCompiler <filepath> <mode>