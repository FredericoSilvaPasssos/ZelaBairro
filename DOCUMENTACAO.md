# ZelaBairro: documentação técnica

Global Solution 2 (2026), CitizenTech. Turma 3SIOA.

## 1. Visão geral

O ZelaBairro é um aplicativo Android para o cidadão **registrar, acompanhar e apoiar** problemas em espaços públicos (iluminação, buracos, calçadas, alagamentos, lixo, sinalização e praças). O poder público recebe as ocorrências já classificadas por categoria, órgão responsável e prioridade. A prioridade é calculada a partir da gravidade, do impacto na acessibilidade e do número de moradores afetados.

| Item | Descrição |
|---|---|
| Problema | Falta de canal simples e transparente para reportar e acompanhar problemas urbanos |
| Público | Moradores de áreas urbanas, com foco em idosos e pessoas com deficiência |
| Proposta de valor | Registro em menos de 1 minuto, protocolo, linha do tempo e força coletiva (apoios) |
| ODS | **11** Cidades e Comunidades Sustentáveis; **10** Redução das Desigualdades; **16** Instituições Eficazes |

### Relação com os ODS

- **ODS 11 (metas 11.2 e 11.7):** a solução busca espaços públicos seguros, inclusivos e acessíveis. Cada poste consertado ou calçada refeita melhora a mobilidade e a segurança do bairro.
- **ODS 10:** problemas que afetam pessoas com deficiência e idosos ganham +3 pontos de prioridade automaticamente.
- **ODS 16:** protocolo, linha do tempo pública e indicadores de prazo (SLA) por órgão dão transparência e permitem cobrar o poder público.

## 2. Funcionalidades

| # | Funcionalidade | Telas / rotinas |
|---|---|---|
| F1 | **Registrar ocorrência** com categoria, local, descrição e impacto na acessibilidade. Mostra o órgão responsável e a prioridade estimada antes do envio. Gera protocolo. | `NovaOcorrenciaScreen`, `PKG_ZELABAIRRO.registrar_ocorrencia` |
| F2 | **Acompanhar ocorrências** com busca, filtros por status e categoria, ordenação por prioridade, linha do tempo e reabertura. | `OcorrenciasScreen`, `DetalheOcorrenciaScreen`, `PKG_ZELABAIRRO.listar_ocorrencias`, `atualizar_status`, `reabrir_ocorrencia` |
| F3 | **Apoio comunitário** ("Também sou afetado"): aumenta a pontuação e a prioridade. | Cartões de ocorrência, `apoiar_ocorrencia`, trigger `TRG_ZB_APOIO_CONTADOR` |
| F4 | **Painel do bairro** com indicadores em tempo real e consolidação mensal. | `InicioScreen`, `gerar_indicadores_mensais` |

## 3. Arquitetura

```
 UI (Jetpack Compose)            Telas sem regra de negócio, só exibem estado
        |  observa StateFlow / envia eventos
 ViewModel (MVVM)                Estado da tela + ações; sobrevive à rotação
        |
 Repository                      Regras de negócio: protocolo, prioridade, status
        |
 Room DAO  --> SQLite local      Consultas reativas (Flow) e transações
        :
        :  (integração futura via API REST)
        :
 Oracle + PL/SQL                 Base central da prefeitura, automações e indicadores
```

O app usa uma única Activity (`MainActivity`). Todas as telas são funções `@Composable` ligadas pelo Navigation Compose.

## 4. Classes, funções e métodos

### 4.1 Pacote `model`: domínio

| Elemento | Tipo | Responsabilidade |
|---|---|---|
| `Categoria` | enum | Tipos de problema. Cada valor tem `rotulo` (texto da tela), `peso` (1 a 4, gravidade) e `orgaoResponsavel`. |
| `StatusOcorrencia` | enum | `REGISTRADA → EM_ANALISE → EM_ATENDIMENTO → RESOLVIDA`. `proximo()` retorna a próxima etapa e `pendente` indica se ainda falta resolver. |
| `Prioridade` | enum | `ALTA`, `MEDIA` e `BAIXA`, com rótulo. |
| `RegraPrioridade` | object | `pontuacao(categoria, afetaAcessibilidade, apoios)` soma peso + 3 (acessibilidade) + apoios (máx. 10). `calcular(...)` converte a pontuação em prioridade (≥10 Alta, ≥6 Média). É a mesma regra das funções Oracle. |
| `FiltroOcorrencias` | data class | Status, categoria e texto de busca da tela de acompanhamento. |
| `NovaOcorrencia` | data class | Dados digitados no formulário de registro. |

