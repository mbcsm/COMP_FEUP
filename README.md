COMP_FEUP

** PROJECT TITLE: The jmm Compiler **

**GROUP: G34 **

NAME1: Diogo Moreira, NR1: 201504359, GRADE1: 14, CONTRIBUTION1: 32%
NAME2: Manuel Monteiro, NR2: 201504445, GRADE2: 14, CONTRIBUTION2: 32%
NAME3: João Mendes, NR3: 201505439, GRADE3: 14, CONTRIBUTION3: 32%
NAME4: Bernardo Santos, NR4: 201504711, GRADE4: 2, CONTRIBUTION4: 4%

...

GLOBAL Grade of the project: 16



** SUMMARY **:

Our tool features a compiler capable of generating Java bytecode, a low level language capable of operation the Java Virtual Machine from Java--, a high level programing language. The main features of our compiler are its ability to perform syntatic analysis, semantic analysis and generating low level code (jasmin readable).


** EXECUTE **:

To execute an user must follow the normal indications:

#### Compiling:
Inside base directory
```
$ sh compile.sh
```

#### Running:

##### Running Code Generation and Semantic Analysis
Inside base directory
```
$ sh run.sh <FILENAME>
```

##### Running Syntactic Analysis (Tree Dump)
Inside base directory
```
$ sh tree.sh <FILENAME>
```

**DEALING WITH SYNTACTIC ERRORS **:

Our initial error treatment was done by Bernardo Santos. He did that and never contacted the group again, so we don't know the extent of his work - explaining low contribution.

The work was later on made to skip while loops and other syntactic errors. Our tool tries to show all the syntatic errors found, so that it does not exit after the first error. For example, if it founds an error in the assignement of a variable, the parser skips to the next semicolon and starts looking for syntatic errors from there.



**SEMANTIC ANALYSIS **:

We check that if a variable is going to be assigned to another, they must have the same type (except for int and array types)


**INTERMEDIATE REPRESENTATIONS (IRs) **: 

Our tool prints the AST, where only CAPITAL letters refer to the following children. For example, if in a certain line there is EXPRESSION + EXPRESSION, we may expect that the two children of this node will descend the respective EXPRESSION. The same goes for STATEMENT and others.


**CODE GENERATION **:

If there aren't any semantic errors, it is generated the jasmin code for the jmm file. The code generation uses the AST as the basis with support from the symbol tables, mostly for variable type checks. While going through it, it the generates the appropriate code.


**OVERVIEW **:

In the end, we feel like we have made almost fully functional tool with a lot of capabilities, however, if given the possibility and time, we would definitely taken a different approach for code structure, since after we realised that there was another way, it was too late for a refactor. Array code generation is missing.


**TASK DISTRIBUTION **:

Diogo Moreira and Manuel Mointeiro did everything untill the semantic analysis. 
We started the semantic analysis but didn't have time to make it very thorough. 
João Mendes did everything from there, mainly JVM.

As refered before Bernardo Santos only did the a small part in error treatment in the grammar analysis.

**PROS **:

It's simple but what it does, it does well.


**CONS **:

Incomplete parts, mainly arrays in code generation.