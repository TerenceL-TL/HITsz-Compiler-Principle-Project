package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRVariable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private List<Instruction> instructions;
    private Map<String, Integer> var;
    private Map<Integer, String> reg;
    private StringBuilder assemblyCode;

    public AssemblyGenerator() {
        this.var = new HashMap<>();
        this.reg = new HashMap<>();
        this.assemblyCode = new StringBuilder();
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        this.instructions = originInstructions;
    }

    private int getReg(String varName) {
        if (var.get(varName) == null) {
            for (int i = 5; i < 8; i++) { // t0-t2
                if (reg.get(i) == null) {
                    var.put(varName, i);
                    reg.put(i, varName);
                    return i;
                }
            }
            for (int i = 10; i < 18; i++) { // a0-a7
                if (reg.get(i) == null) {
                    var.put(varName, i);
                    reg.put(i, varName);
                    return i;
                }
            }
            for (int i = 28; i < 32; i++) { // t3-t6
                if (reg.get(i) == null) {
                    var.put(varName, i);
                    reg.put(i, varName);
                    return i;
                }
            }
            // If no free register is available, steal a register from a variable that is no longer used
            for (Map.Entry<String, Integer> entry : var.entrySet()) {
                String variable = entry.getKey();
                Integer register = entry.getValue();
                if (!isVariableUsed(variable)) {
                    var.remove(variable);
                    reg.remove(register);
                    var.put(varName, register);
                    reg.put(register, varName);
                    return register;
                }
            }
            throw new RuntimeException("No available register");
        } else {
            return var.get(varName);
        }
    }

    private boolean isVariableUsed(String varName) {
        for (Instruction ins : instructions) {
            for (IRValue operand : ins.getOperands()) {
                if (operand.toString().equals(varName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getRegName(int regNumber) {
        if (regNumber >= 5 && regNumber <= 7) {
            return "t" + (regNumber - 5);
        } else if (regNumber >= 10 && regNumber <= 17) {
            return "a" + (regNumber - 10);
        } else if (regNumber >= 28 && regNumber <= 31) {
            return "t" + (regNumber - 25);
        } else {
            throw new IllegalArgumentException("Invalid register number: " + regNumber);
        }
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        assemblyCode.append(".text\n");
        for (Instruction ins : instructions) {
            System.out.println(ins);
            boolean hasReturn = false;
            switch (ins.getKind()) {
                case ADD -> generateAdd(ins);
                case SUB -> generateSub(ins);
                case MUL -> generateMul(ins);
                case MOV -> generateMov(ins);
                case RET -> {
                    generateRet(ins);
                    hasReturn = true;
                }
                default -> throw new UnsupportedOperationException("Operation not supported: " + ins.getKind());
            }
            if (hasReturn)
                break;
        }
    }

    private void generateAdd(Instruction ins) {
        List<IRValue> operands = ins.getOperands();
        if (operands.size() < 2) {
            throw new IllegalArgumentException("ADD instruction requires at least 2 operand");
        }
        String destReg = getRegName(getReg(ins.getResult().toString()));
        String src1 = operands.get(0).isImmediate() ? operands.get(0).toString() : getRegName(getReg(operands.get(0).toString()));
        String src2 = operands.get(1).isImmediate() ? operands.get(1).toString() : getRegName(getReg(operands.get(1).toString()));
        
        if (operands.get(0).isImmediate() && operands.get(1).isImmediate()) {
            throw new IllegalArgumentException("ADD instruction cannot have both operands as immediate values");
        } else if (operands.get(0).isImmediate()) {
            assemblyCode.append(String.format("    addi %s, %s, %s\n", destReg, src2, src1));
        } else if (operands.get(1).isImmediate()) {
            assemblyCode.append(String.format("    addi %s, %s, %s\n", destReg, src1, src2));
        } else {
            assemblyCode.append(String.format("    add %s, %s, %s\n", destReg, src1, src2));
        }
    }

    private void generateSub(Instruction ins) {
        List<IRValue> operands = ins.getOperands();
        if (operands.size() < 2) {
            throw new IllegalArgumentException("SUB instruction requires at least 2 operand");
        }
        String destReg = getRegName(getReg(ins.getResult().toString()));
        String src1 = operands.get(0).isImmediate() ? operands.get(0).toString() : getRegName(getReg(operands.get(0).toString()));
        String src2 = operands.get(1).isImmediate() ? operands.get(1).toString() : getRegName(getReg(operands.get(1).toString()));
        if (operands.get(0).isImmediate()) {
            String tempReg = getRegName(getReg("temp"));
            assemblyCode.append(String.format("    li %s, %s\n", tempReg, src1));
            assemblyCode.append(String.format("    sub %s, %s, %s\n", destReg, tempReg, src2));
        } else {
            assemblyCode.append(String.format("    sub %s, %s, %s\n", destReg, src1, src2));
        }
    }

    private void generateMul(Instruction ins) {
        List<IRValue> operands = ins.getOperands();
        if (operands.size() < 2) {
            throw new IllegalArgumentException("MUL instruction requires at least 2 operand");
        }
        String destReg = getRegName(getReg(ins.getResult().toString()));
        String src1 = operands.get(0).isImmediate() ? operands.get(0).toString() : getRegName(getReg(operands.get(0).toString()));
        String src2 = operands.get(1).isImmediate() ? operands.get(1).toString() : getRegName(getReg(operands.get(1).toString()));
        if (operands.get(0).isImmediate()) {
            String tempReg = getRegName(getReg("temp"));
            assemblyCode.append(String.format("    li %s, %s\n", tempReg, src1));
            assemblyCode.append(String.format("    mul %s, %s, %s\n", destReg, tempReg, src2));
        } else {
            assemblyCode.append(String.format("    mul %s, %s, %s\n", destReg, src1, src2));
        }
    }

    private void generateMov(Instruction ins) {
        List<IRValue> operands = ins.getOperands();
        if (operands.size() < 1) {
            throw new IllegalArgumentException("MOV instruction requires at least 1 operand");
        }
        String destReg = getRegName(getReg(ins.getResult().toString()));
        String src = operands.get(0).isImmediate() ? operands.get(0).toString() : getRegName(getReg(operands.get(0).toString()));
        if (operands.get(0).isImmediate()) {
            assemblyCode.append(String.format("    li %s, %s\n", destReg, src));
        } else {
            assemblyCode.append(String.format("    mv %s, %s\n", destReg, src));
        }
    }

    private void generateRet(Instruction ins) {
        List<IRValue> operands = ins.getOperands();
        if (!operands.isEmpty()) {
            String returnReg = getRegName(getReg(operands.get(0).toString()));
            assemblyCode.append(String.format("    mv a0, %s\n", returnReg));
        }
        // assemblyCode.append("ret\n");
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(assemblyCode.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
