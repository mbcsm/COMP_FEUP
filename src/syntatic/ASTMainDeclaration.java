/* Generated By:JJTree: Do not edit this line. ASTMainDeclaration.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
public class ASTMainDeclaration extends SimpleNode {

  public String args = "";

  public ASTMainDeclaration(int id) {
    super(id);
  }

  public ASTMainDeclaration(Javamm p, int id) {
    super(p, id);
  }

  @Override
  public String getType() throws SemanticException {
    return "";
  }

  @Override
  public String toString() {
    return "main(" + args + ")";
  }
}
/*
 * JavaCC - OriginalChecksum=d36cf5d044cfad0f7a38d1ee514c7e76 (do not edit this
 * line)
 */
