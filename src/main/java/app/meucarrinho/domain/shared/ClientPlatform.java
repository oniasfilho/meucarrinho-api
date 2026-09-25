package app.meucarrinho.domain.shared;

public enum ClientPlatform {
    IOS,
    ANDROID,
    WEB;

    public boolean isMobile() {
        return this != WEB;
    }
}
