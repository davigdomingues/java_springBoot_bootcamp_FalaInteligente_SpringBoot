package io.github.davigdomingues.falainteligente.application.service;

import io.github.davigdomingues.falainteligente.domain.enums.TransactionSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contexto escopado por thread para rastrear origem ({@link TransactionSource}) e ferramentas
 * {@code @Tool} executadas durante uma mesma chamada ao assistente financeiro.
 *
 * Estrutura e impacto: utiliza {@link ThreadLocal} encapsulado por {@link Scope} ({@link AutoCloseable})
 * para capturar com exatidão quais ferramentas o LLM acionou no loop do {@code ToolCallingAdvisor}
 * sem poluir as assinaturas dos parâmetros das ferramentas nem vazar estado entre requisições.
 */
public final class ToolInvocationContext {

    private static final ThreadLocal<ContextState> CURRENT = new ThreadLocal<>();

    private ToolInvocationContext() {
    }

    public static Scope open(TransactionSource source) {
        Objects.requireNonNull(source, "source must not be null");
        ContextState previous = CURRENT.get();
        ContextState next = new ContextState(source);
        CURRENT.set(next);
        return new Scope(previous, next);
    }

    public static void recordTool(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return;
        }
        ContextState state = CURRENT.get();
        if (state != null) {
            state.invokedTools.add(toolName);
        }
    }

    public static TransactionSource currentSourceOrDefault(TransactionSource fallback) {
        ContextState state = CURRENT.get();
        return state != null ? state.source : fallback;
    }

    public static List<String> currentToolsSnapshot() {
        ContextState state = CURRENT.get();
        return state != null ? List.copyOf(state.invokedTools) : List.of();
    }

    private static final class ContextState {
        private final TransactionSource source;
        private final List<String> invokedTools = new ArrayList<>();

        private ContextState(TransactionSource source) {
            this.source = source;
        }
    }

    /**
     * Escopo gerenciado por try-with-resources que restaura o contexto anterior ao finalizar.
     */
    public static final class Scope implements AutoCloseable {
        private final ContextState previous;
        private final ContextState active;

        private Scope(ContextState previous, ContextState active) {
            this.previous = previous;
            this.active = active;
        }

        public List<String> invokedTools() {
            return List.copyOf(active.invokedTools);
        }

        @Override
        public void close() {
            if (previous != null) {
                CURRENT.set(previous);
            } else {
                CURRENT.remove();
            }
        }
    }
}
