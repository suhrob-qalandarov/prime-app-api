package org.exp.primeapp.botadmin.models;

/**
 * Kiyim-kechak uchun asosiy ranglar
 */
public enum ProductColor {
    
    BLACK("Qora", "#000000"),
    WHITE("Oq", "#FFFFFF"),
    RED("Qizil", "#FF0000"),
    BLUE("Ko'k", "#0000FF"),
    GREEN("Yashil", "#00FF00"),
    YELLOW("Sariq", "#FFFF00"),
    PINK("Qizg'ish", "#FF6B6B"),
    GRAY("Kulrang", "#808080"),
    BROWN("Jigarrang", "#8B4513");
    
    private final String name;
    private final String hex;
    
    ProductColor(String name, String hex) {
        this.name = name;
        this.hex = hex;
    }
    
    public String getName() {
        return name;
    }
    
    public String getHex() {
        return hex;
    }
    
    public static ProductColor[] getAllColors() {
        return values();
    }
}
