import java.util.HashMap;

public class Table {
    HashMap<String, Symbol> parametersTable;
    HashMap<String, Symbol> localVariablesTable;

    int loopCounter = -1, stack = 1;
    Symbol returnSymbol;

    Table() {
        parametersTable = new HashMap<String, Symbol>();
        localVariablesTable = new HashMap<String, Symbol>();
    }

    public Symbol getSymbol(String name) {

        if (this.returnSymbol != null && name.equals(this.returnSymbol.getName()))
            return this.returnSymbol;
        else if (this.localVariablesTable != null)
            return this.localVariablesTable.get(name);
        else if (this.parametersTable != null)
            return this.parametersTable.get(name);
        else
            return null;
    }

    public void setVariables(HashMap<String, Symbol> vars) {
        localVariablesTable = vars;
    }

    public int getNumLocalVariables() {
        if (localVariablesTable != null)
            return localVariablesTable.size();
        else
            return 0;
    }

    public void setParameters(HashMap<String, Symbol> params) {
        parametersTable = params;
    }

    public void setReturn(Symbol s1) {
        returnSymbol = s1;
    }

    public HashMap<String, Symbol> getParams() {
        return parametersTable;
    }

    public HashMap<String, Symbol> getVars() {
        return localVariablesTable;
    }

    public Symbol getReturnSymbol() {
        return returnSymbol;
    }

    public int getLoopCounter() {
        return loopCounter;
    }

    public void incLoopCounter() {
        this.loopCounter++;
    }

    public int getStack() {
        return stack;
    }

    public void setMaxStack(int stack) {
        int stack_1 = Math.max(1, stack);
        this.stack = Math.max(this.stack, stack_1);
    }
}