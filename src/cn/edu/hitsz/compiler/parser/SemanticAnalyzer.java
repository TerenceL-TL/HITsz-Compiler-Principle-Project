package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.NonTerminal;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;

import java.util.Stack;

public class SemanticAnalyzer implements ActionObserver {

    private SymbolTable symbolTable;
    private Stack<SourceCodeType> semanticTypeStack = new Stack<>();
    private Stack<Token> semanticTokenStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // No action needed on accept
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        int productionIndex = production.index();
        
        if (productionIndex == 4) {
            // Handle declaration: S -> D id
            Token identifierToken = semanticTokenStack.pop();
            semanticTypeStack.pop(); // Pop the null associated with id
            semanticTokenStack.pop(); // Pop the D token
            SourceCodeType declaredType = semanticTypeStack.pop();
            
            // Update symbol table with type information
            SymbolTableEntry entry = symbolTable.get(identifierToken.getText());
            entry.setType(declaredType);
            
            // Push placeholder values for the reduced production
            pushPlaceholders();
        } 
        else if (productionIndex == 5) {
            // Handle type specifier: D -> int
            SourceCodeType type = semanticTypeStack.pop();
            semanticTokenStack.pop();
            
            // Preserve type information
            semanticTokenStack.push(null);
            semanticTypeStack.push(type);
        } 
        else {
            // Handle all other productions
            int bodySize = production.body().size();
            for (int i = 0; i < bodySize; i++) {
                semanticTypeStack.pop();
                semanticTokenStack.pop();
            }
            pushPlaceholders();
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        semanticTokenStack.push(currentToken);
        
        // Only set type for 'int' tokens, null for all others
        if (currentToken.getKind().getIdentifier().equals("int")) {
            semanticTypeStack.push(SourceCodeType.Int);
        } else {
            semanticTypeStack.push(null);
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        this.symbolTable = table;
    }
    
    private void pushPlaceholders() {
        semanticTypeStack.push(null);
        semanticTokenStack.push(null);
    }
}
