# AUDITORIA — versão gerada por IA (Java)

> Objeto auditado: `gerada-por-ia/RESPOSTA-ORIGINAL/SolucaoOO.java`, usado **sem nenhuma correção**.
> Prompt exato: `gerada-por-ia/PROMPT.md` *(a preencher pelo trio: o prompt não estava disponível durante esta análise)*.
> Ambiente: OpenJDK 21, execução com `java SolucaoOO.java` (launcher de fonte).
> Observação de método: a cópia executada foi transcrita com as chaves de métodos curtos compactadas em uma linha. A semântica é idêntica; o arquivo entregue em `RESPOSTA-ORIGINAL/` deve ser o original, byte a byte.

## 1. Resumo

| | |
|---|---|
| Bateria comum (5 casos) | **5 de 5 corretos**, saída idêntica ao `saidas_esperadas.json` |
| Defeitos encontrados fora da bateria | **5** (1 com resposta errada silenciosa, 2 com omissão silenciosa, 1 com falha de execução, 1 menor) |
| Categorias predominantes | Suposição não declarada (4 primários) e Terminação ou robustez (1 primário) |
| Categorias sem ocorrência | Erro de tipo, Erro de escopo, Fato inventado, Complexidade |

A IA acertou os cinco casos de teste. O relatório não termina aí: todos os defeitos abaixo estão **fora** do que a bateria cobre, e é por isso que nenhum teste existente os pega. Dos cinco, quatro são lacunas do enunciado que o modelo preencheu sem avisar.

## 2. Resultado da bateria

| Caso | Resultado |
|---|---|
| 1 · dados padrão, teto 240 h | OK (4 grades empatadas) |
| 2 · ciclo | OK (`["CC0106","CC0109","CC0106"]`, demais chaves `null`) |
| 3 · calouro | OK (1 liberada, 7 bloqueadas, `[["CC0115"]]`) |
| 4 · histórico completo | OK (`[]`, `[]`, `[[]]`) |
| 5 · teto 150 h | OK (8 grades de 3 disciplinas) |

Comparação feita como JSON (conteúdo), não byte a byte.

## 3. Defeitos

### D1 — Oferta com código repetido: duplicatas na saída e horário sobrescrito

**Descrição.** O enunciado não diz o que fazer se a mesma disciplina aparece duas vezes na oferta. O código acrescenta as duas à lista de pendentes e grava o horário no objeto `Disciplina` compartilhado da matriz (`definirHorariosOfertados`), de modo que a última entrada apaga a anterior para todas as ocorrências.

**Entrada que revela.** `dados/oferta.json` com mais um item no final:

```json
{"codigo": "CC0104", "horarios": [{"dia": "SEX", "turno": "T"}]}
```

**Saída observada** (matriz e histórico do caso 1):

- `liberadas` traz `CC0104` duas vezes.
- `grades_maximas` traz grades repetidas.
- O horário original de CC0104 (SEG-M, QUA-M) desapareceu: surgem grades como `["CC0104","CC0110","CC0112","CC0115"]`, que conflitam no caso 1, e CC0104 nunca aparece junto de CC0114, porque o novo horário (SEX-T) conflita com ele.

Com uma duplicata de `CC0115` **sem horários** (`"horarios": []`), a mesma disciplina entra duas vezes na mesma grade: `["CC0104","CC0112","CC0114","CC0115","CC0115"]`, e conta 5 disciplinas onde há 4.

**Classificação.** Primário: **Suposição não declarada** (o modelo decidiu em silêncio "acumula e a última vence"). Secundário: **Efeito colateral ou aliasing** (horário da oferta guardado em objeto mutável compartilhado da matriz).

**Verificador mais barato.** **Revisão humana.** Basta ler `definirHorariosOfertados` (que faz `clear()` e `addAll`) e perguntar "e se o código repetir?". Um teste automatizado só pega isso depois que alguém já fez a pergunta; ele serve para impedir a regressão, não para descobrir o defeito.

### D2 — Código da oferta que não existe na matriz é descartado em silêncio

**Descrição.** Em `ItemOfertaEntrada.vincularNaMatriz`, `if (disc != null) { ... }` não tem ramo `else`. Uma disciplina ofertada e desconhecida some: não aparece em `liberadas`, nem em `bloqueadas`, e não há aviso.

**Entrada que revela.** `dados/oferta.json` com um item extra:

```json
{"codigo": "CC0999", "horarios": [{"dia": "SEG", "turno": "T"}]}
```

