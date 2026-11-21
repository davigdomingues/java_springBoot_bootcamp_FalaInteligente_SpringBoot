package io.github.davigdomingues.falainteligente.domain.enums;

import java.util.Objects;

/**
 * Categorias financeiras suportadas pelo domínio do orçamento inteligente.
 *
 * Estrutura e impacto: centraliza o catálogo semântico utilizado tanto pelos endpoints REST
 * quanto pelo prompt do sistema e pelas ferramentas de Tool Calling da IA, fornecendo rótulos
 * amigáveis em português brasileiro para respostas em texto e síntese de voz (TTS).
 */
public enum Category {

    GROCERIES("Supermercado e Mercearia", "Compras de supermercado, feira, hortifrúti e padaria"),
    PHARMA("Farmácia e Drogaria", "Medicamentos, itens de farmácia e perfumaria"),
    AUTO("Transporte e Automotivo", "Combustível, manutenção veicular, estacionamento, pedágio e corridas por app"),
    FOOD_DELIVERY("Alimentação e Delivery", "Restaurantes, lanchonetes, cafés e entregas de comida"),
    HOUSING("Moradia", "Aluguel, condomínio, IPTU e manutenção residencial"),
    UTILITIES("Contas e Serviços", "Energia elétrica, água, gás, internet e telefonia"),
    LEISURE("Lazer e Entretenimento", "Cinema, viagens, streaming, eventos e hobbies"),
    HEALTH("Saúde e Bem-estar", "Consultas médicas, exames, planos de saúde e academia"),
    EDUCATION("Educação", "Cursos, livros, mensalidades acadêmicas e treinamentos"),
    SALARY("Salário e Remuneração", "Salário mensal, férias, décimo terceiro e bonificações"),
    FREELANCE("Freelance e Serviços", "Receitas extras de projetos autônomos e consultorias"),
    INVESTMENTS("Investimentos e Rendimentos", "Dividendos, juros, resgates e aplicações financeiras"),
    OTHER("Outros", "Transações gerais que não se enquadram nas demais categorias");

    private final String displayNamePtBr;
    private final String description;

    Category(String displayNamePtBr, String description) {
        this.displayNamePtBr = Objects.requireNonNull(displayNamePtBr, "displayNamePtBr must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
    }

    public String getDisplayNamePtBr() {
        return displayNamePtBr;
    }

    public String getDescription() {
        return description;
    }
}
