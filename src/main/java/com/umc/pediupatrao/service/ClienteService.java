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
                    compararCampos(new Cliente(), novo), null);
            return novo;
        } // Se já tem ID (cliente existente), atualiza
        else {
            // Verifica se o cliente existe antes de atualizar
            Optional<Cliente> antigo = clienteRepository.findById(cliente.getId());
            if (antigo.isPresent()) {
                List<AlteracaoCampo> alteracoes = compararCampos(antigo.get(), cliente);
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

    private List<AlteracaoCampo> compararCampos(Cliente antes, Cliente depois) {
        List<AlteracaoCampo> alteracoes = new ArrayList<>();

        adicionarSeMudou(alteracoes, "nome", antes.getNome(), depois.getNome());
        adicionarSeMudou(alteracoes, "telefone", antes.getTelefone(), depois.getTelefone());
        adicionarSeMudou(alteracoes, "cep", antes.getCep(), depois.getCep());
        adicionarSeMudou(alteracoes, "logradouro", antes.getLogradouro(), depois.getLogradouro());
        adicionarSeMudou(alteracoes, "numero", antes.getNumero(), depois.getNumero());
        adicionarSeMudou(alteracoes, "complemento", antes.getComplemento(), depois.getComplemento());
        adicionarSeMudou(alteracoes, "bairro", antes.getBairro(), depois.getBairro());
        adicionarSeMudou(alteracoes, "cidade", antes.getCidade(), depois.getCidade());
        adicionarSeMudou(alteracoes, "estado", antes.getEstado(), depois.getEstado());
        adicionarSeMudou(alteracoes, "endereco", antes.getEndereco(), depois.getEndereco());

        return alteracoes;
    }

    // null e texto vazio contam como iguais, porque o formulário manda "" onde o banco tem null
    private void adicionarSeMudou(List<AlteracaoCampo> alteracoes, String campo, String valorAntes, String valorDepois) {
        String antes = Objects.toString(valorAntes, "");
        String depois = Objects.toString(valorDepois, "");

        if (!antes.equals(depois)) {
            alteracoes.add(new AlteracaoCampo(campo, antes, depois));
        }
    }

}