### 4.2 Pacote `data.local`: persistência (Room)

| Elemento | Responsabilidade |
|---|---|
| `OcorrenciaEntity` | Tabela `ocorrencias`. Índices em `status`, `categoria` e `protocolo` (único). |
| `HistoricoStatusEntity` | Tabela `historico_status`, com FK para a ocorrência e `ON DELETE CASCADE`. |
| `IndicadoresResumo`, `CategoriaContagem` | Resultados de consultas agregadas (POJOs). |
| `OcorrenciaDao` | `observarFiltradas(status, categoria, busca)`: lista reativa com filtros opcionais e ordenação por prioridade.<br>`observarPorId(id)` e `buscarPorId(id)`: uma ocorrência (`Flow` ou `suspend`).<br>`observarHistorico(id)`: linha do tempo.<br>`observarMaisApoiadas(limite)`: destaque do painel.<br>`observarIndicadores()`: COUNT/SUM/AVG feitos no SQLite.<br>`observarPendentesPorCategoria()`: GROUP BY para o gráfico de barras.<br>`registrarComHistorico(...)` e `atualizarComHistorico(...)`: `@Transaction`, gravam a ocorrência e o histórico de forma atômica. |
| `ZelaBairroDatabase` | `RoomDatabase` em **Singleton** (`getInstance`, com `@Volatile` + `synchronized`). Usa o `applicationContext` e um callback `onCreate` que popula os exemplos. |
| `DadosIniciais` | `popular(db)` insere 6 ocorrências e o histórico delas na primeira execução. `mensagemPadrao(status, categoria)` gera os textos padrão da linha do tempo. |

### 4.3 Pacote `data.repository`

| Método de `OcorrenciaRepository` | O que faz |
|---|---|
| `observarOcorrencias(filtro)` | Repassa os filtros ao DAO (com `trim` na busca). |
| `registrar(nova): String` | **F1.** Gera o protocolo `ZB-AAMMDD-NNNN`, calcula a prioridade inicial e grava a ocorrência e o histórico. Retorna o protocolo. |
| `alternarApoio(id): Boolean` | **F3.** Adiciona ou remove o apoio do usuário, recalcula a prioridade e bloqueia o apoio a ocorrências resolvidas. |
| `avancarStatus(id)` | Avança uma etapa (modo demonstração, simula o órgão). Grava `resolvidaEm` ao concluir. |
| `reabrir(id, motivo)` | Volta uma ocorrência resolvida para "Em análise" e grava o motivo no histórico. |
| `observarIndicadores()`, `observarPendentesPorCategoria()`, `observarMaisApoiadas()` | **F4.** Fluxos usados pelo painel. |

O parâmetro `relogio: () -> Long` permite injetar o tempo em testes.

### 4.4 Pacote `ui`

| Elemento | Responsabilidade |
|---|---|
| `AppViewModelProvider.Factory` | Cria todos os ViewModels passando o repositório (injeção de dependência manual). O `DetalheOcorrenciaViewModel` recebe o `SavedStateHandle` com o id da rota. |
| `InicioViewModel` | Junta 3 fluxos com `combine` em um `InicioUiState`. `alternarApoio(id)`. |
| `OcorrenciasViewModel` | `filtro` (`MutableStateFlow`). `ocorrencias` usa `debounce(250)` + `flatMapLatest`. `atualizarBusca`, `selecionarStatus`, `selecionarCategoria`, `limparFiltros`, `alternarApoio`. |
| `NovaOcorrenciaViewModel` / `FormularioState` | Estado do formulário e validações (`tituloValido`, `descricaoValida`, `valido`, `prioridadeEstimada`). `enviar()` valida, salva em coroutine e expõe o protocolo ou a mensagem de erro. |
| `DetalheOcorrenciaViewModel` | `uiState` (ocorrência + histórico), `mensagem` (snackbar), `alternarApoio()`, `avancarStatus()`, `reabrir(motivo)`. |
| `InicioScreen` | Chamada para registrar e painel: cartões numéricos, taxa de resolução, tempo médio, barras por categoria e ocorrências mais apoiadas. |
| `OcorrenciasScreen` | Busca, chips de status e categoria, lista com `LazyColumn` e estados de carregando/vazio. |
| `NovaOcorrenciaScreen` | Formulário em etapas (o quê, onde, descrição), interruptor de acessibilidade e diálogo com o protocolo. |
| `DetalheOcorrenciaScreen` | Dados, cartão de apoio, confirmação de resolução, linha do tempo e modo demonstração. |
| `Componentes.kt` | `OcorrenciaCard`, `IndicadorCard`, `BarraProporcional`, `Selo`/`SeloStatus`/`SeloPrioridade`, `TituloSecao`, `EstadoVazio`, `formatarData`, `formatarDuracao`. |
| `Navegacao.kt` | Objeto `Rotas`, barra inferior, `NavHost` e `navegarAba()` (evita telas repetidas na pilha e restaura o estado da aba). |
| `Tema.kt` | Paleta de alto contraste, tipografia ampliada, tema claro e escuro. |

