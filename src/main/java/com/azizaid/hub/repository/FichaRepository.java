package com.azizaid.hub.repository;

import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.enums.StatusFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FichaRepository extends JpaRepository<Ficha, Long> {

    long countByStatus(StatusFicha status);

    long countByStatusAndDataAtualizacaoBefore(StatusFicha status, LocalDateTime limite);

    long countByDataCriacaoBetween(LocalDateTime inicio, LocalDateTime fim);

    boolean existsByCpf(String cpf);

    @Query("SELECT COUNT(DISTINCT f.id) FROM Ficha f JOIN f.encaminhamentos e")
    long contarFichasComEncaminhamento();

    @Query("SELECT COUNT(DISTINCT f.id) FROM Ficha f JOIN f.encaminhamentos e WHERE f.historicoAtendimento IS NOT NULL")
    long contarFichasComEncaminhamentoEAtendimento();

    @Query(value = "SELECT MIN(data_criacao) FROM ficha", nativeQuery = true)
    Optional<LocalDateTime> findMinDataCriacao();

    @Query(value = "SELECT date_trunc(:unidade, data_criacao) AS periodo, COUNT(*) AS total "
            + "FROM ficha GROUP BY periodo ORDER BY periodo", nativeQuery = true)
    List<Object[]> progressaoCadastros(@Param("unidade") String unidade);

    @Query("""
            SELECT f FROM Ficha f
            WHERE :termo IS NULL OR :termo = ''
               OR LOWER(f.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR f.cpf LIKE CONCAT('%', :termo, '%')
               OR LOWER(f.codigoFicha) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR f.numeroCaso LIKE CONCAT('%', :termo, '%')
            ORDER BY f.dataAtualizacao DESC, f.id DESC
            """)
    List<Ficha> buscar(@Param("termo") String termo);

    // Filtros de GET /api/acompanhamentos, compartilhados pela página e pelo COUNT. Todo parâmetro
    // opcional leva CAST: com valor nulo, o Postgres não consegue deduzir o tipo de ":x IS NULL".
    // A busca segue a regra de buscar(). O serviço é o do encaminhamento mais recente, com a mesma
    // ordem de EncaminhamentoRepository.findByFichaIdOrderByDataEncaminhamentoDescIdDesc (DESC põe
    // data nula primeiro; não acrescentar NULLS LAST). ENCERRADO é o nome antigo de ARQUIVADO (ver
    // StatusFichaConverter).
    String FILTROS_ACOMPANHAMENTOS = """
            (CAST(:termo AS varchar) IS NULL
                OR LOWER(f.nome) LIKE LOWER('%' || CAST(:termo AS varchar) || '%')
                OR f.cpf LIKE '%' || CAST(:termo AS varchar) || '%'
                OR LOWER(f.codigo_ficha) LIKE LOWER('%' || CAST(:termo AS varchar) || '%')
                OR f.numero_caso LIKE '%' || CAST(:termo AS varchar) || '%')
            AND (CAST(:status AS varchar) IS NULL
                OR f.status = CAST(:status AS varchar)
                OR (CAST(:status AS varchar) = 'ARQUIVADO' AND f.status = 'ENCERRADO'))
            AND (CAST(:criadoPorId AS bigint) IS NULL OR f.criado_por_id = CAST(:criadoPorId AS bigint))
            AND (CAST(:inicioAtualizacao AS timestamp) IS NULL
                OR f.data_atualizacao >= CAST(:inicioAtualizacao AS timestamp))
            AND (CAST(:fimAtualizacao AS timestamp) IS NULL
                OR f.data_atualizacao < CAST(:fimAtualizacao AS timestamp))
            AND (CAST(:inicioCriacao AS timestamp) IS NULL
                OR f.data_criacao >= CAST(:inicioCriacao AS timestamp))
            AND (CAST(:fimCriacao AS timestamp) IS NULL
                OR f.data_criacao < CAST(:fimCriacao AS timestamp))
            AND (CAST(:tipoId AS bigint) IS NULL OR EXISTS (
                SELECT 1 FROM ficha_tipo_acompanhamento fta
                WHERE fta.ficha_id = f.id AND fta.tipo_acompanhamento_id = CAST(:tipoId AS bigint)))
            AND (CAST(:servicoId AS bigint) IS NULL OR (
                SELECT e.servico_id FROM encaminhamento e
                WHERE e.ficha_id = f.id
                ORDER BY e.data_encaminhamento DESC, e.id DESC
                LIMIT 1) = CAST(:servicoId AS bigint))
            """;

    // Aliases entre aspas: sem elas o Postgres devolve tudo em minúsculas e a projeção não casa.
    @Query(value = """
            SELECT f.id AS "id", f.numero_caso AS "numeroCaso", f.codigo_ficha AS "codigoFicha",
                   f.nome AS "nome", f.cpf AS "cpf", f.status AS "status",
                   f.data_atualizacao AS "dataAtualizacao", f.data_criacao AS "dataCriacao",
                   f.criado_por_id AS "criadoPorId",
                   ult.servico_id AS "servicoId", s.nome AS "servicoNome"
            FROM ficha f
            LEFT JOIN LATERAL (
                SELECT e.servico_id FROM encaminhamento e
                WHERE e.ficha_id = f.id
                ORDER BY e.data_encaminhamento DESC, e.id DESC
                LIMIT 1
            ) ult ON true
            LEFT JOIN servico s ON s.id = ult.servico_id
            WHERE """ + FILTROS_ACOMPANHAMENTOS + """
            ORDER BY f.data_atualizacao DESC, f.id DESC
            LIMIT :limite OFFSET :offset
            """, nativeQuery = true)
    List<AcompanhamentoLinha> listarAcompanhamentos(@Param("termo") String termo,
                                                    @Param("status") String status,
                                                    @Param("criadoPorId") Long criadoPorId,
                                                    @Param("inicioAtualizacao") LocalDateTime inicioAtualizacao,
                                                    @Param("fimAtualizacao") LocalDateTime fimAtualizacao,
                                                    @Param("inicioCriacao") LocalDateTime inicioCriacao,
                                                    @Param("fimCriacao") LocalDateTime fimCriacao,
                                                    @Param("tipoId") Long tipoId,
                                                    @Param("servicoId") Long servicoId,
                                                    @Param("limite") int limite,
                                                    @Param("offset") long offset);

    @Query(value = "SELECT COUNT(*) FROM ficha f WHERE " + FILTROS_ACOMPANHAMENTOS, nativeQuery = true)
    long contarAcompanhamentos(@Param("termo") String termo,
                               @Param("status") String status,
                               @Param("criadoPorId") Long criadoPorId,
                               @Param("inicioAtualizacao") LocalDateTime inicioAtualizacao,
                               @Param("fimAtualizacao") LocalDateTime fimAtualizacao,
                               @Param("inicioCriacao") LocalDateTime inicioCriacao,
                               @Param("fimCriacao") LocalDateTime fimCriacao,
                               @Param("tipoId") Long tipoId,
                               @Param("servicoId") Long servicoId);

    // Tipos das fichas de uma página, numa consulta só. Cada linha: [fichaId (Long), TipoAcompanhamento].
    @Query("SELECT f.id, t FROM Ficha f JOIN f.tiposAcompanhamento t WHERE f.id IN :ids ORDER BY t.nome, t.id")
    List<Object[]> buscarTiposAcompanhamentoDasFichas(@Param("ids") Collection<Long> ids);

    interface AcompanhamentoLinha {
        Long getId();

        String getNumeroCaso();

        String getCodigoFicha();

        String getNome();

        String getCpf();

        String getStatus();

        LocalDateTime getDataAtualizacao();

        LocalDateTime getDataCriacao();

        Long getCriadoPorId();

        Long getServicoId();

        String getServicoNome();
    }

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo_ficha FROM 3) AS INTEGER)), 0) "
            + "FROM ficha WHERE codigo_ficha LIKE 'F-%'", nativeQuery = true)
    int buscarMaiorSequencialCodigo();
}
