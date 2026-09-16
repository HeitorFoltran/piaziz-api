package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.PontoSerieDTO;
import com.azizaid.hub.dto.response.RelatorioAnaliticoDTO;
import com.azizaid.hub.repository.EncaminhamentoRepository;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.repository.HistoricoAtendimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RelatorioAnaliticoService {

    private static final Set<String> GRANULARIDADES_VALIDAS = Set.of("dia", "semana", "mes");
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("yyyy-MM");

    private final FichaRepository fichaRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final HistoricoAtendimentoRepository historicoAtendimentoRepository;

    public RelatorioAnaliticoService(FichaRepository fichaRepository,
                                     EncaminhamentoRepository encaminhamentoRepository,
                                     HistoricoAtendimentoRepository historicoAtendimentoRepository) {
        this.fichaRepository = fichaRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.historicoAtendimentoRepository = historicoAtendimentoRepository;
    }

    @Transactional(readOnly = true)
    public RelatorioAnaliticoDTO gerar(String granularidade) {
        String granularidadePt = normalizarGranularidade(granularidade);
        String unidadePostgres = mapearParaUnidadePostgres(granularidadePt);
        DateTimeFormatter formato = granularidadePt.equals("mes") ? FORMATO_MES : FORMATO_DIA;

        long totalCadastros = fichaRepository.count();
        long totalEncaminhamentos = encaminhamentoRepository.count();
        long fichasComEncaminhamento = fichaRepository.contarFichasComEncaminhamento();
        long fichasComEncaminhamentoEAtendimento = fichaRepository.contarFichasComEncaminhamentoEAtendimento();

        double percentualCadastrosEncaminhados = totalCadastros == 0
                ? 0.0
                : fichasComEncaminhamento * 100.0 / totalCadastros;
        double taxaConversaoEncaminhamentoAtendimento = fichasComEncaminhamento == 0
                ? 0.0
                : fichasComEncaminhamentoEAtendimento * 100.0 / fichasComEncaminhamento;

        Double tempoMedioCadastroAtendimentoDias = historicoAtendimentoRepository
                .calcularTempoMedioCadastroAtendimentoDias();

        double[] mediasCadastros = calcularMedias(fichaRepository.findMinDataCriacao()
                .map(LocalDateTime::toLocalDate), totalCadastros);
        double[] mediasEncaminhamentos = calcularMedias(encaminhamentoRepository.findMinDataEncaminhamento(),
                totalEncaminhamentos);

        return new RelatorioAnaliticoDTO(
                totalCadastros,
                totalEncaminhamentos,
                percentualCadastrosEncaminhados,
                taxaConversaoEncaminhamentoAtendimento,
                tempoMedioCadastroAtendimentoDias,
                mediasCadastros[0],
                mediasCadastros[1],
                mediasCadastros[2],
                mediasEncaminhamentos[0],
                mediasEncaminhamentos[1],
                mediasEncaminhamentos[2],
                mapearSerie(fichaRepository.progressaoCadastros(unidadePostgres), formato),
                mapearSerie(encaminhamentoRepository.progressaoEncaminhamentos(unidadePostgres), formato),
                mapearSerie(historicoAtendimentoRepository.progressaoAtendimentos(unidadePostgres), formato)
        );
    }

    private String normalizarGranularidade(String granularidade) {
        if (granularidade == null || granularidade.isBlank()) {
            return "mes";
        }
        String normalizada = granularidade.trim().toLowerCase();
        if (!GRANULARIDADES_VALIDAS.contains(normalizada)) {
            throw new IllegalArgumentException(
                    "granularidade inválida: use 'dia', 'semana' ou 'mes'");
        }
        return normalizada;
    }

    private String mapearParaUnidadePostgres(String granularidadePt) {
        return switch (granularidadePt) {
            case "dia" -> "day";
            case "semana" -> "week";
            default -> "month";
        };
    }

    /** Retorna [porMes, porSemana, porDia]. */
    private double[] calcularMedias(Optional<LocalDate> dataMinima, long total) {
        if (dataMinima.isEmpty()) {
            return new double[]{0.0, 0.0, 0.0};
        }
        LocalDate hoje = LocalDate.now();
        LocalDate minima = dataMinima.get();
        long meses = Math.max(1, ChronoUnit.MONTHS.between(minima, hoje));
        long semanas = Math.max(1, ChronoUnit.WEEKS.between(minima, hoje));
        long dias = Math.max(1, ChronoUnit.DAYS.between(minima, hoje));
        return new double[]{total / (double) meses, total / (double) semanas, total / (double) dias};
    }

    private List<PontoSerieDTO> mapearSerie(List<Object[]> linhas, DateTimeFormatter formato) {
        return linhas.stream()
                .map(linha -> new PontoSerieDTO(formatarPeriodo(linha[0], formato), (Long) linha[1]))
                .toList();
    }

    private String formatarPeriodo(Object periodo, DateTimeFormatter formato) {
        LocalDateTime dataHora = switch (periodo) {
            case Timestamp timestamp -> timestamp.toLocalDateTime();
            case Instant instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            case LocalDateTime localDateTime -> localDateTime;
            default -> throw new IllegalStateException(
                    "Tipo de período inesperado: " + periodo.getClass());
        };
        return dataHora.format(formato);
    }
}