### 4.5 Banco Oracle (PL/SQL)

| Objeto | Tipo | Automação |
|---|---|---|
| `FN_ZB_CALCULAR_PONTUACAO` | function | Pontuação de prioridade (igual ao app). |
| `FN_ZB_CLASSIFICAR_PRIORIDADE` | function | Converte a pontuação em ALTA, MEDIA ou BAIXA. |
| `PRC_ZB_REGISTRAR_LOG` | procedure | Grava erros em `T_ZB_LOG_ERRO` com `AUTONOMOUS_TRANSACTION`, então o log é mantido mesmo após ROLLBACK. |
| `PKG_ZELABAIRRO.registrar_ocorrencia` | procedure | Valida a entrada, encontra a categoria e o órgão, gera o protocolo pela sequence e insere. Retorna id e protocolo. |
| `apoiar_ocorrencia` / `remover_apoio` | procedures | Bloqueiam o apoio do autor, apoio duplicado e apoio a ocorrência resolvida. |
| `atualizar_status` | procedure | Usa `SELECT ... FOR UPDATE` e valida a transição com `fn_transicao_valida`. A exceção `e_transicao_invalida` (-20010) pode ser tratada por quem chama. |
| `reabrir_ocorrencia` | procedure | Leva de RESOLVIDA para EM_ANALISE, registrando quem reabriu e o motivo. |
| `listar_ocorrencias` | function → `SYS_REFCURSOR` | **SQL dinâmico** com `OPEN FOR ... USING`. Os filtros são opcionais e passados por bind (sem SQL injection). A ordenação aceita só valores de uma lista fixa. |
| `gerar_indicadores_mensais` | procedure | `MERGE` que consolida, por bairro, categoria e mês: registradas, resolvidas, fora do prazo, apoios, tempo médio e % de resolução. |
| `fn_taxa_resolucao_bairro` | function | % de ocorrências resolvidas no bairro. |
| `relatorio_fora_do_prazo` | procedure | Cursor explícito que lista as pendências que passaram do SLA do órgão. |
| `TRG_ZB_OCORRENCIA_PRIORIDADE` | trigger BEFORE ROW | Recalcula pontuação e prioridade, atualiza `dt_atualizacao` e controla `dt_resolucao`. |
| `TRG_ZB_OCORRENCIA_HISTORICO` | trigger AFTER ROW | Grava a linha do tempo em todo INSERT e em toda mudança de status. |
| `TRG_ZB_APOIO_CONTADOR` | trigger AFTER ROW | Mantém `qt_apoios` em sincronia com `T_ZB_APOIO`, o que dispara o recálculo da prioridade. |

## 5. Boas práticas de gerenciamento de memória e eficiência

