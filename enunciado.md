# Desafio de Projeto: API Inteligente com Spring Boot e Spring AI (DIO / Bootcamp Itaú) -> transcrição do enunciado

---

## 1. Entendendo o Desafio

Agora é a hora de praticar, aprender fazendo e evoluir um projeto completo com **Spring Boot** e **Inteligência Artificial**. Neste desafio, o objetivo é trabalhar com uma **API Inteligente** desenvolvida com **Spring AI**, demonstrando como integrar recursos generativos a uma aplicação Java real mantendo uma arquitetura organizada, testável e pronta para evoluir.

---

## 2. O Que Criar

O desafio propõe evoluir uma API de orçamento pessoal que utiliza IA para processar comandos de voz relacionados a transações financeiras. O fluxo principal consiste em:

1. Receber um arquivo de áudio enviado pelo cliente;
2. Transformar o áudio em texto (*Speech-to-Text*);
3. Usar IA para entender a intenção do comando em linguagem natural;
4. Executar uma função real da aplicação via *Tool Calling*;
5. Criar ou consultar transações financeiras persistidas em banco de dados;
6. Gerar uma resposta final (textual e falada via *Text-to-Speech*) para a pessoa usuária.

> O foco não é apenas usar IA, mas entender como conectar IA a uma aplicação real respeitando as responsabilidades do código e a organização do projeto.

---

## 3. Como Fazer

Temas centrais abordados ao longo da trilha:

- Configuração do Spring AI;
- Integração com modelos de linguagem (LLMs);
- Uso do `ChatClient`;
- *Tool Calling* para executar funções reais de negócio;
- Transcrição de áudio em texto (*TranscriptionModel*);
- Geração de voz a partir de texto (*TextToSpeechModel*);
- Persistência de dados relacional;
- Exposição de endpoints REST;
- Evolução e auditoria da API.

---

## 4. Ideias Para Evoluir

- Adicionar novos tipos de consulta financeira (resumos, ranking por categoria, relatórios mensais);
- Melhorar as respostas geradas pela IA com engenharia de prompt estruturada;
- Criar novas ferramentas (`@Tool`) para o *Tool Calling*;
- Adicionar validações de domínio e de arquivos de áudio antes de acionar modelos externos;
- Melhorar os endpoints REST com versionamento, DTOs imutáveis e documentação OpenAPI/Swagger;
- Criar testes automatizados (unitários e de integração) para os principais fluxos;
- Documentar claramente arquitetura, fluxos, execução e aprendizados.

---

## 5. O Que Entregar e Resultado Esperado

Disponibilizar o projeto em repositório próprio no GitHub com `README.md` explicando:

- O que o projeto faz;
- Como executar a aplicação;
- Quais melhorias foram implementadas;
- Quais tecnologias foram usadas;
- Como testar o fluxo principal;
- O que foi aprendido durante o desafio.

---

## 6. Links Oficiais da Trilha (DIO)

- **Repositório com todos os conteúdos sobre Spring:** [digitalinnovationone/dio-spring-boot-learning-track](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)
- **Pasta com detalhes do projeto base:** [05-spring-ai/README.md](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/blob/main/05-spring-ai/README.md)

---

## 7. Solução Desenvolvida Neste Repositório

- **Abordagem escolhida:** Evoluir e modernizar do zero com **Java 25 + Spring Boot 4.1.2 + Spring AI 2.0.3**
- **Documentação principal:** [`README.md`](README.md)

### Domínio e Arquitetura: Fala Inteligente (Assistente Financeiro por Voz)

API REST modular construída sob **Clean Architecture / Arquitetura Hexagonal + DDD**, separando o núcleo de domínio puro (`domain`), casos de uso de aplicação (`application`) e adaptadores de infraestrutura (`infrastructure`):

1. **Núcleo de Domínio e Orçamento:** entidades e value objects imutáveis com Java `record`s, `BigDecimal` com arredondamento bancário `HALF_EVEN`, 13 categorias financeiras semânticas, separação entre receitas (`INCOME`) e despesas (`EXPENSE`), rastreamento de origem (`REST`, `VOICE_AI`, `TEXT_AI`) e relatórios analíticos (resumo financeiro, detalhamento percentual por categoria e relatório mensal).
2. **Spring AI 2.0 Multimodal:** transcrição de áudio via `TranscriptionModel` (`whisper-1`, preservando `filename` em `ByteArrayResource`), raciocínio e orquestração via `ChatClient` + `ToolCallingAdvisor` com 6 ferramentas `@Tool` declarativas, prompt externalizado em `system-message.st` com data dinâmica (`{currentDate}`) e síntese de voz via `TextToSpeechModel` (`gpt-4o-mini-tts`).
3. **Auditoria, Observabilidade e Modo Demo Offline:** persistência transacional independente (`REQUIRES_NEW`) de todas as interações (`SUCCESS` e `FAILED`) com latências por estágio, Flyway migrations compatíveis com **H2** e **PostgreSQL 17**, Swagger UI (`/swagger-ui.html`), Spring Boot Actuator e perfil `@Profile("demo")` determinístico para execução completa sem chave da OpenAI.
