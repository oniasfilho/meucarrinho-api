package app.meucarrinho.testfixtures.notifications;

import app.meucarrinho.application.notifications.port.DeliveryId;
import app.meucarrinho.application.notifications.port.Notification;
import app.meucarrinho.application.notifications.port.NotificationError;
import app.meucarrinho.application.notifications.port.EmailSender;
import app.meucarrinho.domain.shared.Result;
import app.meucarrinho.domain.shared.EmailAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RecordingEmailSender implements EmailSender {
    public record Sent(EmailAddress to, Notification notification, Locale locale) {}

    private final List<Sent> sent = new ArrayList<>();

    @Override
    public Result<DeliveryId, NotificationError> send(EmailAddress to, Notification notification, Locale locale) {
        sent.add(new Sent(to, notification, locale));
        return Result.ok(new DeliveryId("email-" + sent.size()));
    }

    public List<Sent> sent() {
        return List.copyOf(sent);
    }
}
