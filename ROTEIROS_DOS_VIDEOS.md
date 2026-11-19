# Roteiros dos vídeos: ZelaBairro

Regras da entrega: publicar no YouTube como **Não listado**, colar o link no `.txt` correspondente e mostrar **rosto e voz** de pelo menos um integrante.

---

## Vídeo 1: Pitch de venda (até 3 min, concorre ao prêmio, não vale nota)

| Tempo | Cena | Fala (sugestão) |
|---|---|---|
| 0:00–0:20 | Integrante na câmera. Se possível, gravar na rua, perto de um poste ou de uma calçada quebrada. | "Quantas vezes você passou por um poste apagado, um buraco ou uma calçada quebrada e pensou: alguém já deve ter reclamado? Pois é. Quase ninguém reclama, porque não sabe para quem reclamar, nem se adianta." |
| 0:20–0:50 | Câmera ou slide com o problema | "Para um cadeirante, uma calçada quebrada não é incômodo, é uma barreira. Para quem volta do ponto de ônibus à noite, uma rua escura é insegurança. E quando alguém reclama, não tem protocolo, não tem retorno e não sabe se outros vizinhos passam pelo mesmo problema." |
| 0:50–1:10 | Logo / tela inicial do app | "Por isso criamos o ZelaBairro: a zeladoria do bairro feita por quem vive nele." |
| 1:10–2:00 | Tela do celular gravada (registrar → protocolo → apoiar → painel) | "Em menos de um minuto você registra o problema, escolhe a categoria e o app já mostra qual órgão é responsável. Você recebe um protocolo e acompanha cada etapa, como um rastreio de encomenda. Os vizinhos afetados tocam em 'Também sou afetado' e a prioridade sobe. Problemas que atrapalham pessoas com deficiência e idosos ganham prioridade automaticamente." |
| 2:00–2:30 | Painel do bairro | "O painel mostra quantos problemas estão abertos, quantos foram resolvidos e quanto tempo a prefeitura leva. É transparência para o cidadão e informação para o gestor decidir onde agir primeiro." |
| 2:30–2:50 | Integrante na câmera | "O ZelaBairro contribui com o ODS 11, Cidades e Comunidades Sustentáveis, e com o ODS 10, Redução das Desigualdades: espaços públicos mais seguros e acessíveis para todos." |
| 2:50–3:00 | Equipe / logo | "ZelaBairro: quem vive o bairro, cuida do bairro." |

---

## Vídeo 2: Pitch técnico (até 5 min, vale nota)

Prepare o emulador já aberto no app, o Android Studio com o projeto e o SQL Developer com os scripts executados.

| Tempo | O que mostrar | Fala (sugestão) |
|---|---|---|
| 0:00–0:25 | Integrante na câmera | "Somos [nomes], da turma 3SIOA. Este é o pitch técnico do ZelaBairro, um app em Kotlin para registrar e acompanhar problemas em espaços públicos." |
| 0:25–1:00 | Android Studio: árvore de pastas | "Usamos Kotlin com Jetpack Compose e arquitetura MVVM. Em `model` está o domínio e a regra de prioridade. Em `data` ficam o banco Room e o repositório. Em `ui` ficam as telas, os ViewModels e os componentes. Há uma única Activity, e a navegação é feita com Navigation Compose." |
| 1:00–1:40 | App: **F1 Registrar** | "Primeira funcionalidade: o registro. Escolho 'Calçada e acessibilidade', preencho o local e ativo 'Afeta a acessibilidade'. O app mostra o órgão responsável e a prioridade estimada. Ao enviar, o repositório gera o protocolo e grava a ocorrência e o histórico em uma única transação Room." |
| 1:40–2:15 | App: **F2 Acompanhar** | "Segunda funcionalidade: o acompanhamento. Busca por protocolo, filtros por status e categoria. A lista é um Flow do Room: qualquer mudança no banco aparece na tela sem recarregar. Usamos debounce para não consultar a cada tecla. No detalhe aparece a linha do tempo." |
| 2:15–2:45 | App: **F3 Apoiar** | "Terceira funcionalidade: o apoio comunitário. Ao tocar em 'Também sou afetado', a pontuação é recalculada: peso da categoria, mais 3 se afeta acessibilidade, mais os apoios até 10. Com 10 pontos ou mais, vira prioridade alta." Mostrar o modo demonstração avançando até "Resolvida" e o botão de reabrir. |
| 2:45–3:10 | App: **F4 Painel** | "Quarta funcionalidade: o painel do bairro. Os indicadores são calculados com COUNT, SUM e AVG direto no SQLite, sem carregar a lista inteira em memória." |
| 3:10–3:40 | Código: `InicioViewModel`, `ZelaBairroDatabase` | "Sobre memória: o banco é um Singleton com applicationContext, então não há vazamento de Activity. Os ViewModels usam stateIn com WhileSubscribed de 5 segundos, e a tela coleta com collectAsStateWithLifecycle, que só escuta enquanto está visível. Listas usam LazyColumn com chave estável, e no release o R8 reduz o APK." |
| 3:40–4:40 | SQL Developer: DER + `02_plsql.sql` + saída do `03` | "No Oracle modelamos 9 tabelas; aqui está o DER. A automação fica no package PKG_ZELABAIRRO: a procedure de registro gera o protocolo pela sequence, a de status valida as transições e a listagem usa SQL dinâmico com OPEN FOR e bind variables. A geração de indicadores faz um MERGE mensal. Três triggers recalculam a prioridade, mantêm o contador de apoios e gravam o histórico automaticamente. Erros vão para uma tabela de log com transação autônoma." Mostrar a saída DBMS com os testes "[OK] Bloqueado" e o relatório fora do prazo. |
| 4:40–5:00 | Integrante na câmera | "A mesma regra de prioridade existe no app e no banco, coberta por testes unitários. O próximo passo é uma API REST ligando o app ao Oracle. Obrigado!" |

### Dicas de gravação

- Use OBS Studio ou a gravação de tela do Windows (Win+Alt+R) e grave a câmera junto.
- Ensaie uma vez com cronômetro. Cada vídeo precisa ficar **dentro** do limite.
- Aumente a fonte do Android Studio e do SQL Developer (Ctrl + roda do mouse) para ficar legível no vídeo.
