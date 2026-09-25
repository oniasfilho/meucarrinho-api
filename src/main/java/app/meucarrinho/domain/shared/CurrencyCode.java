package app.meucarrinho.domain.shared;

public enum CurrencyCode {
    BRL(2);

    private final int minorDigits;

    CurrencyCode(int minorDigits) {
        this.minorDigits = minorDigits;
    }

    public int minorDigits() {
        return minorDigits;
    }
}
