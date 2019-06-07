COMP_FEUP

**PROJECT TITLE: The jmm Compiler

**GROUP: G34

NAME1: Diogo Moreira, NR1: 201504359, GRADE1: 14, CONTRIBUTION1: 32%
NAME2: Manuel Monteiro, NR2: 201504445, GRADE2: 14, CONTRIBUTION2: 32%
NAME3: João Mendes, NR3: 201505439, GRADE3: 14, CONTRIBUTION3: 32%
NAME4: Bernardo Santos, NR4: 201504711, GRADE4: 2, CONTRIBUTION4: 4%

...

GLOBAL Grade of the project: 16



** SUMMARY: (Describe what your tool does and its main features.)

Our tool features the grammar anlysis and Symbol Tables for Class and functions of the jmm file to evaluate.
As wel as JVM Byte Codes for jasmine to interpret 



** EXECUTE: (indicate how to run your tool)

To execute an user must follow the normal indications:

cd "C:\javacc-6.0\javacc-6.0\bin"
open_cmd.bat "C:\Program Files\Java\jdk1.8.0_211\bin"
cd "C:\Users\diogo\Documents\GitKraken\y2019-g34\src\Semantics"
____________________________________________________________

jjtree Parser.jjt
javacc Parser.jj
javac *.java
java Parser

To change the file that is being evaluated, an user must only change the line 13 in the file Parser.jjt, "String s = "MonteCarloPi.jmm";". Where "MonteCarloPi.jmm" must be the name (and/or path) to the desired file.



**DEALING WITH SYNTACTIC ERRORS: (Describe how the syntactic error recovery of your tool does work. Does it exit after the first error?)

Our small error treatment was done by Bernardo Santos. He did that and never contacted the group again, so we don't know the extent of his work.
The work was later on made to skip while loops and other syntactic errors.



**SEMANTIC ANALYSIS: (Refer the semantic rules implemented by your tool.)

Type Checking

**INTERMEDIATE REPRESENTATIONS (IRs): (for example, when applicable, briefly describe the HLIR (high-level IR) and the LLIR (low-level IR) used, if your tool includes an LLIR with structure different from the HLIR)

Our tool prints the AST, where only CAPITAL letters refer to the following children. For example, if in a certain line there is EXPRESSION + EXPRESSION, we may expect that the two children of this node will descend the respective EXPRESSION. The same goes for STATEMENT and others.


**CODE GENERATION: (when applicable, describe how the code generation of your tool works and identify the possible problems your tool has regarding code generation.)

JOAO MENDES

**OVERVIEW: (refer the approach used in your tool, the main algorithms, the third-party tools and/or packages, etc.)

We kept it simple, only using recursive functions.


**TASK DISTRIBUTION: (Identify the set of tasks done by each member of the project.)

Diogo Moreira and Manuel Mointeiro did everything till the semantic analysis. We started the semantic analysis but didn't have time to complete it. João Mendes did everything form there, mainly JVM.
As refered before Bernardo Santos only did the error treatment in the grammar analysis.

**PROS: (Identify the most positive aspects of your tool)

It's simple but what it does, it does well.


**CONS: (Identify the most negative aspects of your tool)

Doesn't have semantic analysis...