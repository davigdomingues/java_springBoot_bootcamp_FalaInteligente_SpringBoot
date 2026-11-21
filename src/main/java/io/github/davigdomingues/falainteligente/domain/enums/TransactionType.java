package io.github.davigdomingues.falainteligente.domain.enums;

import java.util.Objects;

/**
 * Classificação contábil da movimentação financeira no orçamento pessoal.
 *
 * Estrutura e impacto: separa saídas ({@link #EXPENSE}) de entradas ({@link #INCOME}) para
 * cálculo consistente de saldo líquido, ticket médio de despesas e agregação por categoria.
 */
public enum TransactionType {

    EXPENSE("Despesa"),
    INCOME("Receita");

    private final String displayNamePtBr;

    TransactionType(String displayNamePtBr) {
        this.displayNamePtBr = Objects.requireNonNull(displayNamePtBr, "displayNamePtBr must not be null");
    }

    public String getDisplayNamePtBr() {
        return displayNamePtBr;
    }
}
