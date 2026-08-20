package com.wallet.account.infrastructure.projection;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.wallet.account.domain.Account;
import com.wallet.account.infrastructure.persistence.AccountViewEntity;
import com.wallet.account.infrastructure.persistence.AccountViewJpaRepository;
import com.wallet.shared.event.AccountEvent;
import com.wallet.shared.event.AccountOpenedEvent;
import com.wallet.shared.event.MoneyDepositedEvent;
import com.wallet.shared.event.MoneyWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Maintains the CQRS read model.
 *
 * Source of truth: events (internal publishes + Kafka inbound).
 * Target of writes: Redis (primary, hot) + Postgres (durable backup).
 *
 * Why update Redis AFTER Postgres commit:
 * If we wrote Redis first and Postgres rolled back we'd have a phantom
 * projection; flipping the order makes the read model eventually consistent
 * with the write model in the worst case, never wrong in the meantime.
 *
 * Note on idempotency: the consumer that drives this service is responsible
 * for not re-processing events; here we just apply whatever arrives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountProjectionService {

    private final StringRedisTemplate redis;
    private final AccountViewJpaRepository viewRepository;
    private final ObjectMapper objectMapper;

    public void project(AccountEvent event) {
        switch (event) {
            case AccountOpenedEvent e  -> handleOpened(e);
            case MoneyDepositedEvent e -> handleDeposited(e);
            case MoneyWithdrawnEvent e -> handleWithdrawn(e);
        }
    }

    @Transactional
    protected void handleOpened(AccountOpenedEvent e) {
        AccountView view = new AccountView(
            e.accountId(),
            e.userId(),
            e.initialBalance().amount(),
            e.initialBalance().currency(),
            Account.Status.OPEN,
            1L,
            Instant.now()
        );
        writeRedis(view);
        writePostgres(view);
        log.info("projection: opened accountId={}", e.accountId());
    }

    @Transactional
    protected void handleDeposited(MoneyDepositedEvent e) {
        AccountView previous = readOrFallback(e.accountId());
        AccountView view = new AccountView(
            e.accountId(),
            previous.userId(),
            e.newBalance().amount(),
            e.newBalance().currency(),
            previous.status() != null ? previous.status() : Account.Status.OPEN,
            previous.version() + 1,
            Instant.now()
        );
        writeRedis(view);
        writePostgres(view);
        log.info("projection: deposited accountId={} newBalance={} {}",
            e.accountId(), e.newBalance().amount(), e.newBalance().currency());
    }

    @Transactional
    protected void handleWithdrawn(MoneyWithdrawnEvent e) {
        AccountView previous = readOrFallback(e.accountId());
        AccountView view = new AccountView(
            e.accountId(),
            previous.userId(),
            e.newBalance().amount(),
            e.newBalance().currency(),
            previous.status() != null ? previous.status() : Account.Status.OPEN,
            previous.version() + 1,
            Instant.now()
        );
        writeRedis(view);
        writePostgres(view);
        log.info("projection: withdrawn accountId={} newBalance={} {}",
            e.accountId(), e.newBalance().amount(), e.newBalance().currency());
    }

    private AccountView readOrFallback(String accountId) {
        // Prefer Redis; fall back to Postgres if the cache is cold.
        String cached = redis.opsForValue().get("account:view:" + accountId);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, AccountView.class);
            } catch (JacksonException ex) {
                log.warn("projection cache decode failed accountId={}", accountId, ex);
            }
        }
        return viewRepository.findById(accountId)
            .map(v -> new AccountView(
                v.getAccountId(), v.getUserId(),
                v.getBalanceAmount(), v.getBalanceCurrency(),
                v.getStatus() == null ? null : Account.Status.valueOf(v.getStatus()),
                v.getVersion(), v.getLastUpdated()))
            .orElseThrow(() -> new IllegalStateException("no view for " + accountId));
    }

    private void writeRedis(AccountView view) {
        try {
            redis.opsForValue().set("account:view:" + view.accountId(), objectMapper.writeValueAsString(view));
        } catch (JacksonException e) {
            throw new IllegalStateException("failed to serialise AccountView", e);
        }
    }

    private void writePostgres(AccountView view) {
        AccountViewEntity entity = viewRepository.findById(view.accountId()).orElseGet(AccountViewEntity::new);
        entity.setAccountId(view.accountId());
        entity.setUserId(view.userId());
        entity.setBalanceAmount(view.balanceAmount());
        entity.setBalanceCurrency(view.balanceCurrency());
        entity.setStatus(view.status() == null ? null : view.status().name());
        entity.setLastUpdated(view.lastUpdated());
        viewRepository.save(entity);
    }
}
