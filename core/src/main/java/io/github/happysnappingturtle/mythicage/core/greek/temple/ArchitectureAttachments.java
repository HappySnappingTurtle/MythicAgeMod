package io.github.happysnappingturtle.mythicage.core.greek.temple;

import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** GA-6：与 tools/check_structures.py 共读依附数据表；只依赖坐标与方块标识，不依赖游戏类。 */
public final class ArchitectureAttachments {
    public record Pos(int x, int y, int z) {
        Pos offset(int dx, int dy, int dz) {
            return new Pos(x + dx, y + dy, z + dz);
        }
    }

    public record State(String id, String facing, Map<String, String> properties) {
        public State(String id, String facing) {
            this(id, facing, Map.of());
        }

        public State {
            properties = Map.copyOf(properties);
        }
    }

    private record Rule(List<String> directions, Set<String> supports,
                        Map<String, Map<String, Set<String>>> supportProperties) {
        boolean accepts(State state) {
            return supports.contains(state.id()) && supportProperties.getOrDefault(state.id(), Map.of()).entrySet().stream()
                    .allMatch(e -> state.properties().containsKey(e.getKey())
                            && e.getValue().contains(state.properties().get(e.getKey())));
        }
    }

    private final Map<String, Rule> rules;

    private ArchitectureAttachments(Map<String, Rule> rules) {
        this.rules = Map.copyOf(rules);
    }

    public static ArchitectureAttachments parse(String json) {
        var root = JsonParser.parseString(json).getAsJsonObject();
        Set<String> solid = new HashSet<>();
        root.getAsJsonArray("solid_blocks").forEach(e -> solid.add(e.getAsString()));
        Map<String, Rule> rules = new HashMap<>();
        root.getAsJsonObject("blocks").entrySet().forEach(entry -> {
            var value = entry.getValue().getAsJsonObject();
            List<String> directions = new ArrayList<>();
            value.getAsJsonArray("directions").forEach(e -> {
                String direction = e.getAsString();
                if (!Set.of("down", "up", "back", "left", "right").contains(direction)) {
                    throw new IllegalArgumentException("无效依附方向：" + direction);
                }
                directions.add(direction);
            });
            Set<String> supports = new HashSet<>(solid);
            value.getAsJsonArray("allowed_supports").forEach(e -> supports.add(e.getAsString()));
            Map<String, Map<String, Set<String>>> properties = new HashMap<>();
            if (value.has("support_properties")) {
                value.getAsJsonObject("support_properties").entrySet().forEach(support -> {
                    Map<String, Set<String>> conditions = new HashMap<>();
                    support.getValue().getAsJsonObject().entrySet().forEach(property -> {
                        Set<String> allowed = new HashSet<>();
                        property.getValue().getAsJsonArray().forEach(e -> allowed.add(e.getAsString()));
                        conditions.put(property.getKey(), Set.copyOf(allowed));
                    });
                    properties.put(support.getKey(), Map.copyOf(conditions));
                });
            }
            rules.put(entry.getKey(), new Rule(List.copyOf(directions), Set.copyOf(supports), Map.copyOf(properties)));
        });
        return new ArchitectureAttachments(rules);
    }

    public boolean allowsSupport(String decoration, String support) {
        Rule rule = rules.get(decoration);
        return rule != null && rule.accepts(new State(support, "north"));
    }

    public List<String> check(String name, Map<Pos, State> blocks) {
        List<String> errors = new ArrayList<>();
        blocks.forEach((pos, state) -> {
            Rule rule = rules.get(state.id());
            if (rule == null || rule.directions().isEmpty()) {
                return;
            }
            boolean supported = rule.directions().stream().anyMatch(direction -> {
                State other = blocks.get(supportPos(pos, state.facing(), direction));
                return other != null && rule.accepts(other);
            });
            if (!supported) {
                errors.add(name + ": " + pos + " " + state.id() + " 无依附支撑");
            }
        });
        return List.copyOf(errors);
    }

    private static Pos supportPos(Pos pos, String facing, String direction) {
        if (direction.equals("down")) {
            return pos.offset(0, -1, 0);
        }
        if (direction.equals("up")) {
            return pos.offset(0, 1, 0);
        }
        int x = 0, z = 0;
        switch (facing) {
            case "north" -> z = -1;
            case "east" -> x = 1;
            case "south" -> z = 1;
            case "west" -> x = -1;
            default -> throw new IllegalArgumentException("无效朝向：" + facing);
        }
        return switch (direction) {
            case "back" -> pos.offset(-x, 0, -z);
            case "left" -> pos.offset(z, 0, -x);
            case "right" -> pos.offset(-z, 0, x);
            default -> throw new IllegalArgumentException("无效依附方向：" + direction);
        };
    }
}
