package io.github.happysnappingturtle.mythicage.core.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 持有一份配置的当前生效值，并负责热更。规则见 T3 设计文档 §4。
 * <p>
 * 读取是无锁的：任何线程调用 {@link #current()} 都会看到某个完整快照。
 * 热更只应在服务端主线程上调用。
 */
public final class ConfigHolder<T> {
    /** 生效前钩子：在新值替换旧值之前调用；抛出异常会中止本次热更。 */
    @FunctionalInterface
    public interface PreApplyHook<T> {
        void beforeApply(T oldValue, T newValue) throws Exception;
    }

    /** 生效后监听器：抛出的异常只记录到结果中，不回滚。 */
    @FunctionalInterface
    public interface ChangeListener<T> {
        void onApplied(T oldValue, T newValue, long version);
    }

    public enum Status { APPLIED, UNCHANGED, KEPT_OLD, ABORTED }

    public record ReloadOutcome(Status status, List<ConfigIssue> issues, long version, String reason) {
        public ReloadOutcome {
            issues = List.copyOf(issues);
        }
    }

    private record Snapshot<T>(T value, long version) {
    }

    private final ConfigSchema<T> schema;
    private final AtomicReference<Snapshot<T>> snapshot;
    private final List<PreApplyHook<T>> hooks = new CopyOnWriteArrayList<>();
    private final List<ChangeListener<T>> listeners = new CopyOnWriteArrayList<>();

    private ConfigHolder(ConfigSchema<T> schema, T initial) {
        this.schema = schema;
        this.snapshot = new AtomicReference<>(new Snapshot<>(initial, 1));
    }

    /** 用首次加载的结果创建，版本号为 1。 */
    public static <T> ConfigHolder<T> create(ConfigSchema<T> schema, ConfigLoadResult<T> initial) {
        return new ConfigHolder<>(schema, initial.value());
    }

    public ConfigSchema<T> schema() {
        return schema;
    }

    public T current() {
        return snapshot.get().value();
    }

    public long version() {
        return snapshot.get().version();
    }

    public void addPreApplyHook(PreApplyHook<T> hook) {
        hooks.add(hook);
    }

    public void addListener(ChangeListener<T> listener) {
        listeners.add(listener);
    }

    /** 读取文件并热更；文件不可读时等同于整份解析失败。 */
    public ReloadOutcome reloadFile(Path file) {
        ConfigFiles.ReadResult read = ConfigFiles.read(file);
        if (read.text() == null) {
            Snapshot<T> now = snapshot.get();
            return new ReloadOutcome(Status.KEPT_OLD, List.of(read.error()), now.version(), read.error().message());
        }
        return reload(read.text());
    }

    public synchronized ReloadOutcome reload(String text) {
        Snapshot<T> old = snapshot.get();
        ConfigLoadResult<T> loaded = ConfigLoader.load(schema, text, old.value());
        List<ConfigIssue> issues = new ArrayList<>(loaded.issues());
        if (loaded.usedFallback()) {
            return new ReloadOutcome(Status.KEPT_OLD, issues, old.version(), "整份文件无法解析，保留旧配置");
        }
        T newValue = loaded.value();
        if (newValue.equals(old.value())) {
            return new ReloadOutcome(Status.UNCHANGED, issues, old.version(), "");
        }
        for (PreApplyHook<T> hook : hooks) {
            try {
                hook.beforeApply(old.value(), newValue);
            } catch (Exception e) {
                String reason = "生效前钩子失败，已中止本次热更：" + e;
                issues.add(ConfigIssue.error("", reason));
                return new ReloadOutcome(Status.ABORTED, issues, old.version(), reason);
            }
        }
        Snapshot<T> next = new Snapshot<>(newValue, old.version() + 1);
        snapshot.set(next);
        for (ChangeListener<T> listener : listeners) {
            try {
                listener.onApplied(old.value(), newValue, next.version());
            } catch (RuntimeException e) {
                issues.add(ConfigIssue.warn("", "监听器异常（配置已生效）：" + e));
            }
        }
        return new ReloadOutcome(Status.APPLIED, issues, next.version(), "");
    }
}
