/* Generated By:JJTree: Do not edit this line. ASTMethodArgumentPiece.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
public class ASTMethodArgumentPiece extends SimpleNode {

  public String name = "", type = "";

  public ASTMethodArgumentPiece(int id) {
    super(id);
  }

  public ASTMethodArgumentPiece(Javamm p, int id) {
    super(p, id);
  }

  @Override
  public String getType() throws SemanticException {
    return "";
  }

  @Override
  public String toString() {
    return "Argument (" + name + ")";
  }

  public String getMethodArgumentName() {
    return name;
  }

  public String getMethodArgumentType() {
    return type;
  }

}
/*
 * JavaCC - OriginalChecksum=d38823dfd538374f7610fe4a56907105 (do not edit this
 * line)
 */
