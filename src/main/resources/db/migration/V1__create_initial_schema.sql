CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    external_subject VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320),
    display_name VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE shopping_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    store_name VARCHAR(160),
    budget NUMERIC(12, 2),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_shopping_session_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT chk_shopping_session_name
        CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_shopping_session_budget
        CHECK (budget IS NULL OR budget >= 0),
    CONSTRAINT chk_shopping_session_status
        CHECK (status IN ('ACTIVE', 'COMPLETED')),
    CONSTRAINT chk_shopping_session_completion
        CHECK (
            (status = 'ACTIVE' AND completed_at IS NULL)
            OR
            (status = 'COMPLETED' AND completed_at IS NOT NULL)
        )
);

CREATE TABLE shopping_session_item (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    note TEXT,
    label_photo_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_shopping_session_item_session
        FOREIGN KEY (session_id) REFERENCES shopping_session(id) ON DELETE CASCADE,
    CONSTRAINT chk_shopping_session_item_name
        CHECK (length(trim(name)) > 0),
    CONSTRAINT chk_shopping_session_item_price
        CHECK (unit_price >= 0),
    CONSTRAINT chk_shopping_session_item_quantity
        CHECK (quantity > 0)
);

CREATE INDEX idx_shopping_session_user_status_created
    ON shopping_session (user_id, status, created_at DESC);

CREATE INDEX idx_shopping_session_user_completed
    ON shopping_session (user_id, completed_at DESC)
    WHERE status = 'COMPLETED';

CREATE INDEX idx_shopping_session_item_session
    ON shopping_session_item (session_id);

CREATE INDEX idx_shopping_session_item_session_created
    ON shopping_session_item (session_id, created_at DESC);

