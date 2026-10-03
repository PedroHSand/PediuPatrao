package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo) {
        return args -> {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            if (repo.findByUsername("admin").isEmpty()) {

                Usuario user = new Usuario();
                user.setUsername("admin");
                user.setPassword(encoder.encode("teste123"));
                user.setPerfil(Perfil.ADMIN);

                repo.save(user);

                System.out.println("Usuário admin criado!");
            }

            if (repo.findByUsername("gerente01").isEmpty()) {

                Usuario gerente = new Usuario();
                gerente.setUsername("gerente01");
                gerente.setPassword(encoder.encode("teste123"));
                gerente.setPerfil(Perfil.GERENTE);

                repo.save(gerente);

                System.out.println("Usuário gerente01 criado!");
            }

            if (repo.findByUsername("atendente01").isEmpty()) {

                Usuario atendente = new Usuario();
                atendente.setUsername("atendente01");
                atendente.setPassword(encoder.encode("teste123"));
                atendente.setPerfil(Perfil.ATENDENTE);

                repo.save(atendente);

                System.out.println("Usuário atendente01 criado!");
            }
        };
    }
}