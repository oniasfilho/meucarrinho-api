package app.meucarrinho.domain.entitlement;

public record PlanId(String value) {
    public PlanId {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Plan id cannot be blank");
        }
    }
}
