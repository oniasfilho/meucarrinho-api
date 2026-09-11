package br.com.oniasfilho.meucarrinho.user;

import br.com.oniasfilho.meucarrinho.user.dto.AccountStatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private final AccountStatsService accountStatsService;

    public AccountController(AccountStatsService accountStatsService) {
        this.accountStatsService = accountStatsService;
    }

    @GetMapping("/stats")
    public AccountStatsResponse getStats() {
        return accountStatsService.getStats();
    }
}
