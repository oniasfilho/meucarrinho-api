package app.meucarrinho.domain.list;

import app.meucarrinho.domain.shared.ActorRef;
import app.meucarrinho.domain.shared.Result;
import org.jspecify.annotations.Nullable;

public final class ListAccessPolicy {
    public enum Role {
        OWNER,
        ACCOUNT_MEMBER,
        GUEST_MEMBER,
        NONE
    }

    public enum Action {
        VIEW,

        EDIT_ITEMS,

        FINISH,

        MANAGE
    }

    private ListAccessPolicy() {}

    public static Role roleOf(ShoppingList list, ActorRef actor) {
        if (actor instanceof ActorRef.AccountActor(var id) && id.equals(list.ownerId())) {
            return Role.OWNER;
        }
        return list.members().stream()
                .filter(member -> member.actor().equals(actor))
                .findFirst()
                .map(member -> member.isGuest() ? Role.GUEST_MEMBER : Role.ACCOUNT_MEMBER)
                .orElse(Role.NONE);
    }

    public static Result<@Nullable Void, ListError> require(ShoppingList list, ActorRef actor, Action action) {
        Role role = roleOf(list, actor);
        if (role == Role.NONE) {
            return Result.err(new ListError.ListNotFound(list.id()));
        }
        if (action == Action.MANAGE && role != Role.OWNER) {
            return Result.err(new ListError.OwnerOnly(list.id()));
        }
        return Result.ok();
    }
}
