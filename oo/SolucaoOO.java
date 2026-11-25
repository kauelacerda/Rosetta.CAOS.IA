import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SolucaoOO {

    // =========================================================================
    // MODELO DE DOMÍNIO (DIGA, NÃO PERGUNTE)
    // =========================================================================

    static final class Horario {
        private final String dia;
        private final String turno;

        Horario(String dia, String turno) {
            this.dia = dia;
            this.turno = turno;
        }

        boolean conflitaCom(Horario outro) {
            return this.dia.equals(outro.dia) && this.turno.equals(outro.turno);
        }
    }

    static final class Disciplina implements Comparable<Disciplina> {
        private final String codigo;
        private final String nome;
        private final int cargaHoraria;
        private final List<String> prerequisitos;
        private final List<Horario> horarios;

        Disciplina(String codigo, String nome, int cargaHoraria, List<String> prerequisitos) {
            this.codigo = codigo;
            this.nome = nome;
            this.cargaHoraria = cargaHoraria;
            this.prerequisitos = new ArrayList<>(prerequisitos);
            Collections.sort(this.prerequisitos);
            this.horarios = new ArrayList<>();
        }

        void registrarEm(Map<String, Disciplina> indice) {
            indice.put(this.codigo, this);
        }

        void definirHorariosOfertados(List<Horario> novosHorarios) {
            this.horarios.clear();
            this.horarios.addAll(novosHorarios);
        }

        boolean foiCursadaEm(HistoricoAluno historico) {
            return historico.foiAprovada(this.codigo);
        }

        void adicionarCodigoEm(List<String> destino) {
            destino.add(this.codigo);
        }

        @Override
        public int compareTo(Disciplina outra) {
            return this.codigo.compareTo(outra.codigo);
        }

        // Pergunta 1: a propria disciplina conduz a travessia pelos seus pre-requisitos
        boolean explorarDependencias(MatrizCurricular matriz, EstadoBuscaCiclo estado) {
            estado.entrar(this.codigo);

            for (String req : this.prerequisitos) {
                if (matriz.visitarPrerequisito(req, estado)) {
                    return true;
                }
            }

            estado.sair();
            return false;
        }

        // Perguntas 2 e 3: a regra sobre pre-requisitos pertence a quem guarda os dados deles
        boolean estaLiberadaPara(HistoricoAluno historico) {
            for (String req : this.prerequisitos) {
                if (!historico.foiAprovada(req)) {
                    return false;
                }
            }
            return true;
        }

        BloqueioDisciplina avaliarBloqueio(HistoricoAluno historico) {
            List<String> faltantes = new ArrayList<>();
            for (String req : this.prerequisitos) {
                if (!historico.foiAprovada(req)) {
                    faltantes.add(req);
                }
            }
            Collections.sort(faltantes);
            return new BloqueioDisciplina(this.codigo, faltantes);
        }

        // Pergunta 4: decisoes encapsuladas sobre carga horaria e choque de horario
        int somarCargaHoraria(int acumulado) {
            return acumulado + this.cargaHoraria;
        }

        boolean conflitaHorarioCom(Disciplina outra) {
            for (Horario meuHorario : this.horarios) {
                if (outra.possuiConflitoComSlot(meuHorario)) {
                    return true;
                }
            }
            return false;
        }

        private boolean possuiConflitoComSlot(Horario slotExterno) {
            for (Horario meuHorario : this.horarios) {
                if (meuHorario.conflitaCom(slotExterno)) {
                    return true;
                }
            }
            return false;
        }
    }

    static final class BloqueioDisciplina implements Comparable<BloqueioDisciplina> {
        private final String codigoDisciplina;
        private final List<String> faltam;

        BloqueioDisciplina(String codigoDisciplina, List<String> faltam) {
            this.codigoDisciplina = codigoDisciplina;
            this.faltam = new ArrayList<>(faltam);
        }

        @Override
        public int compareTo(BloqueioDisciplina outro) {
            return this.codigoDisciplina.compareTo(outro.codigoDisciplina);
        }

        String paraJson(String indentacao) {
            StringBuilder sb = new StringBuilder();
            sb.append(indentacao).append("{\n");
            sb.append(indentacao).append("  \"codigo\": ").append(SerializadorJson.stringJson(codigoDisciplina)).append(",\n");
            sb.append(indentacao).append("  \"faltam\": ").append(SerializadorJson.listaStringsInline(faltam)).append("\n");
            sb.append(indentacao).append("}");
            return sb.toString();
        }
    }

    static final class HistoricoAluno {
        private final Set<String> aprovadas;
        private final int tetoCargaHoraria;

        HistoricoAluno(List<String> aprovadas, int tetoCargaHoraria) {
            this.aprovadas = new HashSet<>(aprovadas);
            this.tetoCargaHoraria = tetoCargaHoraria;
        }

        boolean foiAprovada(String codigoDisciplina) {
            return this.aprovadas.contains(codigoDisciplina);
        }

        Restricao criarRestricaoDeTeto() {
            return new TetoDeCargaHoraria(this.tetoCargaHoraria);
        }
    }

    static final class EstadoBuscaCiclo {
        private enum Situacao { NAO_VISITADO, NA_PILHA, CONCLUIDO }

        private final Map<String, Situacao> situacoes = new HashMap<>();
        private final List<String> caminhoAtual = new ArrayList<>();
        private List<String> cicloEncontrado;

        boolean estaNaPilha(String codigo) {
            return situacoes.getOrDefault(codigo, Situacao.NAO_VISITADO) == Situacao.NA_PILHA;
        }

        boolean jaConcluido(String codigo) {
            return situacoes.getOrDefault(codigo, Situacao.NAO_VISITADO) == Situacao.CONCLUIDO;
        }

        void entrar(String codigo) {
            situacoes.put(codigo, Situacao.NA_PILHA);
            caminhoAtual.add(codigo);
        }

        void sair() {
            String topo = caminhoAtual.remove(caminhoAtual.size() - 1);
            situacoes.put(topo, Situacao.CONCLUIDO);
        }

        void registrarCicloFechadoEm(String codigoAlvo) {
            int inicio = caminhoAtual.indexOf(codigoAlvo);
            List<String> ciclo = new ArrayList<>();
            for (int i = inicio; i < caminhoAtual.size(); i++) {
                ciclo.add(caminhoAtual.get(i));
            }
            ciclo.add(codigoAlvo);
            this.cicloEncontrado = ciclo;
        }

        boolean encontrouCiclo() {
            return this.cicloEncontrado != null;
        }

        List<String> exportarCiclo() {
            return new ArrayList<>(this.cicloEncontrado);
        }
    }

    static final class ItemOfertaEntrada {
        private final String codigo;
        private final List<Horario> horarios;

        ItemOfertaEntrada(String codigo, List<Horario> horarios) {
            this.codigo = codigo;
            this.horarios = horarios;
        }

        void vincularNaMatriz(
            Map<String, Disciplina> porCodigo,
            HistoricoAluno historico,
            List<Disciplina> pendentesDestino
        ) {
            Disciplina disc = porCodigo.get(this.codigo);
            if (disc != null) {
                disc.definirHorariosOfertados(this.horarios);
                if (!disc.foiCursadaEm(historico)) {
                    pendentesDestino.add(disc);
                }
            }
        }
    }

    static final class MatrizCurricular {
        private final Map<String, Disciplina> porCodigo = new HashMap<>();
        private final List<Disciplina> disciplinasOrdenadas = new ArrayList<>();

        MatrizCurricular(List<Disciplina> disciplinas) {
            for (Disciplina d : disciplinas) {
                d.registrarEm(this.porCodigo);
                this.disciplinasOrdenadas.add(d);
            }
            Collections.sort(this.disciplinasOrdenadas);
        }

        EstadoBuscaCiclo validarCiclos() {
            EstadoBuscaCiclo estado = new EstadoBuscaCiclo();
            List<String> codigoBuffer = new ArrayList<>(1);

            for (Disciplina disc : this.disciplinasOrdenadas) {
                codigoBuffer.clear();
                disc.adicionarCodigoEm(codigoBuffer);
                String cod = codigoBuffer.get(0);

                if (!estado.jaConcluido(cod)) {
                    if (disc.explorarDependencias(this, estado)) {
                        return estado;
                    }
                }
            }
            return estado;
        }

        boolean visitarPrerequisito(String codigoPrerequisito, EstadoBuscaCiclo estado) {
            Disciplina alvo = this.porCodigo.get(codigoPrerequisito);
            if (alvo == null) {
                return false;
            }

            if (estado.estaNaPilha(codigoPrerequisito)) {
                estado.registrarCicloFechadoEm(codigoPrerequisito);
                return true;
            }

            if (!estado.jaConcluido(codigoPrerequisito)) {
                return alvo.explorarDependencias(this, estado);
            }

            return false;
        }

        List<Disciplina> filtrarOfertaPendente(List<ItemOfertaEntrada> oferta, HistoricoAluno historico) {
            List<Disciplina> pendentes = new ArrayList<>();
            for (ItemOfertaEntrada item : oferta) {
                item.vincularNaMatriz(this.porCodigo, historico, pendentes);
            }
            Collections.sort(pendentes);
            return pendentes;
        }
    }

    // =========================================================================
    // POLIMORFISMO COM DESPACHO DINÂMICO PARA RESTRIÇÕES (PERGUNTA 4)
    // =========================================================================

    interface Restricao {
        boolean permiteAdicionar(GradeEmConstrucao gradeAtual, Disciplina candidata);
    }

    static final class TetoDeCargaHoraria implements Restricao {
        private final int tetoMaximo;

        TetoDeCargaHoraria(int tetoMaximo) {
            this.tetoMaximo = tetoMaximo;
        }

        @Override
        public boolean permiteAdicionar(GradeEmConstrucao gradeAtual, Disciplina candidata) {
            return gradeAtual.calcularCargaHorariaCom(candidata) <= this.tetoMaximo;
        }
    }

    static final class ConflitoDeHorario implements Restricao {
        @Override
        public boolean permiteAdicionar(GradeEmConstrucao gradeAtual, Disciplina candidata) {
            return !gradeAtual.possuiConflitoCom(candidata);
        }
    }

    static final class GradeEmConstrucao {
        private final List<Disciplina> escolhidas = new ArrayList<>();

        int tamanho() {
            return this.escolhidas.size();
        }

        int calcularCargaHorariaCom(Disciplina candidata) {
            int total = 0;
            for (Disciplina d : this.escolhidas) {
                total = d.somarCargaHoraria(total);
            }
            return candidata.somarCargaHoraria(total);
        }

        boolean possuiConflitoCom(Disciplina candidata) {
            for (Disciplina d : this.escolhidas) {
                if (d.conflitaHorarioCom(candidata)) {
                    return true;
                }
            }
            return false;
        }

        void adicionar(Disciplina disciplina) {
            this.escolhidas.add(disciplina);
        }

        void removerUltima() {
            this.escolhidas.remove(this.escolhidas.size() - 1);
        }

        GradeConsolidada consolidar() {
            List<Disciplina> ordenadas = new ArrayList<>(this.escolhidas);
            Collections.sort(ordenadas);
            List<String> codigos = new ArrayList<>(ordenadas.size());
            for (Disciplina d : ordenadas) {
                d.adicionarCodigoEm(codigos);
            }
            return new GradeConsolidada(codigos);
        }
    }

    static final class GradeConsolidada implements Comparable<GradeConsolidada> {
        private final List<String> codigos;

        GradeConsolidada(List<String> codigos) {
            this.codigos = codigos;
        }

        @Override
        public int compareTo(GradeConsolidada outra) {
            int limite = Math.min(this.codigos.size(), outra.codigos.size());
            for (int i = 0; i < limite; i++) {
                int cmp = this.codigos.get(i).compareTo(outra.codigos.get(i));
                if (cmp != 0) {
                    return cmp;
                }
            }
            return Integer.compare(this.codigos.size(), outra.codigos.size());
        }

        String paraJsonInline() {
            return SerializadorJson.listaStringsInline(this.codigos);
        }
    }

    static final class ColecaoGradesMaximas {
        private int melhorTamanho = 0;
        private final List<GradeConsolidada> grades = new ArrayList<>();

        boolean podeSuperarOuEmpatar(int tamanhoAtual, int restantes) {
            return tamanhoAtual + restantes >= this.melhorTamanho;
        }

        void registrar(GradeEmConstrucao candidata) {
            int tam = candidata.tamanho();
            if (tam > this.melhorTamanho) {
                this.melhorTamanho = tam;
                this.grades.clear();
                this.grades.add(candidata.consolidar());
            } else if (tam == this.melhorTamanho) {
                this.grades.add(candidata.consolidar());
            }
        }

        String paraJson(String indentacao) {
            Collections.sort(this.grades);
            if (this.grades.isEmpty()) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < this.grades.size(); i++) {
                sb.append(indentacao).append("  ").append(this.grades.get(i).paraJsonInline());
                if (i + 1 < this.grades.size()) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append(indentacao).append("]");
            return sb.toString();
        }
    }

    static final class ResultadoMatricula {
        private final List<String> ciclo;
        private final List<Disciplina> liberadas;
        private final List<BloqueioDisciplina> bloqueadas;
        private final ColecaoGradesMaximas gradesMaximas;

        private ResultadoMatricula(
            List<String> ciclo,
            List<Disciplina> liberadas,
            List<BloqueioDisciplina> bloqueadas,
            ColecaoGradesMaximas gradesMaximas
        ) {
            this.ciclo = ciclo;
            this.liberadas = liberadas;
            this.bloqueadas = bloqueadas;
            this.gradesMaximas = gradesMaximas;
        }

        static ResultadoMatricula comCiclo(List<String> ciclo) {
            return new ResultadoMatricula(ciclo, null, null, null);
        }

        static ResultadoMatricula valido(
            List<Disciplina> liberadas,
            List<BloqueioDisciplina> bloqueadas,
            ColecaoGradesMaximas gradesMaximas
        ) {
            return new ResultadoMatricula(null, liberadas, bloqueadas, gradesMaximas);
        }

        String paraJson() {
            if (this.ciclo != null) {
                return "{\n"
                    + "  \"ciclo\": " + SerializadorJson.listaStringsInline(this.ciclo) + ",\n"
                    + "  \"liberadas\": null,\n"
                    + "  \"bloqueadas\": null,\n"
                    + "  \"grades_maximas\": null\n"
                    + "}";
            }

            List<String> codigosLiberadas = new ArrayList<>(this.liberadas.size());
            for (Disciplina d : this.liberadas) {
                d.adicionarCodigoEm(codigosLiberadas);
            }

            String bloqueadasJson;
            if (this.bloqueadas.isEmpty()) {
                bloqueadasJson = "[]";
            } else {
                StringBuilder sb = new StringBuilder("[\n");
                for (int i = 0; i < this.bloqueadas.size(); i++) {
                    sb.append(this.bloqueadas.get(i).paraJson("    "));
                    if (i + 1 < this.bloqueadas.size()) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append("  ]");
                bloqueadasJson = sb.toString();
            }

            return "{\n"
                + "  \"ciclo\": null,\n"
                + "  \"liberadas\": " + SerializadorJson.listaStringsInline(codigosLiberadas) + ",\n"
                + "  \"bloqueadas\": " + bloqueadasJson + ",\n"
                + "  \"grades_maximas\": " + this.gradesMaximas.paraJson("  ") + "\n"
                + "}";
        }
    }

    static final class PlanejadorDeMatricula {
        private final MatrizCurricular matriz;
        private final HistoricoAluno historico;
        private final List<Restricao> restricoes;

        PlanejadorDeMatricula(MatrizCurricular matriz, HistoricoAluno historico) {
            this.matriz = matriz;
            this.historico = historico;
            this.restricoes = List.of(
                historico.criarRestricaoDeTeto(),
                new ConflitoDeHorario()
            );
        }

        ResultadoMatricula executar(List<ItemOfertaEntrada> oferta) {
            // 1. Validacao da matriz (roda primeiro; se houver ciclo, interrompe)
            EstadoBuscaCiclo estadoCiclo = this.matriz.validarCiclos();
            if (estadoCiclo.encontrouCiclo()) {
                return ResultadoMatricula.comCiclo(estadoCiclo.exportarCiclo());
            }

            // 2 e 3. Classificacao entre liberadas e bloqueadas
            List<Disciplina> pendentes = this.matriz.filtrarOfertaPendente(oferta, this.historico);
            List<Disciplina> liberadas = new ArrayList<>();
            List<BloqueioDisciplina> bloqueadas = new ArrayList<>();

            for (Disciplina disc : pendentes) {
                if (disc.estaLiberadaPara(this.historico)) {
                    liberadas.add(disc);
                } else {
                    bloqueadas.add(disc.avaliarBloqueio(this.historico));
                }
            }

            Collections.sort(liberadas);
            Collections.sort(bloqueadas);

            // 4. Busca da maior grade sem conflito e dentro do teto via despacho dinamico
            ColecaoGradesMaximas colecaoMaximas = new ColecaoGradesMaximas();
            GradeEmConstrucao gradeAtual = new GradeEmConstrucao();
            buscarSubconjuntos(0, liberadas, gradeAtual, colecaoMaximas);

            return ResultadoMatricula.valido(liberadas, bloqueadas, colecaoMaximas);
        }

        private void buscarSubconjuntos(
            int indice,
            List<Disciplina> liberadas,
            GradeEmConstrucao gradeAtual,
            ColecaoGradesMaximas colecaoMaximas
        ) {
            int restantes = liberadas.size() - indice;
            if (!colecaoMaximas.podeSuperarOuEmpatar(gradeAtual.tamanho(), restantes)) {
                return;
            }

            if (indice == liberadas.size()) {
                colecaoMaximas.registrar(gradeAtual);
                return;
            }

            Disciplina candidata = liberadas.get(indice);
            if (todasRestricoesPermitem(gradeAtual, candidata)) {
                gradeAtual.adicionar(candidata);
                buscarSubconjuntos(indice + 1, liberadas, gradeAtual, colecaoMaximas);
                gradeAtual.removerUltima();
            }

            buscarSubconjuntos(indice + 1, liberadas, gradeAtual, colecaoMaximas);
        }

        private boolean todasRestricoesPermitem(GradeEmConstrucao gradeAtual, Disciplina candidata) {
            // Polimorfismo com despacho dinamico sobre Restricao
            for (Restricao restricao : this.restricoes) {
                if (!restricao.permiteAdicionar(gradeAtual, candidata)) {
                    return false;
                }
            }
            return true;
        }
    }

    // =========================================================================
    // FRONTEIRA DE ENTRADA E SAÍDA JSON (SEM DEPENDÊNCIAS EXTERNAS)
    // =========================================================================

    static final class SerializadorJson {
        static String stringJson(String valor) {
            StringBuilder sb = new StringBuilder("\"");
            for (int i = 0; i < valor.length(); i++) {
                char c = valor.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\b' -> sb.append("\\b");
                    case '\f' -> sb.append("\\f");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append(c);
                }
            }
            sb.append('"');
            return sb.toString();
        }

        static String listaStringsInline(List<String> itens) {
            if (itens.isEmpty()) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < itens.size(); i++) {
                sb.append(stringJson(itens.get(i)));
                if (i + 1 < itens.size()) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    static final class LeitorJson {
        private final String texto;
        private int pos;

        private LeitorJson(String texto) {
            this.texto = texto;
            this.pos = 0;
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> lerObjeto(Path caminho) throws IOException {
            String conteudo = Files.readString(caminho, StandardCharsets.UTF_8);
            LeitorJson leitor = new LeitorJson(conteudo);
            return (Map<String, Object>) leitor.lerValor();
        }

        private Object lerValor() {
            pularEspacos();
            char c = texto.charAt(pos);
            if (c == '{') return lerObjetoInterno();
            if (c == '[') return lerArray();
            if (c == '"') return lerString();
            if (c == 't' || c == 'f') return lerBooleano();
            if (c == 'n') {
                pos += 4;
                return null;
            }
            return lerNumero();
        }

        private Map<String, Object> lerObjetoInterno() {
            Map<String, Object> mapa = new LinkedHashMap<>();
            pos++; // '{'
            pularEspacos();
            if (texto.charAt(pos) == '}') {
                pos++;
                return mapa;
            }
            while (true) {
                pularEspacos();
                String chave = lerString();
                pularEspacos();
                pos++; // ':'
                Object valor = lerValor();
                mapa.put(chave, valor);
                pularEspacos();
                char sep = texto.charAt(pos++);
                if (sep == '}') break;
            }
            return mapa;
        }

        private List<Object> lerArray() {
            List<Object> lista = new ArrayList<>();
            pos++; // '['
            pularEspacos();
            if (texto.charAt(pos) == ']') {
                pos++;
                return lista;
            }
            while (true) {
                lista.add(lerValor());
                pularEspacos();
                char sep = texto.charAt(pos++);
                if (sep == ']') break;
            }
            return lista;
        }

        private String lerString() {
            pos++; // '"'
            StringBuilder sb = new StringBuilder();
            while (pos < texto.length()) {
                char c = texto.charAt(pos++);
                if (c == '"') break;
                if (c == '\\') {
                    char esc = texto.charAt(pos++);
                    switch (esc) {
                        case '"', '\\', '/' -> sb.append(esc);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = texto.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean lerBooleano() {
            if (texto.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            pos += 5;
            return Boolean.FALSE;
        }

        private Integer lerNumero() {
            int inicio = pos;
            if (texto.charAt(pos) == '-') pos++;
            while (pos < texto.length() && Character.isDigit(texto.charAt(pos))) {
                pos++;
            }
            return Integer.parseInt(texto.substring(inicio, pos));
        }

        private void pularEspacos() {
            while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
                pos++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        String caminhoMatriz = args.length > 0 ? args[0] : "dados/matriz.json";
        String caminhoOferta = args.length > 1 ? args[1] : "dados/oferta.json";
        String caminhoHistorico = args.length > 2 ? args[2] : "dados/historico.json";

        Map<String, Object> jsonMatriz = LeitorJson.lerObjeto(Path.of(caminhoMatriz));
        Map<String, Object> jsonOferta = LeitorJson.lerObjeto(Path.of(caminhoOferta));
        Map<String, Object> jsonHistorico = LeitorJson.lerObjeto(Path.of(caminhoHistorico));

        List<Disciplina> disciplinas = new ArrayList<>();
        for (Object itemObj : (List<Object>) jsonMatriz.get("disciplinas")) {
            Map<String, Object> d = (Map<String, Object>) itemObj;
            String codigo = (String) d.get("codigo");
            String nome = (String) d.get("nome");
            int ch = (Integer) d.get("ch");
            List<String> prereqs = new ArrayList<>();
            for (Object req : (List<Object>) d.get("prerequisitos")) {
                prereqs.add((String) req);
            }
            disciplinas.add(new Disciplina(codigo, nome, ch, prereqs));
        }

        List<ItemOfertaEntrada> oferta = new ArrayList<>();
        for (Object itemObj : (List<Object>) jsonOferta.get("oferta")) {
            Map<String, Object> item = (Map<String, Object>) itemObj;
            String codigo = (String) item.get("codigo");
            List<Horario> horarios = new ArrayList<>();
            for (Object hObj : (List<Object>) item.get("horarios")) {
                Map<String, Object> h = (Map<String, Object>) hObj;
                horarios.add(new Horario((String) h.get("dia"), (String) h.get("turno")));
            }
            oferta.add(new ItemOfertaEntrada(codigo, horarios));
        }

        List<String> aprovadas = new ArrayList<>();
        for (Object ap : (List<Object>) jsonHistorico.get("aprovadas")) {
            aprovadas.add((String) ap);
        }
        int tetoCh = (Integer) jsonHistorico.get("teto_ch");

        MatrizCurricular matriz = new MatrizCurricular(disciplinas);
        HistoricoAluno historico = new HistoricoAluno(aprovadas, tetoCh);
        PlanejadorDeMatricula planejador = new PlanejadorDeMatricula(matriz, historico);

        ResultadoMatricula resultado = planejador.executar(oferta);
        System.out.println(resultado.paraJson());
    }
}
