"use strict";

const fs = require("fs");

// =============================================================================
// UTILITÁRIOS DE IMUTABILIDADE, INDEXAÇÃO E ORDENAÇÃO PURA
// =============================================================================

const clonarCongelado = (valor) => {
  if (valor === null || typeof valor !== "object") {
    return valor;
  }
  if (Array.isArray(valor)) {
    return Object.freeze(valor.map(clonarCongelado));
  }
  return Object.freeze(
    Object.fromEntries(
      Object.entries(valor).map(([chave, val]) => [chave, clonarCongelado(val)])
    )
  );
};

const temChave = (mapa, chave) =>
  Object.prototype.hasOwnProperty.call(mapa, chave);

const indexarPorCodigo = (lista, extrairValor) =>
  Object.freeze(
    Object.fromEntries(
      lista.map((item) => [item.codigo, extrairValor(item)])
    )
  );

const indexarConjunto = (codigos) =>
  Object.freeze(
    Object.fromEntries(codigos.map((codigo) => [codigo, true]))
  );

const compararStrings = (a, b) => (a < b ? -1 : a > b ? 1 : 0);

const compararBloqueadas = (a, b) => compararStrings(a.codigo, b.codigo);

const compararGrades = (gradeA, gradeB) => {
  const compararPosicao = (idx) => {
    if (idx >= gradeA.length || idx >= gradeB.length) {
      return gradeA.length - gradeB.length;
    }
    const cmp = compararStrings(gradeA[idx], gradeB[idx]);
    return cmp !== 0 ? cmp : compararPosicao(idx + 1);
  };
  return compararPosicao(0);
};

const intercalar = (esq, dir, comparar) => {
  if (esq.length === 0) return dir;
  if (dir.length === 0) return esq;

  const [cabEsq, ...restoEsq] = esq;
  const [cabDir, ...restoDir] = dir;

  return comparar(cabEsq, cabDir) <= 0
    ? Object.freeze([cabEsq, ...intercalar(restoEsq, dir, comparar)])
    : Object.freeze([cabDir, ...intercalar(esq, restoDir, comparar)]);
};

const mergeSort = (lista, comparar) => {
  if (lista.length <= 1) {
    return Object.freeze([...lista]);
  }
  const meio = Math.floor(lista.length / 2);
  const esquerda = mergeSort(lista.slice(0, meio), comparar);
  const direita = mergeSort(lista.slice(meio), comparar);
  return intercalar(esquerda, direita, comparar);
};

// =============================================================================
// NÚCLEO PURO — PERGUNTA 1: VALIDAÇÃO DE CICLOS NA MATRIZ CURRICULAR
// =============================================================================

const explorarNo = (codigo, mapaDisciplinas, caminho, visitados) => {
  if (caminho.includes(codigo)) {
    const inicioCiclo = caminho.indexOf(codigo);
    return Object.freeze({
      ciclo: Object.freeze([...caminho.slice(inicioCiclo), codigo]),
      visitados
    });
  }

  if (temChave(visitados, codigo)) {
    return Object.freeze({ ciclo: null, visitados });
  }

  if (!temChave(mapaDisciplinas, codigo)) {
    return Object.freeze({ ciclo: null, visitados });
  }

  const disciplina = mapaDisciplinas[codigo];
  const novoCaminho = Object.freeze([...caminho, codigo]);
  const prereqsOrdenados = mergeSort(disciplina.prerequisitos, compararStrings);

  const resultadoPrereqs = explorarLista(
    prereqsOrdenados,
    mapaDisciplinas,
    novoCaminho,
    visitados
  );

  if (resultadoPrereqs.ciclo !== null) {
    return resultadoPrereqs;
  }

  return Object.freeze({
    ciclo: null,
    visitados: Object.freeze({ ...resultadoPrereqs.visitados, [codigo]: true })
  });
};

const explorarLista = (codigos, mapaDisciplinas, caminho, visitados) => {
  if (codigos.length === 0) {
    return Object.freeze({ ciclo: null, visitados });
  }

  const [cabeca, ...cauda] = codigos;
  const resCabeca = explorarNo(cabeca, mapaDisciplinas, caminho, visitados);

  if (resCabeca.ciclo !== null) {
    return resCabeca;
  }

  return explorarLista(cauda, mapaDisciplinas, caminho, resCabeca.visitados);
};

const detectarCiclo = (mapaDisciplinas, codigosOrdenados) => {
  const resultado = explorarLista(
    codigosOrdenados,
    mapaDisciplinas,
    Object.freeze([]),
    Object.freeze({})
  );
  return resultado.ciclo;
};

// =============================================================================
// NÚCLEO PURO — PERGUNTAS 2 E 3: DISCIPLINAS LIBERADAS E BLOQUEADAS
// =============================================================================

const classificarOferta = (mapaDisciplinas, oferta, mapaAprovadas) => {
  const pendentes = oferta
    .map((item) => item.codigo)
    .filter((codigo) => !temChave(mapaAprovadas, codigo));

  const avaliacoes = pendentes.map((codigo) => {
    const prereqs = mapaDisciplinas[codigo].prerequisitos;
    const faltam = mergeSort(
      prereqs.filter((req) => !temChave(mapaAprovadas, req)),
      compararStrings
    );
    return Object.freeze({ codigo, faltam });
  });

  const liberadas = mergeSort(
    avaliacoes
      .filter((item) => item.faltam.length === 0)
      .map((item) => item.codigo),
    compararStrings
  );

  const bloqueadas = mergeSort(
    avaliacoes.filter((item) => item.faltam.length > 0),
    compararBloqueadas
  );

  return Object.freeze({ liberadas, bloqueadas });
};

