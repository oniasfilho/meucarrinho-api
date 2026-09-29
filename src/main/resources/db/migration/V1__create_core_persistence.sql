CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    identity_provider VARCHAR(80) NOT NULL,
    identity_subject VARCHAR(255) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    email VARCHAR(320),
    sort_order VARCHAR(20) NOT NULL,
    collaboration_alerts BOOLEAN NOT NULL,
    haptics BOOLEAN NOT NULL,
    analytics_opt_out BOOLEAN NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    created_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL CHECK (version >= 1),
    CONSTRAINT uq_accounts_identity UNIQUE (identity_provider, identity_subject),
    CONSTRAINT ck_accounts_currency CHECK (currency = 'BRL'),
    CONSTRAINT ck_accounts_sort_order CHECK (sort_order IN ('ADDED', 'AZ'))
);

CREATE TABLE shopping_lists (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    store_name VARCHAR(80),
    budget_minor_units BIGINT CHECK (budget_minor_units IS NULL OR budget_minor_units >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    status VARCHAR(20) NOT NULL,
    status_before_delete VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    receipt_id UUID,
    version BIGINT NOT NULL CHECK (version >= 1),
    CONSTRAINT ck_shopping_lists_currency CHECK (currency = 'BRL'),
    CONSTRAINT ck_shopping_lists_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'DELETED')),
    CONSTRAINT ck_shopping_lists_previous_status CHECK (
        status_before_delete IS NULL OR status_before_delete IN ('ACTIVE', 'COMPLETED'))
);

CREATE TABLE shopping_list_members (
    list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    member_index INTEGER NOT NULL CHECK (member_index >= 0),
    actor_kind VARCHAR(12) NOT NULL,
    actor_id UUID NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (list_id, member_index),
    CONSTRAINT uq_shopping_list_member_actor UNIQUE (list_id, actor_kind, actor_id),
    CONSTRAINT ck_shopping_list_member_actor CHECK (actor_kind IN ('ACCOUNT', 'GUEST', 'DEVICE'))
);

CREATE TABLE shopping_list_items (
    item_id UUID PRIMARY KEY,
    list_id UUID NOT NULL REFERENCES shopping_lists(id) ON DELETE CASCADE,
    item_index INTEGER NOT NULL CHECK (item_index >= 0),
    name VARCHAR(120) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(8) NOT NULL,
    unit_price_minor_units BIGINT CHECK (unit_price_minor_units IS NULL OR unit_price_minor_units >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    note VARCHAR(280),
    photo_ref VARCHAR(512),
    picked BOOLEAN NOT NULL,
    picked_by_kind VARCHAR(12),
    picked_by_id UUID,
    last_edited_by_kind VARCHAR(12) NOT NULL,
    last_edited_by_id UUID NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    removed_at TIMESTAMPTZ,
    CONSTRAINT uq_shopping_list_item_index UNIQUE (list_id, item_index),
    CONSTRAINT uq_shopping_list_item_position UNIQUE (list_id, position),
    CONSTRAINT ck_shopping_list_item_unit CHECK (unit IN ('UN', 'KG')),
    CONSTRAINT ck_shopping_list_item_currency CHECK (currency = 'BRL'),
    CONSTRAINT ck_shopping_list_item_actor CHECK (last_edited_by_kind IN ('ACCOUNT', 'GUEST', 'DEVICE')),
    CONSTRAINT ck_shopping_list_item_picker CHECK (
        (picked_by_kind IS NULL AND picked_by_id IS NULL)
        OR (picked_by_kind IN ('ACCOUNT', 'GUEST', 'DEVICE') AND picked_by_id IS NOT NULL))
);

CREATE TABLE receipts (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL,
    list_name VARCHAR(80) NOT NULL,
    store_name VARCHAR(80),
    completed_at TIMESTAMPTZ NOT NULL,
    finished_by_kind VARCHAR(12) NOT NULL,
    finished_by_id UUID NOT NULL,
    total_minor_units BIGINT NOT NULL CHECK (total_minor_units >= 0),
    budget_minor_units BIGINT CHECK (budget_minor_units IS NULL OR budget_minor_units >= 0),
    budget_delta_minor_units BIGINT,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    CONSTRAINT ck_receipts_currency CHECK (currency = 'BRL'),
    CONSTRAINT ck_receipts_actor CHECK (finished_by_kind IN ('ACCOUNT', 'GUEST', 'DEVICE'))
);

CREATE TABLE receipt_participants (
    receipt_id UUID NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    participant_index INTEGER NOT NULL CHECK (participant_index >= 0),
    actor_kind VARCHAR(12) NOT NULL,
    actor_id UUID NOT NULL,
    PRIMARY KEY (receipt_id, participant_index),
    CONSTRAINT ck_receipt_participant_actor CHECK (actor_kind IN ('ACCOUNT', 'GUEST', 'DEVICE'))
);

CREATE TABLE receipt_lines (
    receipt_id UUID NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    line_index INTEGER NOT NULL CHECK (line_index >= 0),
    name VARCHAR(120) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(8) NOT NULL,
    unit_price_minor_units BIGINT CHECK (unit_price_minor_units IS NULL OR unit_price_minor_units >= 0),
    subtotal_minor_units BIGINT NOT NULL CHECK (subtotal_minor_units >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    PRIMARY KEY (receipt_id, line_index),
    CONSTRAINT ck_receipt_line_unit CHECK (unit IN ('UN', 'KG')),
    CONSTRAINT ck_receipt_line_currency CHECK (currency = 'BRL')
);

CREATE INDEX ix_shopping_lists_owner_status_updated
    ON shopping_lists (owner_id, status, updated_at DESC);
CREATE INDEX ix_shopping_list_members_actor
    ON shopping_list_members (actor_kind, actor_id, list_id);
CREATE INDEX ix_shopping_list_items_list_position
    ON shopping_list_items (list_id, position);
CREATE INDEX ix_receipt_participants_actor_completed
    ON receipt_participants (actor_kind, actor_id, receipt_id);
CREATE INDEX ix_receipts_completed_at
    ON receipts (completed_at DESC, id);