**Saída observada.** Idêntica à do caso 1, sem nenhuma menção a `CC0999`.

**Classificação.** **Suposição não declarada** (o enunciado não define o comportamento; o modelo escolheu "ignorar").

**Verificador mais barato.** **Revisão humana.** O `if` sem `else` é visível no código. Nenhum teste da bateria espera falha aqui.

### D3 — Pré-requisito inexistente na matriz: matriz "válida", disciplina bloqueada para sempre

**Descrição.** Há duas regras incompatíveis no mesmo código. Na detecção de ciclo, `visitarPrerequisito` trata um pré-requisito desconhecido como inexistente (`alvo == null → return false`) e a matriz sai "válida". Na liberação, `estaLiberadaPara` trata o mesmo pré-requisito como não cumprido. O enunciado chama erro de cadastro o ciclo; uma referência para um código que não existe é erro de cadastro do mesmo tipo e não é detectada.

**Entrada que revela.** `dados/matriz.json` com `CC0104.prerequisitos = ["CC0999"]`.

**Saída observada.** `"ciclo": null`; `CC0104` fica em `bloqueadas` com `"faltam": ["CC0999"]`, e `CC0106` e `CC0107` ficam bloqueadas "por falta de CC0104". Ninguém consegue cumprir CC0999, então o aluno nunca poderá cursar CC0104, e o programa não reporta que a matriz está inconsistente.

**Classificação.** Primário: **Suposição não declarada**. Secundário: **Semântica do problema** (a pergunta 1 deveria validar os dados, e a validação ficou incompleta).

**Verificador mais barato.** **Revisão humana**, no `return false` do ramo `alvo == null`. Um teste só ajuda depois que o trio decidir qual é a regra correta (erro? ignora?).

### D4 — Leitor JSON próprio frágil: falha de execução com mensagens enganosas

**Descrição.** O código implementa o próprio parser de JSON e só entende inteiros, sem BOM, sem `null` e sem tratamento de erro.

**Entradas que revelam e saídas observadas:**

| Entrada | Saída |
|---|---|
| `historico.json` salvo com BOM UTF-8 (comum em editores no Windows) | `NumberFormatException: For input string: ""` |
| `"ch": 60.0` na matriz | `NumberFormatException: For input string: ""` |
| `"teto_ch": 240.0` | `StringIndexOutOfBoundsException: Index 100 out of bounds for length 99` |
| `"prerequisitos": null` | `NullPointerException` em `main` |

Nenhuma das quatro mensagens diz qual arquivo ou campo tem problema.

**Classificação.** Primário: **Terminação ou robustez** (exceção não tratada em entrada válida segundo o formato JSON). Secundário: **Suposição não declarada** (assume inteiros e ausência de BOM).

**Verificador mais barato.** **Teste automatizado** com entradas degeneradas (BOM, decimal, `null`): cada caso é uma linha de arquivo de teste. O caso do BOM, em particular, só costuma ser descoberto por **quem usa o programa no Windows** se ninguém testar.

### D5 — Dia e turno são sensíveis a maiúsculas (menor)

**Descrição.** `Horario.conflitaCom` usa `equals` em `dia` e `turno`. `"seg"` e `"SEG"` são tratados como horários diferentes, e o conflito não é detectado.

**Entrada que revela.** `CC0110` na oferta com `{"dia":"seg","turno":"m"}` no lugar de SEG-M.

**Saída observada.** Aparecem grades com `CC0104` e `CC0110` juntas, que o caso 1 proíbe. A resposta fica errada, sem erro nenhum.

**Gravidade.** Baixa: os arquivos fornecidos já usam o formato canônico em maiúsculas, então só falha com dados fora do padrão.

**Classificação.** **Suposição não declarada** (formato de `dia` e `turno` assumido).

**Verificador mais barato.** **Teste automatizado** com entrada em caixa mista, se o trio decidir que normalizar é parte da especificação. Caso contrário, é uma observação para a análise, não um defeito a corrigir.

## 4. Investigado e **não** é defeito

