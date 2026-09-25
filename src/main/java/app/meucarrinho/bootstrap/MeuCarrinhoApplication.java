package app.meucarrinho.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "app.meucarrinho")
public class MeuCarrinhoApplication {
    static void main(String[] args) {
        SpringApplication.run(MeuCarrinhoApplication.class, args);
    }
}
