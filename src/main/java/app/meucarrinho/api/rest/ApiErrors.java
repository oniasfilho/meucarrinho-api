package app.meucarrinho.api.rest;

import app.meucarrinho.application.accounts.AccountError;
import app.meucarrinho.application.receipts.ReceiptError;
import app.meucarrinho.domain.list.ListError;
import app.meucarrinho.domain.shared.Result;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.http.HttpStatus;

/** The one table from use-case errors to HTTP status and stable code (spec §13); the switches are exhaustive. */
public final class ApiErrors {
    private ApiErrors() {}

    public static <T, E> T unwrap(Result<T, E> result, Function<E, ApiProblem> problem) {
        return switch (result) {
            case Result.Ok<T, E> ok -> ok.value();
            case Result.Err<T, E> err -> throw problem.apply(err.error());
        };
    }

    public static ApiProblem of(ListError error) {
        return switch (error) {
            case ListError.ListNotFound e ->
                    new ApiProblem(HttpStatus.NOT_FOUND, "LIST_NOT_FOUND", "The list does not exist.");
            case ListError.NotAMember e ->
                    new ApiProblem(HttpStatus.FORBIDDEN, "NOT_A_MEMBER", "You are not a member of this list.");
            case ListError.OwnerOnly e ->
                    new ApiProblem(HttpStatus.FORBIDDEN, "OWNER_ONLY", "Only the list owner may do this.");
            case ListError.ListNotActive e -> new ApiProblem(HttpStatus.CONFLICT, "LIST_NOT_ACTIVE",
                    "The list is no longer active.", Map.of("status", e.status().name()));
            case ListError.ItemNotFound e ->
                    new ApiProblem(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "The item does not exist.");
            case ListError.ItemDeleted e ->
                    new ApiProblem(HttpStatus.CONFLICT, "ITEM_DELETED", "The item was removed.");
            case ListError.NothingPicked e ->
                    new ApiProblem(HttpStatus.CONFLICT, "NOTHING_PICKED", "Pick at least one item before finishing.");
            case ListError.LimitExceeded e -> new ApiProblem(HttpStatus.UNPROCESSABLE_CONTENT, "LIMIT_EXCEEDED",
                    "A limit was reached.", Map.of("limit", e.limit(), "max", e.max()));
            case ListError.RestoreWindowExpired e ->
                    new ApiProblem(HttpStatus.GONE, "RESTORE_WINDOW_EXPIRED", "It is too late to undo this.");
            case ListError.ListIdTaken e ->
                    new ApiProblem(HttpStatus.CONFLICT, "LIST_ID_TAKEN", "That list ID is already in use.");
            case ListError.VersionConflict e -> new ApiProblem(HttpStatus.PRECONDITION_FAILED, "VERSION_CONFLICT",
                    "The list changed since you loaded it.", Map.of("currentRevision", e.currentRevision()));
            case ListError.InvalidItem e -> new ApiProblem(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED",
                    "The request has invalid fields.", Map.of("errors", List.of(Map.of("field", "text",
                            "code", e.code(), "message", "This text is not an item."))));
            case ListError.ReceiptNotFound e ->
                    new ApiProblem(HttpStatus.NOT_FOUND, "RECEIPT_NOT_FOUND", "The receipt does not exist.");
            case ListError.NoPastPurchase e ->
                    new ApiProblem(HttpStatus.NOT_FOUND, "NO_PAST_PURCHASE", "There is no finished purchase yet.");
        };
    }

    public static ApiProblem of(AccountError error) {
        return switch (error) {
            case AccountError.AccountNotFound e ->
                    new ApiProblem(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "There is no account yet.");
            case AccountError.UnknownIdentity e ->
                    new ApiProblem(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "There is no account yet.");
            case AccountError.AccountDeleted e ->
                    new ApiProblem(HttpStatus.GONE, "ACCOUNT_DELETED", "This account was deleted.");
        };
    }

    public static ApiProblem of(ReceiptError error) {
        return switch (error) {
            case ReceiptError.ReceiptNotFound e ->
                    new ApiProblem(HttpStatus.NOT_FOUND, "RECEIPT_NOT_FOUND", "The receipt does not exist.");
        };
    }
}
