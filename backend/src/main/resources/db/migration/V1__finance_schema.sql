CREATE TABLE app_user (
 id VARCHAR(36) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL,
 email VARCHAR(190) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL,
 display_name VARCHAR(100) NOT NULL, user_role VARCHAR(20) NOT NULL
);
CREATE TABLE finance_account (
 id VARCHAR(36) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL,
 name VARCHAR(100) NOT NULL, institution VARCHAR(100) NOT NULL,
 currency VARCHAR(3) NOT NULL, account_type VARCHAR(20) NOT NULL,
 opening_balance DECIMAL(19,2) NOT NULL, active BOOLEAN NOT NULL,
 version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_account_tenant ON finance_account(tenant_id);
CREATE TABLE ledger_entry (
 id VARCHAR(36) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL,
 account_id VARCHAR(36) NOT NULL, kind VARCHAR(10) NOT NULL,
 amount DECIMAL(19,2) NOT NULL, currency VARCHAR(3) NOT NULL,
 base_amount DECIMAL(19,2) NOT NULL, fx_rate DECIMAL(24,10) NOT NULL,
 fx_date DATE NOT NULL, fx_source VARCHAR(32) NOT NULL,
 description VARCHAR(200) NOT NULL, category VARCHAR(40) NOT NULL,
 booked_on DATE NOT NULL, status VARCHAR(10) NOT NULL,
 client_ref VARCHAR(64) NOT NULL, created_at TIMESTAMP NOT NULL,
 CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES finance_account(id),
 CONSTRAINT uq_ledger_idempotency UNIQUE(tenant_id, client_ref),
 CONSTRAINT ck_positive_amount CHECK (amount > 0),
 CONSTRAINT ck_ledger_kind CHECK (kind IN ('INCOME','EXPENSE')),
 CONSTRAINT ck_ledger_status CHECK (status IN ('POSTED','VOID'))
);
CREATE INDEX idx_ledger_tenant_date ON ledger_entry(tenant_id, booked_on, status);
CREATE INDEX idx_ledger_account ON ledger_entry(tenant_id, account_id, status);
CREATE TABLE fx_rate (
 id VARCHAR(36) PRIMARY KEY, base_currency VARCHAR(3) NOT NULL,
 quote_currency VARCHAR(3) NOT NULL, rate_date DATE NOT NULL,
 rate DECIMAL(24,10) NOT NULL, source VARCHAR(32) NOT NULL,
 fetched_at TIMESTAMP NOT NULL,
 CONSTRAINT uq_fx_observation UNIQUE(base_currency, quote_currency, rate_date, source)
);
CREATE INDEX idx_fx_pair_date ON fx_rate(base_currency, quote_currency, rate_date);
CREATE TABLE audit_event (
 id VARCHAR(36) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL,
 actor VARCHAR(190) NOT NULL, action VARCHAR(40) NOT NULL,
 subject_id VARCHAR(64) NOT NULL, detail VARCHAR(255) NOT NULL,
 created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_tenant_date ON audit_event(tenant_id, created_at);
