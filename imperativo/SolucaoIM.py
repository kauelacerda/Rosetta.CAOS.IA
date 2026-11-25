import json
import sys


def contem_item(lista, alvo):
    i = 0
    tamanho = len(lista)
    while i < tamanho:
        if lista[i] == alvo:
            return True
        i += 1
    return False


def copiar_lista(origem):
    destino = []
    i = 0
    tamanho = len(origem)
    while i < tamanho:
        destino.append(origem[i])
        i += 1
    return destino


def ordenar_strings(lista):
    # Insertion sort in-place para listas de strings
    i = 1
    tamanho = len(lista)
    while i < tamanho:
        chave = lista[i]
        j = i - 1
        while j >= 0 and lista[j] > chave:
            lista[j + 1] = lista[j]
            j -= 1
        lista[j + 1] = chave
        i += 1


def ordenar_bloqueadas(lista_bloqueadas):
    # Insertion sort in-place ordenado pelo campo "codigo"
    i = 1
    tamanho = len(lista_bloqueadas)
    while i < tamanho:
        chave = lista_bloqueadas[i]
        j = i - 1
        while j >= 0 and lista_bloqueadas[j]["codigo"] > chave["codigo"]:
            lista_bloqueadas[j + 1] = lista_bloqueadas[j]
            j -= 1
        lista_bloqueadas[j + 1] = chave
        i += 1


def lista_menor_que(a, b):
    # Comparacao lexicografica manual entre duas listas de strings
    tam_a = len(a)
    tam_b = len(b)
    limite = tam_a if tam_a < tam_b else tam_b
    i = 0
    while i < limite:
        if a[i] < b[i]:
            return True
        if a[i] > b[i]:
            return False
        i += 1
    return tam_a < tam_b


def ordenar_grades(grades):
    # Insertion sort in-place em ordem lexicografica
    i = 1
    tamanho = len(grades)
    while i < tamanho:
        chave = grades[i]
        j = i - 1
        while j >= 0 and lista_menor_que(chave, grades[j]):
            grades[j + 1] = grades[j]
            j -= 1
        grades[j + 1] = chave
        i += 1


def encontrar_ciclo(disciplinas_por_codigo, codigos_ordenados):
    # Pergunta 1: DFS com 3 estados (0 = nao visitado, 1 = na pilha, 2 = concluido)
    estado = {}
    i = 0
    total = len(codigos_ordenados)
    while i < total:
        estado[codigos_ordenados[i]] = 0
        i += 1

    caminho_atual = []
    ciclo_encontrado = []

    def dfs(codigo):
        estado[codigo] = 1
        caminho_atual.append(codigo)

        disc = disciplinas_por_codigo[codigo]
        prereqs = copiar_lista(disc["prerequisitos"])
        ordenar_strings(prereqs)

        idx = 0
        qtd_prereqs = len(prereqs)
        while idx < qtd_prereqs:
            req = prereqs[idx]
            if req in estado:
                if estado[req] == 1:
                    pos = 0
                    tam_caminho = len(caminho_atual)
                    while pos < tam_caminho and caminho_atual[pos] != req:
                        pos += 1
                    while pos < tam_caminho:
                        ciclo_encontrado.append(caminho_atual[pos])
                        pos += 1
                    ciclo_encontrado.append(req)
                    return True
                elif estado[req] == 0:
                    if dfs(req):
                        return True
            idx += 1

        caminho_atual.pop()
        estado[codigo] = 2
        return False

    i = 0
    while i < total:
        cod = codigos_ordenados[i]
        if estado[cod] == 0:
            if dfs(cod):
                return ciclo_encontrado
        i += 1

    return None


def classificar_oferta(disciplinas_por_codigo, oferta_lista, aprovadas):
    # Perguntas 2 e 3: separa disciplinas da oferta em liberadas e bloqueadas
    liberadas = []
    bloqueadas = []

    i = 0
    qtd_oferta = len(oferta_lista)
    while i < qtd_oferta:
        item_oferta = oferta_lista[i]
        codigo = item_oferta["codigo"]

        if not contem_item(aprovadas, codigo):
            disc = disciplinas_por_codigo[codigo]
            prereqs = disc["prerequisitos"]
            faltam = []

            j = 0
            qtd_prereqs = len(prereqs)
            while j < qtd_prereqs:
                req = prereqs[j]
                if not contem_item(aprovadas, req):
                    faltam.append(req)
                j += 1

            if len(faltam) == 0:
                liberadas.append(codigo)
            else:
                ordenar_strings(faltam)
                bloqueadas.append({
                    "codigo": codigo,
                    "faltam": faltam
                })
        i += 1

    ordenar_strings(liberadas)
    ordenar_bloqueadas(bloqueadas)
    return liberadas, bloqueadas


def horarios_conflitam(horarios_a, horarios_b):
    i = 0
    tam_a = len(horarios_a)
    while i < tam_a:
        ha = horarios_a[i]
        j = 0
        tam_b = len(horarios_b)
        while j < tam_b:
            hb = horarios_b[j]
            if ha["dia"] == hb["dia"] and ha["turno"] == hb["turno"]:
                return True
            j += 1
        i += 1
    return False


