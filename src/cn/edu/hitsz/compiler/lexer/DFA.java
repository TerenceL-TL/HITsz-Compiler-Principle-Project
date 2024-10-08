package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.StreamSupport;

// 一个泛型DFA

public class DFA {
    private final Map<String, Map<Character, String>> transitions;
    private String currentState;
    private final String startState;
    private final List<String> acceptingStates;

    public DFA(String startState, List<String> acceptingStates) {
        this.startState = startState;
        this.currentState = startState;
        this.acceptingStates = acceptingStates;
        this.transitions = new HashMap<>();
    }

    public void addTransition(String fromState, char symbol, String toState) {
        transitions.putIfAbsent(fromState, new HashMap<>());
        transitions.get(fromState).put(symbol, toState);
    }

    public void reset() {
        currentState = startState;
    }

    public void makeTransition(char symbol) {
        if (transitions.containsKey(currentState) &&
                transitions.get(currentState).containsKey(symbol)) {
            currentState = transitions.get(currentState).get(symbol);
        } else {
            currentState = null; // 或者抛出异常表示无效输入
        }
    }

    public boolean isAccepting() {
        return acceptingStates.contains(currentState);
    }

    public String getCurrentState() {
        return currentState;
    }
}