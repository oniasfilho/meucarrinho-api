package br.com.oniasfilho.meucarrinho.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    static final String DEVELOPMENT_SUBJECT = "local-demo-user";

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentUser() {
        return appUserRepository.findByExternalSubject(DEVELOPMENT_SUBJECT)
            .orElseThrow(() -> new IllegalStateException(
                "Development user 'local-demo-user' is missing. Check the Flyway seed migration."
            ));
    }
}

