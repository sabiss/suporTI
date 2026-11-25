# SuporTI — Sistema de Helpdesk

Sistema de Helpdesk para gerenciamento de chamados de suporte técnico desenvolvido para a disciplina de **Linguagem de Programação II (LP2)** do **Instituto Metrópole Digital (IMD/UFRN)** sob orientação do Prof. Mateus Alves de Oliveira.

---

## 👥 Equipe e Divisão de Responsabilidades

| Integrante | Responsabilidades Principais | Atividades Complementares |
|------------|------------------------------|---------------------------|
| **Sabrina** | **Model e Factory**: `Chamado`, `Incidente` e `Requisicao`; enums; regras de status e prazo. | **Diagrama de classes e testes do model.** |
| **Paula** | **Repository, Service e filtros**: SQLite, `schema.sql`, `JdbcTemplate` e `ChamadoService`. | Diagramas de sequência, README e testes dos filtros. |
| **Gabriel** | **Controller e templates**: rotas, Thymeleaf, CSS e `TratadorDeErros`. | Slides, roteiro da demonstração e JavaDoc. |

---

## 🚀 Requisitos e Tecnologias

- **Linguagem:** Java 25 (OpenJDK 25)
- **Framework:** Spring Boot 4.1.2 (Spring Web MVC, Thymeleaf)
- **Persistência / Banco de Dados:** SQLite via Spring JDBC (`JdbcTemplate`)
- **Testes Unitários:** JUnit 5 (Jupiter)
- **Gerenciador de Dependências:** Apache Maven (com Maven Wrapper `./mvnw`)

---

## 🏗️ Conceitos de POO e Padrões Aplicados

1. **Abstração e Herança:**
   - Classe abstrata `Chamado` como modelo base;
   - Subclasses concretas `Incidente` (falhas operacionais) e `Requisicao` (pedidos de serviço).
2. **Polimorfismo e Interfaces:**
   - Interface `SlaCalculavel` implementada polimorficamente por `Incidente` e `Requisicao` para cálculo dinâmico de prazos de atendimento (SLA) e detecção de urgência imediata.
   - Interface `Comparable<Chamado>` para ordenação natural da fila de tarefas do técnico por prioridade (Alta > Média > Baixa) e menor prazo de vencimento.
3. **Encapsulamento e Regras de Domínio:**
   - Atributos estritamente privados com validações em construtores e setters;
   - Máquina de estados controlada por métodos de negócio (`iniciarAtendimento()`, `concluir()`, `cancelar()`, `classificarPrioridade()`), lançando exceções personalizadas (`TransicaoStatusInvalidaException`, `DadosChamadoInvalidosException`).
4. **Padrão de Projeto (Design Pattern):**
   - **Factory Method / Simple Factory (`ChamadoFactory`):** Centraliza a criação dos objetos polimórficos de chamado, garantindo aplicação uniforme das regras de inicialização, validação de integridade e cálculo de SLA.
   - *Justificativa:* Desacopla as camadas de persistência e apresentação dos construtores internos dos tipos específicos, permitindo adicionar novos tipos de chamados futuramente sem impacto no restante da aplicação.

Consulte a documentação completa e o diagrama UML em: [docs/DIAGRAMA_CLASSES.md](file:///home/gb-franca/suporTI/docs/DIAGRAMA_CLASSES.md).

---

## 🛠️ Como Compilar e Executar

### Pré-requisitos
- JDK 25 instalado (`java -version`)

### Executar os Testes Unitários
```bash
./mvnw clean test
```

### Compilar e Empacotar o Projeto
```bash
./mvnw clean package
```

### Gerar a Documentação JavaDoc
```bash
./mvnw javadoc:javadoc
```
A documentação HTML será gerada em `target/reports/apidocs/index.html`.

### Executar a Aplicação Web
Via Spring Boot:
```bash
./mvnw spring-boot:run
```
Ou executando o JAR gerado:
```bash
java -jar target/suporti-0.0.1-SNAPSHOT.jar
```
A aplicação estará disponível em `http://localhost:8080`.
