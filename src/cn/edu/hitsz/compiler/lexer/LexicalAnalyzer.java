package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String content; // 缓冲区存储输入内容
    private final List<Token> tokens; // 存储词法分析后的Token列表

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.tokens = new ArrayList<>();
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // 词法分析前的缓冲区实现
        // 直接读取整个文件内容作为缓冲区
        this.content = FileUtils.readFile(path);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // 初始化DFA，设置状态转换
        DFA dfa = new DFA("START", List.of("IDENTIFIER", "NUMBER", "PLUS", "MINUS", "STAR", "SLASH", "LPAREN", "RPAREN", "SEMICOLON", "ASSIGN"));
        setupDFA(dfa);

        int index = 0;
        StringBuilder buffer = new StringBuilder();
//        System.out.println(content);

        while (index < content.length()) {
            char currentChar = content.charAt(index);
//            System.out.print(currentChar);

            if (Character.isWhitespace(currentChar)) {
                index++;
                continue;
            }

            buffer.append(currentChar);
            dfa.makeTransition(currentChar);

            if (dfa.getCurrentState() == null) {
                throw new RuntimeException("Unexpected character: " + currentChar);
            }

            switch (dfa.getCurrentState()) {
                case "IDENTIFIER":
                    char nextChar = (index + 1 < content.length()) ? content.charAt(index + 1) : '\0';
                    if (!Character.isLetterOrDigit(nextChar) && nextChar != '_') {
                        String identifier = buffer.toString();
                        if (identifier.equals("int"))
                        {
                            tokens.add(Token.simple("int"));
                        }
                        else if (identifier.equals("return"))
                        {
                            tokens.add(Token.simple("return"));
                        }
                        else {
                            if (!symbolTable.has(identifier))
                            {
                                symbolTable.add(identifier);
                            }
                            tokens.add(Token.normal("id", identifier));
                        }
                        dfa.reset();
                        buffer.setLength(0);
                    }
                    break;

                case "NUMBER":
                    char nextNumberChar = (index + 1 < content.length()) ? content.charAt(index + 1) : '\0';
                    if (!Character.isDigit(nextNumberChar)) {
                        tokens.add(Token.normal("IntConst", buffer.toString()));
                        dfa.reset();
                        buffer.setLength(0);
                    }
                    break;

                case "SEMICOLON":
                    tokens.add(Token.simple("Semicolon"));
                    dfa.reset();
                    buffer.setLength(0);
                    break;

                case "PLUS":
                    tokens.add(Token.simple("+"));
                    dfa.reset();
                    buffer.setLength(0);
                    break;

                case "MINUS":
                    tokens.add(Token.simple("-"));
                    dfa.reset();
                    buffer.setLength(0);
                    break;

                case "STAR":
                    tokens.add(Token.simple("*"));
                    dfa.reset();
                    buffer.setLength(0);
                    break;

                case "SLASH":
                    tokens.add(Token.simple("/"));
                    dfa.reset();
                    buffer.setLength(0);
                    break;

                case "LPAREN":
                    tokens.add(Token.simple("("));
                    dfa.reset();
                    buffer.setLength(0);
                    break;

                case "RPAREN":
                    tokens.add(Token.simple(")"));
                    dfa.reset();
                    buffer.setLength(0);
                    break;

                case "ASSIGN":
                    tokens.add(Token.simple("="));
                    dfa.reset();
                    buffer.setLength(0);
                    break;

                default:
                    break;
            }

            index++;
        }

        // 添加EOF标记
        tokens.add(Token.eof());
    }

    private void setupDFA(DFA dfa) {
        // 初始化DFA的状态转换
        dfa.addTransition("START", ';', "SEMICOLON");
        dfa.addTransition("START", '+', "PLUS");
        dfa.addTransition("START", '-', "MINUS");
        dfa.addTransition("START", '*', "STAR");
        dfa.addTransition("START", '/', "SLASH");
        dfa.addTransition("START", '(', "LPAREN");
        dfa.addTransition("START", ')', "RPAREN");
        dfa.addTransition("START", '=', "ASSIGN");

        // 添加标识符和数字的处理
        for (char c = 'a'; c <= 'z'; c++) {
            dfa.addTransition("START", c, "IDENTIFIER");
            dfa.addTransition("IDENTIFIER", c, "IDENTIFIER");
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            dfa.addTransition("START", c, "IDENTIFIER");
            dfa.addTransition("IDENTIFIER", c, "IDENTIFIER");
        }
        dfa.addTransition("START", '_', "IDENTIFIER");
        dfa.addTransition("IDENTIFIER", '_', "IDENTIFIER");

        for (char c = '0'; c <= '9'; c++) {
            dfa.addTransition("START", c, "NUMBER");
            dfa.addTransition("NUMBER", c, "NUMBER");
        }
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }
}
