package app.meucarrinho.testfixtures.notifications;

import app.meucarrinho.application.notifications.port.DeliveryId;
import app.meucarrinho.application.notifications.port.Notification;
import app.meucarrinho.application.notifications.port.NotificationError;
import app.meucarrinho.application.notifications.port.PushSender;
import app.meucarrinho.application.notifications.port.PushToken;
import app.meucarrinho.domain.shared.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RecordingPushSender implements PushSender {
    public record Sent(PushToken to, Notification notification, Locale locale) {}

    private final List<Sent> sent = new ArrayList<>();

    @Override
    public Result<DeliveryId, NotificationError> send(PushToken to, Notification notification, Locale locale) {
        sent.add(new Sent(to, notification, locale));
        return Result.ok(new DeliveryId("push-" + sent.size()));
    }

    public List<Sent> sent() {
        return List.copyOf(sent);
    }
}
