package app.meucarrinho.application.lists.port;

public enum PhotoContentType {
    JPEG("image/jpeg"),
    WEBP("image/webp");

    private final String mediaType;

    PhotoContentType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String mediaType() {
        return mediaType;
    }
}
