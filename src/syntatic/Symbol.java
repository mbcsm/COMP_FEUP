class Symbol {
    String type = "", name = "", boolValue = "";
    int register;

    Symbol(String name, String type) {
        this.name = name;
        this.type = type;
        this.boolValue = "null";
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getRegister() {
        return register;
    }

    public void setBool(String bool) {
        boolValue = bool;
    }

    public String getBool() {
        return boolValue;
    }

    public void setRegister(int reg) {
        register = reg;
    }
}