package app.meucarrinho.testfixtures.receipts;

import app.meucarrinho.application.receipts.port.ReceiptRepository;

class InMemoryReceiptRepositoryTest extends ReceiptRepositoryContract {
    @Override
    protected ReceiptRepository createRepository() {
        return new InMemoryReceiptRepository();
    }
}
