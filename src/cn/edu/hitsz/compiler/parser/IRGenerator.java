package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.InstructionKind;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;

public class IRGenerator implements ActionObserver {
    private SymbolTable symbolTable;
    private final List<Instruction> intermediateCode = new ArrayList<>();
    private final Stack<IRValue> operandStack = new Stack<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        switch (currentToken.getKindId()) {
            case "id" -> operandStack.push(IRVariable.named(currentToken.getText()));
            case "IntConst" -> operandStack.push(IRImmediate.of(Integer.parseInt(currentToken.getText())));
            case "int", "return", "=", ",", "Semicolon", "+", "-", "*", "/", "(", ")" -> operandStack.push(null);
            default -> throw new IllegalArgumentException("Invalid token type: " + currentToken.getKindId());
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        switch (production.index()) {
            case 1, 2, 3, 4, 5 -> { 
                // P -> S_list; 
                // S_list -> S Semicolon S_list; 
                // S_list -> S Semicolon; 
                // S -> D id; 
                // D -> int;
                popNElements(production.body().size());
                operandStack.push(null);
            }
            case 6 -> { // S -> id = E;
                processAssignment();
            }
            case 7 -> { // S -> return E;
                processReturn();
            }
            case 8 -> { // E -> E + A;
                processArithmeticOp(InstructionKind.ADD);
            }
            case 9 -> { // E -> E - A;
                processArithmeticOp(InstructionKind.SUB);
            }
            case 10 -> { // E -> A;
                // No action needed
            }
            case 11 -> { // A -> A * B;
                processArithmeticOp(InstructionKind.MUL);
            }
            case 12 -> { // A -> B;
                // No action needed
            }
            case 13 -> { // B -> ( E );
                processParentheses();
            }
            case 14, 15 -> { // B -> id; B -> IntConst;
                IRValue value = operandStack.pop();
                operandStack.push(value);
            }
            default -> throw new IllegalStateException("Unknown production index: " + production.index());
        }
    }

    private void processAssignment() {
        IRValue expression = operandStack.pop();
        operandStack.pop(); // Pop '=' token
        IRValue target = operandStack.pop();
        operandStack.push(null);
        
        intermediateCode.add(Instruction.createMov((IRVariable) target, expression));
    }

    private void processReturn() {
        IRValue returnValue = operandStack.pop();
        operandStack.pop(); // Pop 'return' token
        operandStack.push(null);
        
        intermediateCode.add(Instruction.createRet(returnValue));
    }

    private void processArithmeticOp(InstructionKind kind) {
        IRValue rightOperand = operandStack.pop();
        operandStack.pop(); // Pop operator token
        IRValue leftOperand = operandStack.pop();
        
        IRVariable tempResult = IRVariable.temp();
        
        Instruction instruction = switch (kind) {
            case ADD -> Instruction.createAdd(tempResult, leftOperand, rightOperand);
            case SUB -> Instruction.createSub(tempResult, leftOperand, rightOperand);
            case MUL -> Instruction.createMul(tempResult, leftOperand, rightOperand);
            default -> throw new IllegalStateException("Unsupported operation: " + kind);
        };
        
        intermediateCode.add(instruction);
        operandStack.push(tempResult);
    }

    private void processParentheses() {
        operandStack.pop(); // Pop ')'
        IRValue expression = operandStack.pop();
        operandStack.pop(); // Pop '('
        operandStack.push(expression);
    }

    private void popNElements(int n) {
        for (int i = 0; i < n; i++) {
            operandStack.pop();
        }
    }

    @Override
    public void whenAccept(Status currentStatus) {
        // No action needed for accept
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        return Collections.unmodifiableList(intermediateCode);
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, intermediateCode.stream()
            .map(Instruction::toString)
            .toList());
    }
}
