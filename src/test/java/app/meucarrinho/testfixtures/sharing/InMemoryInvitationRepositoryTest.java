package app.meucarrinho.testfixtures.sharing;

import app.meucarrinho.application.sharing.port.InvitationRepository;

class InMemoryInvitationRepositoryTest extends InvitationRepositoryContract {
    @Override
    protected InvitationRepository createRepository() {
        return new InMemoryInvitationRepository();
    }
}