| Hipótese | Resultado |
|---|---|
| Ciclo de um nó (`CC0112` exige `CC0112`) | Detectado: `["CC0112","CC0112"]` |
| Ciclo de três nós (`CC0101 → CC0104 → CC0103 → CC0101`) | Detectado: `["CC0101","CC0104","CC0103","CC0101"]` |
| Laço infinito na verificação de pré-requisitos (falha clássica da IA) | **Não ocorre**: a travessia usa estados na pilha/concluído |
| Estouro de pilha em cadeia profunda de pré-requisitos | **Não reproduzido** com cadeias de 20.000, 60.000 e 200.000 disciplinas (JVM com pilha padrão) |
| Oferta vazia | `liberadas: []`, `bloqueadas: []`, `grades_maximas: [[]]` |
| Teto: `<` vs `<=` | Correto (`<=`); o caso 5 fecha exatamente em 150 h |
| Poda perdendo empates | Não perde: o limite `tamanho + restantes >= melhor` mantém as grades empatadas |
| "Cursou" vs "aprovado" | Usa só `aprovadas`, como pede o enunciado |
| Pré-requisito direto, não transitivo | Correto |
| **Complexidade** da pergunta 4 | Ver abaixo |

**Sobre complexidade.** Medi a busca com oferta sem conflitos e teto ativo (execução única, incluindo o startup da JVM, então os números são indicativos e não entram no eixo Desempenho):

| Oferta | Teto | Grades máximas | Tempo total |
|---|---|---|---|
| 16 | 480 h | 12.870 | 1,5 s |
| 20 | 600 h | 184.756 | 2,0 s |
| 22 | 660 h | 705.432 | 2,9 s |
| 24 | 720 h | 2.704.156 | 7,1 s |

O número de grades é exatamente C(n, n/2). O crescimento vem do **tamanho da saída exigida** (todas as empatadas), e o enunciado já avisa que a pergunta 4 é intratável. Por isso **não classifico como defeito**. Fica a observação de que as grades são materializadas todas em memória antes da impressão.

## 5. Por que a IA acertou a bateria

- O problema é **clássico e bem delimitado**: busca em profundidade com três estados para ciclo, e subconjuntos com retrocesso para a grade.
- O contrato de saída (seção 3 do enunciado) é muito explícito e fecha as ambiguidades dos casos 1 a 5: ordenação, listas vazias e `[[]]`.
- Os cinco casos exercitam os pontos que o enunciado destaca (ciclo, conjunto vazio, teto ativo). Nenhum deles toca em **dado malformado, repetido ou fora do padrão**, que é exatamente onde estão os cinco defeitos.

## 6. Distribuição por categoria (causa mais precoce)

| Categoria | Primário | Secundário |
|---|---|---|
| Erro de tipo | 0 | 0 |
| Erro de escopo ou vinculação | 0 | 0 |
| Fato inventado | 0 | 0 |
| Efeito colateral ou aliasing | 0 | 1 (D1) |
| Semântica do problema | 0 | 1 (D3) |
| Terminação ou robustez | 1 (D4) | 0 |
| Complexidade | 0 | 0 |
| Suposição não declarada | 4 (D1, D2, D3, D5) | 1 (D4) |

Leitura: o compilador não teria pegado nenhum dos cinco, e a bateria de cinco casos também não. Quatro dos cinco exigem uma **decisão de especificação**, que um humano precisa tomar. Isso conecta com o ponto do enunciado sobre o número "94% das falhas são de tipo": ele mede só o código que não compila, e aqui o código compila e passa nos testes.

## 7. Como reproduzir

A partir da raiz do repositório, com cada entrada modificada salva em `auditoria/` (arquivos derivados de `dados/`, que não devem ser alterados):

```bash
java gerada-por-ia/RESPOSTA-ORIGINAL/SolucaoOO.java dados/matriz.json auditoria/D1_oferta_dup.json dados/historico.json
java gerada-por-ia/RESPOSTA-ORIGINAL/SolucaoOO.java dados/matriz.json auditoria/D2_oferta_fora.json dados/historico.json
java gerada-por-ia/RESPOSTA-ORIGINAL/SolucaoOO.java auditoria/D3_matriz_prereq_fantasma.json dados/oferta.json dados/historico.json
java gerada-por-ia/RESPOSTA-ORIGINAL/SolucaoOO.java dados/matriz.json dados/oferta.json auditoria/D4_historico_bom.json
java gerada-por-ia/RESPOSTA-ORIGINAL/SolucaoOO.java dados/matriz.json auditoria/D5_oferta_minuscula.json dados/historico.json
```

## 8. Pendências do trio

- Colar em `PROMPT.md` o prompt exato e salvar em `RESPOSTA-ORIGINAL/` o código como veio.
- Criar as entradas de `auditoria/` conforme as descrições acima.
- Se este mesmo arquivo for também a versão OO do trio, **separar**: a versão em `gerada-por-ia/` não pode ser reaproveitada em `oo/`, e o uso precisa constar no `USO-DE-IA.md`.
