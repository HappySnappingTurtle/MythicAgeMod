package com.example.mythic_age.item.spell;

public enum SpellCategory {
    OFFENSIVE("offensive"),
    HEALING("healing"),
    UTILITY("utility"),
    SUMMONING("summoning"),
    DEBUFF("debuff"),
    BUFF("buff"),
    TRANSPORT("transportation"),
    RESOURCE("resource");

    private final String id;

    SpellCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
