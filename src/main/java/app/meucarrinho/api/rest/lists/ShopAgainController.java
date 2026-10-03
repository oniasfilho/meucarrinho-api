package app.meucarrinho.api.rest.lists;

import static app.meucarrinho.api.rest.ApiErrors.unwrap;

import app.meucarrinho.api.rest.ApiErrors;
import app.meucarrinho.api.rest.CopyRequest;
import app.meucarrinho.api.rest.CurrentActor;
import app.meucarrinho.api.rest.receipts.ReceiptMapper;
import app.meucarrinho.application.lists.ShopAgain;
import app.meucarrinho.domain.shared.AccountId;
import app.meucarrinho.domain.shared.ListId;
import app.meucarrinho.domain.shared.ReceiptId;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ShopAgainController implements ShopAgainApi {
    private final CurrentActor caller;
    private final ShopAgain shopAgain;

    ShopAgainController(CurrentActor caller, ShopAgain shopAgain) {
        this.caller = caller;
        this.shopAgain = shopAgain;
    }

    @Override
    public ResponseEntity<ListResponse> shopAgain(UUID receiptId, @Nullable CopyRequest request) {
        AccountId me = caller.require();
        ReceiptId receipt = ReceiptMapper.receiptId(receiptId);
        Optional<ListId> newList = Optional.ofNullable(request).map(CopyRequest::id).map(ListMapper::listId);
        return ListMapper.created(unwrap(shopAgain.shopAgain(receipt, me, newList), ApiErrors::of));
    }
}
