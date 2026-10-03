package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    public void salvarUsuario(Usuario usuario) {
        if (usuario.getPerfil() == null) {
            throw new IllegalStateException("Escolha um perfil para o usuário.");
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID
    public void deletarUsuario(String id, String usernameLogado) {
        usuarioRepository.findById(id).ifPresent(alvo -> {
            if (alvo.getUsername().equals(usernameLogado)) {
                throw new IllegalStateException("Você não pode excluir o seu próprio usuário.");
            }
            usuarioRepository.deleteById(id);
        });
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário usuários
    public void atualizarUsuario(String id, Usuario usuarioAtualizado, String usernameLogado) {
        usuarioRepository.findById(id).ifPresent(usuarioExistente -> {
            if (usuarioExistente.getUsername().equals(usernameLogado)
                    && usuarioExistente.getPerfil() != usuarioAtualizado.getPerfil()) {
                throw new IllegalStateException("Você não pode alterar o seu próprio perfil.");
            }

            if (usuarioAtualizado.getPerfil() == null) {
                throw new IllegalStateException("Escolha um perfil para o usuário.");
            }

            usuarioExistente.setUsername(usuarioAtualizado.getUsername());
            usuarioExistente.setPerfil(usuarioAtualizado.getPerfil());

            // Só atualiza a senha se uma nova foi informada
            if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
                usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
            }

            usuarioRepository.save(usuarioExistente);
        });
    }
}
