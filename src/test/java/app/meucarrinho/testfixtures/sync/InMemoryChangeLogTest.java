package app.meucarrinho.testfixtures.sync;

import app.meucarrinho.application.sync.port.ChangeLog;

class InMemoryChangeLogTest extends ChangeLogContract {
    @Override
    protected ChangeLog createChangeLog() {
        return new InMemoryChangeLog();
    }
}
