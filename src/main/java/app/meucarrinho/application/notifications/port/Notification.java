package app.meucarrinho.application.notifications.port;

import app.meucarrinho.domain.shared.DisplayName;
import app.meucarrinho.domain.shared.ListName;
import app.meucarrinho.domain.shared.ReceiptId;

public sealed interface Notification {
    record ListInvitation(ListName list, DisplayName inviter, ShareLink link) implements Notification {}

    record ReceiptCopy(ReceiptId receipt) implements Notification {}

    record Welcome(DisplayName name) implements Notification {}
}
