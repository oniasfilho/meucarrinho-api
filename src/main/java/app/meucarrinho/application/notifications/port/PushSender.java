package app.meucarrinho.application.notifications.port;

import app.meucarrinho.domain.shared.Result;
import java.util.Locale;

public interface PushSender {
    Result<DeliveryId, NotificationError> send(PushToken to, Notification notification, Locale locale);
}
