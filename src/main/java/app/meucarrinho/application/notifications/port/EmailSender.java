package app.meucarrinho.application.notifications.port;

import app.meucarrinho.domain.shared.EmailAddress;
import app.meucarrinho.domain.shared.Result;
import java.util.Locale;

public interface EmailSender {
    Result<DeliveryId, NotificationError> send(EmailAddress to, Notification notification, Locale locale);
}