// =============================================================================
// NÚCLEO PURO — PERGUNTA 4: MAIOR GRADE SEM CONFLITO (COM BRANCH AND BOUND)
// =============================================================================

const horariosConflitam = (horariosA, horariosB) =>
  horariosA.some((ha) =>
    horariosB.some((hb) => ha.dia === hb.dia && ha.turno === hb.turno)
  );

const gradeConflitaCom = (gradeAtual, codigoCandidata, mapaHorarios) =>
  gradeAtual.some((codigoEscolhido) =>
    horariosConflitam(
      mapaHorarios[codigoEscolhido],
      mapaHorarios[codigoCandidata]
    )
  );

const buscarSubconjuntos = (
  restantes,
  gradeAtual,
  chAtual,
  melhorTamanho,
  melhoresGrades,
  mapaDisciplinas,
  mapaHorarios,
  tetoCh
) => {
  // Poda equivalente às versões imperativa e OO
  if (gradeAtual.length + restantes.length < melhorTamanho) {
    return Object.freeze({ melhorTamanho, melhoresGrades });
  }

  if (restantes.length === 0) {
    if (gradeAtual.length > melhorTamanho) {
      return Object.freeze({
        melhorTamanho: gradeAtual.length,
        melhoresGrades: Object.freeze([gradeAtual])
      });
    }
    if (gradeAtual.length === melhorTamanho) {
      return Object.freeze({
        melhorTamanho,
        melhoresGrades: Object.freeze([...melhoresGrades, gradeAtual])
      });
    }
    return Object.freeze({ melhorTamanho, melhoresGrades });
  }

  const [candidata, ...cauda] = restantes;
  const chCandidata = mapaDisciplinas[candidata].ch;
  const cabeNoTeto = chAtual + chCandidata <= tetoCh;
  const semChoque = !gradeConflitaCom(gradeAtual, candidata, mapaHorarios);

  const aposIncluir =
    cabeNoTeto && semChoque
      ? buscarSubconjuntos(
          cauda,
          Object.freeze([...gradeAtual, candidata]),
          chAtual + chCandidata,
          melhorTamanho,
          melhoresGrades,
          mapaDisciplinas,
          mapaHorarios,
          tetoCh
        )
      : Object.freeze({ melhorTamanho, melhoresGrades });

  return buscarSubconjuntos(
    cauda,
    gradeAtual,
    chAtual,
    aposIncluir.melhorTamanho,
    aposIncluir.melhoresGrades,
    mapaDisciplinas,
    mapaHorarios,
    tetoCh
  );
};

const buscarGradesMaximas = (
  liberadas,
  mapaDisciplinas,
  mapaHorarios,
  tetoCh
) => {
  const resultado = buscarSubconjuntos(
    liberadas,
    Object.freeze([]),
    0,
    0,
    Object.freeze([]),
    mapaDisciplinas,
    mapaHorarios,
    tetoCh
  );

  return mergeSort(resultado.melhoresGrades, compararGrades);
};

// =============================================================================
// ORQUESTRAÇÃO PURA DO PROBLEMA
// =============================================================================

const resolver = (dadosMatriz, dadosOferta, dadosHistorico) => {
  const matriz = clonarCongelado(dadosMatriz);
  const oferta = clonarCongelado(dadosOferta);
  const historico = clonarCongelado(dadosHistorico);

  const mapaDisciplinas = indexarPorCodigo(matriz.disciplinas, (d) => d);
  const codigosOrdenados = mergeSort(
    matriz.disciplinas.map((d) => d.codigo),
    compararStrings
  );

  // 1. Validação da matriz curricular (roda primeiro; se houver ciclo, encerra)
  const ciclo = detectarCiclo(mapaDisciplinas, codigosOrdenados);
  if (ciclo !== null) {
    return Object.freeze({
      ciclo,
      liberadas: null,
      bloqueadas: null,
      grades_maximas: null
    });
  }

  // 2 e 3. Disciplinas liberadas e bloqueadas
  const mapaAprovadas = indexarConjunto(historico.aprovadas);
  const { liberadas, bloqueadas } = classificarOferta(
    mapaDisciplinas,
    oferta.oferta,
    mapaAprovadas
  );

  // 4. Maior grade possível sem conflito e dentro do teto
  const mapaHorarios = indexarPorCodigo(oferta.oferta, (item) => item.horarios);
  const gradesMaximas = buscarGradesMaximas(
    liberadas,
    mapaDisciplinas,
    mapaHorarios,
    historico.teto_ch
  );

  return Object.freeze({
    ciclo: null,
    liberadas,
    bloqueadas,
    grades_maximas: gradesMaximas
  });
};

// =============================================================================
// CASCA IMPURA (FRONTEIRA DE I/O)
// =============================================================================

const main = () => {
  const caminhoMatriz = process.argv[2] || "dados/matriz.json";
  const caminhoOferta = process.argv[3] || "dados/oferta.json";
  const caminhoHistorico = process.argv[4] || "dados/historico.json";

  const dadosMatriz = JSON.parse(fs.readFileSync(caminhoMatriz, "utf-8"));
  const dadosOferta = JSON.parse(fs.readFileSync(caminhoOferta, "utf-8"));
  const dadosHistorico = JSON.parse(fs.readFileSync(caminhoHistorico, "utf-8"));

  const resultado = resolver(dadosMatriz, dadosOferta, dadosHistorico);
  console.log(JSON.stringify(resultado, null, 2));
};

if (require.main === module) {
  main();
}

module.exports = { resolver };