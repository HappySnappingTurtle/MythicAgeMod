package com.example.mythic_age.entity;

public enum WalkingTreeVariant {
    OAK("oak"),
    SPRUCE("spruce"),
    BIRCH("birch"),
    JUNGLE("jungle"),
    ACACIA("acacia"),
    DARK_OAK("dark_oak"),
    MANGROVE("mangrove"),
    CHERRY("cherry");

    private final String serializedName;

    WalkingTreeVariant(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public static WalkingTreeVariant fromOrdinal(int ordinal) {
        WalkingTreeVariant[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return OAK;
        }
        return values[ordinal];
    }

    public static WalkingTreeVariant fromSerializedName(String name) {
        for (WalkingTreeVariant value : values()) {
            if (value.serializedName.equals(name)) {
                return value;
            }
        }
        return OAK;
    }
}
