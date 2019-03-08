COMP_FEUP

Instructions to install:

On Windows:
    - C:\Program files\Java\jdk...\bin or C:\Program files (x86)\Java\jdk...\bin
    - execute the open_cmd.bat script with the "(...)\jdk...\bin" path as argument

Instructions to run:

1. Run javacc on the grammar input file to generate a bunch of Java
   files that implement the parser and lexical analyzer (or token
   manager):

	javacc Simple1.jj

2. Now compile the resulting Java programs:

	javac *.java

3. The parser is now ready to use.  To run the parser, type:

	java Simple1