def buscar_grades_maximas(liberadas, disciplinas_por_codigo, horarios_por_codigo, teto_ch):
    # Pergunta 4: busca combinatoria por backtracking com mutacao e desfazimento
    n = len(liberadas)

    chs = []
    i = 0
    while i < n:
        cod = liberadas[i]
        chs.append(disciplinas_por_codigo[cod]["ch"])
        i += 1

    # Matriz de adjacencia de conflitos de horario entre disciplinas liberadas
    conflito = []
    i = 0
    while i < n:
        linha = []
        j = 0
        while j < n:
            if i == j:
                linha.append(False)
            else:
                tem_choque = horarios_conflitam(
                    horarios_por_codigo[liberadas[i]],
                    horarios_por_codigo[liberadas[j]]
                )
                linha.append(tem_choque)
            j += 1
        conflito.append(linha)
        i += 1

    melhor_qtd = [0]
    melhores_grades = []
    indices_escolhidos = []
    grade_atual = []

    def backtrack(idx, ch_atual):
        qtd_atual = len(grade_atual)

        # Poda: mesmo incluindo todas as disciplinas restantes nao atinge melhor_qtd[0]
        if qtd_atual + (n - idx) < melhor_qtd[0]:
            return

        if idx == n:
            if qtd_atual > melhor_qtd[0]:
                melhor_qtd[0] = qtd_atual
                while len(melhores_grades) > 0:
                    melhores_grades.pop()
                melhores_grades.append(copiar_lista(grade_atual))
            elif qtd_atual == melhor_qtd[0]:
                melhores_grades.append(copiar_lista(grade_atual))
            return

        # Ramo 1: tenta incluir liberadas[idx]
        nova_ch = ch_atual + chs[idx]
        if nova_ch <= teto_ch:
            tem_conflito = False
            k = 0
            qtd_esc = len(indices_escolhidos)
            while k < qtd_esc:
                if conflito[indices_escolhidos[k]][idx]:
                    tem_conflito = True
                    break
                k += 1

            if not tem_conflito:
                indices_escolhidos.append(idx)
                grade_atual.append(liberadas[idx])
                backtrack(idx + 1, nova_ch)
                grade_atual.pop()
                indices_escolhidos.pop()

        # Ramo 2: nao inclui liberadas[idx]
        backtrack(idx + 1, ch_atual)

    backtrack(0, 0)
    ordenar_grades(melhores_grades)
    return melhores_grades


def resolver(dados_matriz, dados_oferta, dados_historico):
    disciplinas_lista = dados_matriz["disciplinas"]
    disciplinas_por_codigo = {}
    codigos_ordenados = []

    i = 0
    total_disc = len(disciplinas_lista)
    while i < total_disc:
        disc = disciplinas_lista[i]
        cod = disc["codigo"]
        disciplinas_por_codigo[cod] = disc
        codigos_ordenados.append(cod)
        i += 1

    ordenar_strings(codigos_ordenados)

    # 1. Validacao da matriz (roda primeiro; se houver ciclo, encerra)
    ciclo = encontrar_ciclo(disciplinas_por_codigo, codigos_ordenados)
    if ciclo is not None:
        return {
            "ciclo": ciclo,
            "liberadas": None,
            "bloqueadas": None,
            "grades_maximas": None
        }

    # Indexa horarios da oferta por codigo
    oferta_lista = dados_oferta["oferta"]
    horarios_por_codigo = {}
    i = 0
    total_oferta = len(oferta_lista)
    while i < total_oferta:
        item = oferta_lista[i]
        horarios_por_codigo[item["codigo"]] = item["horarios"]
        i += 1

    aprovadas = dados_historico["aprovadas"]
    teto_ch = dados_historico["teto_ch"]

    # 2 e 3. Disciplinas liberadas e bloqueadas
    liberadas, bloqueadas = classificar_oferta(
        disciplinas_por_codigo,
        oferta_lista,
        aprovadas
    )

    # 4. Maior grade possivel sem conflito e dentro do teto
    grades_maximas = buscar_grades_maximas(
        liberadas,
        disciplinas_por_codigo,
        horarios_por_codigo,
        teto_ch
    )

    return {
        "ciclo": None,
        "liberadas": liberadas,
        "bloqueadas": bloqueadas,
        "grades_maximas": grades_maximas
    }


def main():
    caminho_matriz = sys.argv[1] if len(sys.argv) > 1 else "dados/matriz.json"
    caminho_oferta = sys.argv[2] if len(sys.argv) > 2 else "dados/oferta.json"
    caminho_historico = sys.argv[3] if len(sys.argv) > 3 else "dados/historico.json"

    with open(caminho_matriz, "r", encoding="utf-8") as f:
        dados_matriz = json.load(f)

    with open(caminho_oferta, "r", encoding="utf-8") as f:
        dados_oferta = json.load(f)

    with open(caminho_historico, "r", encoding="utf-8") as f:
        dados_historico = json.load(f)

    resultado = resolver(dados_matriz, dados_oferta, dados_historico)
    print(json.dumps(resultado, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
