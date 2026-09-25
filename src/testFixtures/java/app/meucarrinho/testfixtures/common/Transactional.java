package app.meucarrinho.testfixtures.common;

public interface Transactional {
    Object checkpoint();

    void rollbackTo(Object checkpoint);
}
