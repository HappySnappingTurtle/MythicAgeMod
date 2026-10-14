package io.github.happysnappingturtle.mythicage.core.player.tx;

import io.github.happysnappingturtle.mythicage.core.player.PlayerMythData;
import io.github.happysnappingturtle.mythicage.core.player.ReceiptBook;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 事务执行器（T4 设计文档 §3；12 号合同 §2）：查收据 → 全部 check → 全部 apply → 写收据并递增版本。
 * 服务端单线程执行，同一 tick 内 check 与 apply 之间不会插入其他修改。
 */
public final class Transactions {
    public enum Status { COMMITTED, DUPLICATE, REJECTED }

    public record Request(String requestId, ReceiptBook.Scope scope, List<TxStep> steps) {
        public Request {
            Objects.requireNonNull(requestId);
            Objects.requireNonNull(scope);
            steps = List.copyOf(steps);
        }

        /** 防重复请求（交易点击、网络重试）。 */
        public static Request of(String requestId, TxStep... steps) {
            return new Request(requestId, ReceiptBook.Scope.REQUEST, List.of(steps));
        }

        /** 唯一奖励（首杀、任务、晋升奖励）。 */
        public static Request unique(String rewardId, TxStep... steps) {
            return new Request(rewardId, ReceiptBook.Scope.UNIQUE, List.of(steps));
        }
    }

    public record Result(Status status, String reason, long dataVersion) {
        public boolean committed() {
            return status == Status.COMMITTED;
        }
    }

    private Transactions() {
    }

    /**
     * @throws RuntimeException apply 阶段抛出的异常原样向上抛出；此时不写收据、不递增版本，以便修复后重试
     */
    public static Result execute(PlayerMythData data, Request request) {
        if (data.receipts().contains(request.scope(), request.requestId())) {
            return new Result(Status.DUPLICATE, "重复请求：" + request.requestId(), data.dataVersion());
        }
        for (TxStep step : request.steps()) {
            Optional<String> failure = step.check(data);
            if (failure.isPresent()) {
                return new Result(Status.REJECTED, failure.get(), data.dataVersion());
            }
        }
        for (TxStep step : request.steps()) {
            step.apply(data);
        }
        data.receipts().record(request.scope(), request.requestId());
        data.markChanged();
        return new Result(Status.COMMITTED, "", data.dataVersion());
    }
}
