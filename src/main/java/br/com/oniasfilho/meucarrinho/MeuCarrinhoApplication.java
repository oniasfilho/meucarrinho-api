package br.com.oniasfilho.meucarrinho;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MeuCarrinhoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeuCarrinhoApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

