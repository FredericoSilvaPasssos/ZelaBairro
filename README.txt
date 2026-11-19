===============================================================================
 ZelaBairro - Zeladoria urbana feita pelo cidadão
 Global Solution 2 - 2026 | CitizenTech: Tecnologia Centrada no Cidadão
 Turma 3SIOA
===============================================================================

INTEGRANTES
-----------
 - NOME COMPLETO 1 ................................. RM 000000
 - NOME COMPLETO 2 ................................. RM 000000
 - NOME COMPLETO 3 ................................. RM 000000

LINKS
-----
 - Repositório GitHub ...... https://github.com/SEU_USUARIO/ZelaBairro
 - Vídeo pitch técnico ..... https://youtu.be/XXXXXXXX
 - Vídeo pitch de venda .... https://youtu.be/XXXXXXXX


1. O PROBLEMA
-------------
Postes apagados, buracos, calçadas quebradas, bueiros entupidos e entulho
descartado afetam a segurança e a mobilidade de milhões de pessoas. Hoje o
cidadão não sabe para qual órgão reclamar, não recebe retorno e não consegue
saber se outras pessoas já reportaram o mesmo problema. Quem mais sofre são
idosos e pessoas com deficiência, para quem uma calçada quebrada é uma barreira.

2. A SOLUÇÃO
------------
ZelaBairro é um aplicativo Android em que o morador registra um problema do
espaço público em menos de um minuto, recebe um protocolo e acompanha cada
etapa do atendimento. Vizinhos afetados pelo mesmo problema "apoiam" a
ocorrência, o que aumenta automaticamente a prioridade. Problemas que afetam a
acessibilidade recebem prioridade maior.

Público beneficiado: moradores de áreas urbanas, com atenção especial a idosos,
pessoas com deficiência e pessoas com mobilidade reduzida.

ODS relacionados:
 - ODS 11 - Cidades e Comunidades Sustentáveis (meta 11.2 e 11.7: espaços
   públicos seguros, inclusivos e acessíveis).
 - ODS 10 - Redução das Desigualdades (prioridade para problemas de acessibilidade).
 - ODS 16 - Instituições Eficazes (transparência e prestação de contas do poder público).

3. FUNCIONALIDADES PRINCIPAIS (sem contar login/cadastro)
---------------------------------------------------------
 F1. Registro de ocorrência: categoria, endereço, ponto de referência, descrição
     e indicação de impacto na acessibilidade. Gera protocolo, mostra o órgão
     responsável e a prioridade estimada antes do envio.
 F2. Acompanhamento: lista com busca (título, bairro, protocolo), filtros por
     status e categoria, ordenada por prioridade. Tela de detalhe com linha do
     tempo do atendimento e opção de reabrir se o problema não foi resolvido.
 F3. Apoio comunitário ("Também sou afetado"): cada apoio aumenta a pontuação e
     pode elevar a prioridade (Baixa > Média > Alta).
 F4. Painel do bairro: indicadores em tempo real (aguardando, em atendimento,
     resolvidas, apoios, taxa de resolução, tempo médio para resolver,
     pendências por categoria e ocorrências mais apoiadas).

Regra de prioridade (igual no app e no Oracle):
   pontuação = peso da categoria (1 a 4)
             + 3 se afeta acessibilidade
             + número de apoios (máximo 10)
   ALTA >= 10 | MÉDIA >= 6 | BAIXA < 6

4. TECNOLOGIAS
--------------
 - Kotlin 2.0 + Jetpack Compose (Material 3) ... interface declarativa e acessível
 - Arquitetura MVVM (ViewModel + StateFlow) .... separa tela, estado e regra
 - Room (SQLite) + Kotlin Flow ................. banco local reativo, funciona offline
 - Coroutines ................................. operações de banco fora da thread principal
 - Navigation Compose ......................... navegação entre telas com argumentos
 - Oracle Database (ATP) + PL/SQL ............. modelo relacional central e automações
   (package, procedures, functions, triggers, SQL dinâmico, MERGE)

Justificativas em docs/DOCUMENTACAO.md (seção "Tecnologias adotadas").