1. **Singleton do banco:** existe uma única instância de `ZelaBairroDatabase`, criada com `applicationContext`. Nenhuma Activity fica presa a um objeto de longa duração, o que evita *memory leak*.
2. **Inicialização preguiçosa:** o repositório usa `by lazy`, então o banco só é aberto no primeiro uso e a abertura do app fica mais rápida.
3. **Estado no ViewModel:** os dados sobrevivem à rotação de tela sem recarregar do banco, e `viewModelScope` cancela automaticamente as coroutines quando a tela é destruída.
4. **`stateIn(SharingStarted.WhileSubscribed(5_000))`:** a leitura do banco para 5 segundos depois que a tela sai de cena. O prazo cobre uma rotação, mas não mantém consultas rodando em segundo plano.
5. **`collectAsStateWithLifecycle`:** a UI só coleta os fluxos enquanto está pelo menos em `STARTED`. Com o app em segundo plano, não há recomposição nem consumo de CPU.
6. **Coroutines e Room fora da main thread:** DAOs `suspend` e `Flow` rodam em executores do Room, o que evita travamentos (ANR).
7. **Listas preguiçosas:** `LazyColumn` e `LazyRow` só compõem os itens visíveis. O `key = { it.id }` deixa o Compose reaproveitar itens e redesenhar apenas o que mudou.
8. **Agregação no banco:** os indicadores do painel saem de `COUNT/SUM/AVG/GROUP BY` no SQLite. A lista inteira não é carregada em memória para ser somada em Kotlin.
9. **Índices:** índices em `status`, `categoria`, `protocolo` (Room) e `st_ocorrencia`, `id_categoria`, `id_bairro` (Oracle) aceleram os filtros mais usados.
10. **`debounce` + `flatMapLatest` na busca:** não há uma consulta por tecla digitada, e a consulta anterior é cancelada quando o filtro muda.
11. **Classes imutáveis (`data class` + `copy`) e `StateFlow`:** o Compose detecta mudanças com eficiência e evita recomposições desnecessárias.
12. **Limite de tamanho nos campos** (`take(80)`, `take(500)`): evita strings enormes em memória e no banco.
13. **Sem bibliotecas pesadas:** os gráficos são desenhados com `Box` e só é usado o pacote `material-icons-core`, não o extended de cerca de 30 MB.
14. **R8 no release:** `isMinifyEnabled` e `isShrinkResources` removem código e recursos não utilizados, gerando um APK menor e com menos uso de memória.
15. **No Oracle:** `NOCACHE` na sequence (protocolos sem saltos), cursor `FOR` (abre e fecha sozinho), `CLOSE` explícito do `SYS_REFCURSOR`, `MERGE` em vez de DELETE+INSERT e bind variables, que reaproveitam o plano de execução na shared pool.

## 6. Interface e acessibilidade

- Texto do corpo em 17sp e respeito à escala de fonte do sistema (unidades `sp`).
- Cores com contraste AA. Status e prioridade sempre aparecem **com texto**, nunca só com cor.
- Áreas de toque ≥ 48dp. O interruptor de acessibilidade ocupa a linha inteira (`toggleable` com `Role.Switch`).
- Semântica para TalkBack: `heading()` nos títulos de seção, `mergeDescendants` nos cartões, `stateDescription` no botão de apoio e `contentDescription` nos ícones que agem sozinhos.
- Linguagem simples, sem jargão técnico, e prévia da prioridade e do órgão antes do envio.
- Tema escuro automático.

## 7. Tecnologias adotadas e benefícios

| Tecnologia | Por que foi escolhida |
|---|---|
| **Kotlin** | Linguagem oficial do Android, *null safety* (menos crashes), coroutines nativas e código conciso. |
| **Jetpack Compose + Material 3** | UI declarativa com menos código que XML, temas claro e escuro e componentes acessíveis prontos. |
| **MVVM (ViewModel + StateFlow)** | Separa tela, estado e regra. Facilita testes e sobrevive a mudanças de configuração. |
| **Room** | Camada sobre SQLite com verificação de SQL em tempo de compilação, `Flow` reativo e transações. Permite uso **offline**, importante para quem está na rua com sinal fraco. |
| **Kotlin Flow / Coroutines** | Mantém um canal aberto entre a fonte de dados e a UI. A tela atualiza sozinha após qualquer mudança. |
| **Navigation Compose** | Navegação tipada com argumentos (id da ocorrência) e preservação do estado das abas. |
| **Oracle + PL/SQL** | Banco corporativo usado por órgãos públicos. As regras ficam centralizadas no banco (triggers e package) e valem para qualquer sistema que acesse os dados. Indicadores consolidados apoiam a gestão. |
| **JUnit** | Testes da regra de prioridade, que precisa dar o mesmo resultado no app e no banco. |

## 8. Testes

- `RegraPrioridadeTest` (`./gradlew test`): cobre os limites da prioridade, o teto de apoios e o fluxo de status.
- `database/03_carga_e_testes.sql`: executa o fluxo completo e testa três regras que devem ser bloqueadas (transição inválida, autor apoiando a própria ocorrência e categoria inexistente).

## 9. Evolução futura

API REST (Spring Boot ou Oracle REST Data Services) chamando o `PKG_ZELABAIRRO`, para sincronizar o Room com o Oracle; foto e GPS na ocorrência; notificações push a cada mudança de status; mapa de calor por bairro; painel web para os órgãos públicos.
