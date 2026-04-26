package DHCP;

public class FieldAttributes {
    public String name; // for printing and all
    public int len; // in bytes
    public boolean isVariable; // if true, len is the size per field

    public FieldAttributes(String name, int len, boolean isVariable) {
        this.name = name;
        this.len  = len;
        this.isVariable = isVariable;
    }

}
