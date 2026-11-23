# T2 — Rosetta de Paradigmas

CC0114 — Teoria e Paradigmas de Linguagens de Programação
Ciência da Computação · UNIFAP · 2026.2

Peso: 15% da média final · Em trios
Lançado: 04/11/2026 · Entrega final: 25/11/2026

## O que tem neste pacote

| Arquivo | O que é |
|---|---|
| `T2_rosetta_de_paradigmas.html` | O enunciado completo. Abra no navegador — é a versão para ler. |
| `T2_rosetta_de_paradigmas.pdf` | O mesmo enunciado, para imprimir. |
| `dados/` | Os sete arquivos de entrada. **Não alterem nenhum deles.** |
| `saidas_esperadas.json` | As respostas dos cinco casos de teste, no formato canônico da seção 3. |

## Por onde começar

1. Leiam o enunciado inteiro antes de escrever qualquer linha. As seções 1, 3 e 4 contêm decisões já tomadas que, se ignoradas, fazem as três versões divergirem — e a equivalência entre elas vale 25% da nota.
2. Formem o trio e escolham as três linguagens, uma por paradigma. As sugestões estão na seção 4.
3. Criem o repositório com a estrutura da seção 9 e copiem a pasta `dados/` para dentro dele.
4. Façam o programa ler os arquivos e imprimir o JSON do contrato de saída, ainda sem resolver nada. Essa é a Etapa 0, para 04/11.

## Sobre `saidas_esperadas.json`

Essas respostas vêm prontas de propósito. Elas são **especificação**, não gabarito: definem o que "correto" significa, do mesmo jeito que o contrato de saída da seção 3.

Usem para montar a bateria de testes comum que a rubrica cobra — rode as três implementações sobre os cinco casos e compare a saída de cada uma com este arquivo. Se as três concordarem entre si e com ele, o critério de 25% está atendido, e vocês podem gastar o tempo restante onde a nota de fato está: fidelidade ao paradigma, análise comparativa e auditoria, que somam 60%.

Se a sua saída divergir, comece assumindo que a sua está errada — mas releia e, se ainda achar que o erro é do enunciado, venha falar comigo. Achar erro no enunciado vale ponto.

## Cronograma

| Data | Etapa | O que entregar |
|---|---|---|
| 04/11 | 0 · Partida | Trio, linguagens, repositório, leitura dos dados |
| 11/11 | 1 · Imperativa + IA + medição | Versão imperativa nos cinco casos, versão gerada por IA com auditoria, arnês de medição |
| 18/11 | 2 · Funcional | Versão funcional, saída idêntica à imperativa |
| 25/11 | 3 · OO + análise | Versão OO, análise comparativa, README de reprodução |

Cada entrega parcial não entregue custa 0,5 ponto na nota final do T2.