5. ESTRUTURA DO REPOSITÓRIO
---------------------------
 ZelaBairro/
 |-- README.txt                      este arquivo
 |-- app/                            aplicativo Android (Kotlin)
 |   |-- src/main/java/br/com/fiap/zelabairro/
 |   |   |-- MainActivity.kt         Activity única
 |   |   |-- ZelaBairroApp.kt        Application + container de dependências
 |   |   |-- model/                  enums e regra de prioridade
 |   |   |-- data/local/             Room: entidades, DAO, banco, dados iniciais
 |   |   |-- data/repository/        repositório (regras de negócio)
 |   |   |-- ui/                     telas, ViewModels, componentes, tema, navegação
 |   |-- src/test/                   testes unitários da regra de prioridade
 |-- database/
 |   |-- 01_ddl.sql                  criação das tabelas
 |   |-- 02_plsql.sql                funções, package, triggers
 |   |-- 03_carga_e_testes.sql       carga e demonstração das rotinas
 |-- docs/
     |-- DOCUMENTACAO.md             classes, métodos, memória, tecnologias
     |-- MODELO_DE_DADOS.md          MER/DER explicados + dicionário de dados
     |-- DER_ZelaBairro.png          diagrama lógico (DER)
     |-- MER_ZelaBairro.png          diagrama conceitual (MER)

6. COMO EXECUTAR O APLICATIVO
-----------------------------
Requisitos: Android Studio Koala (2024.1) ou mais recente, JDK 17 (já vem com o
Android Studio), emulador ou celular com Android 8.0 (API 26) ou superior.

 1. Abra o Android Studio > File > Open > selecione a pasta "ZelaBairro".
 2. Aguarde o Gradle Sync baixar as dependências (precisa de internet na 1a vez).
 3. Crie/selecione um emulador (Device Manager) ou conecte o celular com
    "Depuração USB" ativada.
 4. Clique em Run (Shift+F10) com a configuração "app".
 5. Na primeira abertura o banco local é criado com 6 ocorrências de exemplo.

Linha de comando (opcional):
   ./gradlew assembleDebug        -> app/build/outputs/apk/debug/app-debug.apk
   ./gradlew test                 -> executa os testes unitários

Roteiro rápido para testar:
 - Início: veja o painel do bairro e as ocorrências mais apoiadas.
 - Registrar: escolha "Calçada e acessibilidade", ative "Afeta a
   acessibilidade" e veja a prioridade estimada. Envie e anote o protocolo.
 - Ocorrências: busque pelo protocolo, filtre por status/categoria.
 - Detalhe: toque em "Também sou afetado" e veja a prioridade e o painel mudarem.
   Use "Modo demonstração" para simular a prefeitura avançando o status até
   "Resolvida"; depois teste "Não, o problema continua" para reabrir.

7. COMO EXECUTAR OS SCRIPTS ORACLE
----------------------------------
 1. Conecte no Oracle (ATP ou banco da FIAP) pelo SQL Developer.
 2. Ative a saída: Exibir > Saída DBMS > "+" (selecione a conexão).
 3. Execute, nesta ordem, com F5 (Executar Script):
      database/01_ddl.sql
      database/02_plsql.sql
      database/03_carga_e_testes.sql
 4. O script 03 registra ocorrências, gera apoios, muda status, reabre uma
    ocorrência, testa regras que devem ser bloqueadas, gera indicadores,
    executa a consulta dinâmica e o relatório de prazo. As consultas finais
    mostram ocorrências, histórico, indicadores e log de erros.
 Os scripts podem ser executados novamente: o 01 apaga e recria tudo.

8. JUSTIFICATIVAS TÉCNICAS (resumo)
-----------------------------------
 - Offline first: o cidadão registra o problema mesmo sem sinal na rua; os dados
   ficam no Room. A integração com o Oracle seria feita por uma API REST que
   chama as procedures do PKG_ZELABAIRRO (mesmos códigos de categoria e status).
 - Mesma regra nos dois lados: RegraPrioridade.kt e FN_ZB_CALCULAR_PONTUACAO
   produzem a mesma pontuação, testada em RegraPrioridadeTest.
 - Automação no banco: triggers mantêm prioridade, contador de apoios e
   histórico sempre corretos, mesmo se outro sistema alterar os dados.
 - Acessibilidade: textos maiores, alto contraste, áreas de toque >= 48dp,
   descrições para TalkBack, status sempre com texto (não só cor).
 - Memória e desempenho: ver docs/DOCUMENTACAO.md, seção 5.
