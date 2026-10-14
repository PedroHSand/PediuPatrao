package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Cliente novoCliente(Cliente cliente) {
        return salvar(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        clienteRepository.findById(id).ifPresent(cliente -> {
            clienteRepository.deleteById(id);
            auditoriaService.registrar(TipoOperacao.EXCLUSAO_CLIENTE, "Cliente", id,
                    List.of(new AlteracaoCampo("nome", cliente.getNome(), "")), null);
        });
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        // Se o cliente não tem ID (novo cliente), salva como novo
        if (cliente.getId() == null) {
            Cliente novo = clienteRepository.save(cliente);  // Cria um novo cliente
            auditoriaService.registrar(TipoOperacao.CRIACAO_CLIENTE, "Cliente", novo.getId(),
                    levantarDiferencas(new Cliente(), novo), null);
            return novo;
        } // Se já tem ID (cliente existente), atualiza
        else {
            // Verifica se o cliente existe antes de atualizar
            Optional<Cliente> versaoAnterior = clienteRepository.findById(cliente.getId());
            if (versaoAnterior.isPresent()) {
                List<AlteracaoCampo> alteracoes = levantarDiferencas(versaoAnterior.get(), cliente);
                Cliente salvo = clienteRepository.save(cliente);  // Atualiza o cliente existente

                if (!alteracoes.isEmpty()) {
                    auditoriaService.registrar(TipoOperacao.ALTERACAO_CLIENTE, "Cliente", salvo.getId(), alteracoes, null);
                }
                return salvo;
            } else {
                throw new IllegalArgumentException("Cliente não encontrado para atualização.");
            }
        }
    }

    private List<AlteracaoCampo> levantarDiferencas(Cliente antes, Cliente depois) {
        List<AlteracaoCampo> alteracoes = new ArrayList<>();

        anotarSeDiferente(alteracoes, "nome", antes.getNome(), depois.getNome());
        anotarSeDiferente(alteracoes, "telefone", antes.getTelefone(), depois.getTelefone());
        anotarSeDiferente(alteracoes, "cep", antes.getCep(), depois.getCep());
        anotarSeDiferente(alteracoes, "logradouro", antes.getLogradouro(), depois.getLogradouro());
        anotarSeDiferente(alteracoes, "numero", antes.getNumero(), depois.getNumero());
        anotarSeDiferente(alteracoes, "complemento", antes.getComplemento(), depois.getComplemento());
        anotarSeDiferente(alteracoes, "bairro", antes.getBairro(), depois.getBairro());
        anotarSeDiferente(alteracoes, "cidade", antes.getCidade(), depois.getCidade());
        anotarSeDiferente(alteracoes, "estado", antes.getEstado(), depois.getEstado());
        anotarSeDiferente(alteracoes, "endereco", antes.getEndereco(), depois.getEndereco());

        return alteracoes;
    }

    private void anotarSeDiferente(List<AlteracaoCampo> alteracoes, String campo, String valorAntes, String valorDepois) {
        String antes = Objects.toString(valorAntes, "");
        String depois = Objects.toString(valorDepois, "");

        if (!antes.equals(depois)) {
            alteracoes.add(new AlteracaoCampo(campo, antes, depois));
        }
    }

}
