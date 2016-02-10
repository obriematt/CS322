// This is supporting software for CS321/CS322 Compilers and Language Design.
// Copyright (c) Portland State University
//---------------------------------------------------------------------------
// For CS322 W'16 (J. Li).
//

// IR1 code generator.
//
//
import java.util.*;
import java.io.*;
import ast.*;
import ir.*;

class IR1Gen {

  static class GenException extends Exception {
    public GenException(String msg) { super(msg); }
  }

  // For returning <src,code> pair from gen routines
  //
  static class CodePack {
    IR1.Src src;
    List<IR1.Inst> code;
    CodePack(IR1.Src src, List<IR1.Inst> code) { 
      this.src=src; this.code=code; 
    }
  }

  // From the hints on the PDF. Using a secondary class 
  // for accessing the addresses. This should make the 
  // array access much simpler

  // From Slide IR Code Gen 27

  static CodePack genAddr(Ast1.ArrayElm n) throws Exception {
  
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    CodePack ar = gen(n.ar);
    CodePack idx = gen(n.idx);
    IR1.Temp t1 = new IR1.Temp();
    IR1.Temp t2 = new IR1.Temp();
    code.addAll(ar.code);
    code.addAll(idx.code);

    IR1.IntLit ints = new IR1.IntLit(4);

    code.add(new IR1.Binop(IR1.AOP.MUL, t1, idx.src, ints));
    code.add(new IR1.Binop(IR1.AOP.ADD, t2, ar.src, t1));
    return new CodePack(t2,code);
  }



  // The main routine
  //
  public static void main(String [] args) throws Exception {
    if (args.length == 1) {
      FileInputStream stream = new FileInputStream(args[0]);
      Ast1.Program p = new Ast1Parser(stream).Program();
      stream.close();
      IR1.Program ir = IR1Gen.gen(p);
      System.out.print(ir.toString());
    } else {
      System.out.println("You must provide an input file name.");
    }
  }

  // Ast1.Program ---
  // Ast1.Func[] funcs;
  //
  // AG:
  //   code: stmts.c  -- append all individual stmt.c
  //
  public static IR1.Program gen(Ast1.Program n) throws Exception {
    List<IR1.Func> functions = new ArrayList<IR1.Func>();
    for (Ast1.Func f: n.funcs)
      functions.add(gen(f));
    return new IR1.Program(functions);
  }



  // Ast1.Func ---
  // Type t 
  // String nm
  // Param[] params
  // VarDecl[] vars
  // Stmt[] stmts
 
  // IR1.Func ---
  // Global gname
  // Id[] params
  // Id[] locals
  // Inst[] code
  static IR1.Func gen(Ast1.Func n) throws Exception {
    
    //Initial lists needed for each function
    List<IR1.Inst> codeList = new ArrayList<IR1.Inst>();
    List<IR1.Id> paramList = new ArrayList<IR1.Id>();
    List<IR1.Id> localList = new ArrayList<IR1.Id>();    
    IR1.Global gname = new IR1.Global("_" + n.nm);
    //Building the Function
    // 
    //Collecting Params
    for(Ast1.Param p: n.params) {
      paramList.add(gen(p));
    }

    //Generating the code for these VarDecl
    for(Ast1.VarDecl v: n.vars) {
      if(v.init !=null){
        codeList.addAll(gen(v));
      }
      localList.add(new IR1.Id(v.nm));
    }

    //Generating the code for the Stms
    for(Ast1.Stmt s: n.stmts) {
      codeList.addAll(gen(s));
    }

    //Checking for a return statement in the Stmts
    if(n.t == null){
      codeList.add(new IR1.Return());
    }

    return new IR1.Func(gname, paramList, localList, codeList);
 


  }
  
  // Gen for Params to return ID types
  static IR1.Id gen(Ast1.Param n) throws Exception {
    return new IR1.Id(n.nm);
  }

  static List<IR1.Inst> gen(Ast1.VarDecl n) throws Exception {
    
    //Handles the list of code
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    IR1.Id id = new IR1.Id(n.nm);
    
    CodePack init = gen(n.init);
    IR1.Move move = new IR1.Move(id, init.src);
    code.addAll(init.code);
    code.add(move);
   
    return code;
  }



  // STATEMENTS
  //
  // Needs work
  // Add instanceof for every Ast1 thing from Ast1.java
  static List<IR1.Inst> gen(Ast1.Stmt n) throws Exception {
    if (n instanceof Ast1.Block)    return gen((Ast1.Block) n);
    if (n instanceof Ast1.Assign)   return gen((Ast1.Assign) n);
    if (n instanceof Ast1.CallStmt) return gen((Ast1.CallStmt) n);
    if (n instanceof Ast1.If)       return gen((Ast1.If) n);
    if (n instanceof Ast1.While)    return gen((Ast1.While) n);
    if (n instanceof Ast1.Print)    return gen((Ast1.Print) n);
    if (n instanceof Ast1.Return)   return gen((Ast1.Return) n);
    throw new GenException("Unknown Stmt: " + n);
  }


  // Ast1.Block ---
  // Ast1.Stmt[] stmts;
  //
  // AG:
  //   code: {stmt.c}
  //
  static List<IR1.Inst> gen(Ast1.Block n) throws Exception {
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();

    // ... need code ...
    for(Ast1.Stmt s: n.stmts)
      code.addAll(gen(s));

    return code;
  }


  // Ast1.Assign ---
  // Ast1.Id lhs;
  // Ast1.Exp rhs;
  //
  // AG:
  //   code: rhs.c + lhs.c + "lhs.s = rhs.v"
  //
  static List<IR1.Inst> gen(Ast1.Assign n) throws Exception {
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();


    
    //Local variables
    CodePack left;
    CodePack right = gen(n.rhs);
    code.addAll(right.code);  

    //Lefthand side is an ID
    if(n.lhs instanceof Ast1.Id) {
      left = gen((Ast1.Id) n.lhs);
      code.addAll(left.code);
      code.add(new IR1.Move((IR1.Id)left.src, right.src));
    }
    // left hand side is an Array Element
    else if(n.lhs instanceof Ast1.ArrayElm) {
      left = genAddr((Ast1.ArrayElm) n.lhs);
      code.addAll(left.code);
      IR1.Addr addr = new IR1.Addr(left.src);
      code.add(new IR1.Store(addr, right.src));
    }
    return code;

  }

  // Ast1.CallStmt
  // String nm
  // Ast1.Exp Exp[] args
  // Needs more work
  static List<IR1.Inst> gen(Ast1.CallStmt n) throws Exception {
    
    //Lists of the instructions and sources
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    List<IR1.Src> srcs = new ArrayList<IR1.Src>();
    IR1.Global gname = new IR1.Global("_"+n.nm);


    for (Ast1.Exp e: n.args) { 
      CodePack cp = gen(e);
      code.addAll(cp.code);
      srcs.add(cp.src);
    }
    IR1.Call calls = new IR1.Call(gname, srcs);
    code.add(calls);    
    return code; 
  }


  // Ast1.If ---
  // Ast1.Exp cond;
  // Ast1.Stmt s1, s2;
  //
  // AG:
  //   newLabel: L1[,L2]
  //   code: cond.c 
  //         + "if cond.v == false goto L1" 
  //         + s1.c 
  //         [+ "goto L2"] 
  //         + "L1:" 
  //         [+ s2.c]
  //         [+ "L2:"]
  //
  static List<IR1.Inst> gen(Ast1.If n) throws Exception {
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    
    // Label creation 
    IR1.Label label1 = new IR1.Label();
    IR1.Label label2 = null;
    
    CodePack cp = gen(n.cond);
    code.addAll(cp.code);
    code.add(new IR1.CJump(IR1.ROP.EQ, cp.src, IR1.FALSE, label1));
    code.addAll(gen(n.s1));
  
    // checking for the else clause is null
    if(n.s2 != null) {
      label2 = new IR1.Label();
      code.add(new IR1.Jump(label2));
    }
   
    code.add(new IR1.LabelDec(label1)); 
    // There is an else clause.
    if(n.s2 != null){
      //IR1.Label label2 = new IR1.Label();
      code.addAll(gen(n.s2));
      code.add(new IR1.LabelDec(label2));
    }
    return code; 
  }


  // Ast1.While ---
  // Ast1.Exp cond;
  // Ast1.Stmt s;
  //
  // AG:
  //   newLabel: L1,L2
  //   code: "L1:" 
  //         + cond.c 
  //         + "if cond.v == false goto L2" 
  //         + s.c 
  //         + "goto L1" 
  //         + "L2:"
  //
  static List<IR1.Inst> gen(Ast1.While n) throws Exception {
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();

    // New label creation
    IR1.Label label1 = new IR1.Label();
    IR1.Label label2 = new IR1.Label();

    code.add(new IR1.LabelDec(label1));
    CodePack cp = gen(n.cond);
    code.addAll(cp.code);
    code.add(new IR1.CJump(IR1.ROP.EQ, cp.src, IR1.FALSE, label2));
    code.addAll(gen(n.s));
    code.add(new IR1.Jump(label1));
    code.add(new IR1.LabelDec(label2));
    return code;
  }
  

  // Ast1.Print ---
  // Ast1.Exp arg;
  //
  // AG:
  //   code: arg.c + "print (arg.v)"
  //
  static List<IR1.Inst> gen(Ast1.Print n) throws Exception {
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    List<IR1.Src> srcs = new ArrayList<IR1.Src>();
    IR1.Global gname;

   
    CodePack cp = gen(n.arg);
    srcs.add(cp.src);
    code.addAll(cp.code);
    if(cp.src instanceof IR1.IntLit || cp.src instanceof IR1.BoolLit || cp.src instanceof IR1.Id) {
      gname = new IR1.Global("_printInt");
    }
    else if (cp.src instanceof IR1.StrLit) {
      gname = new IR1.Global("_printStr");
    }
    else {
      gname = new IR1.Global("_printStr");
    }
    code.add(new IR1.Call(gname, srcs));
    return code;
  }


  // Ast1.Return --
  // Ast1.Exp val;
  //
  // 

  static List<IR1.Inst> gen(Ast1.Return n) throws Exception {

    List<IR1.Inst> code = new ArrayList<IR1.Inst>();

    CodePack cp = gen(n.val);
    code.addAll(cp.code);
    code.add(new IR1.Return(cp.src));
    
    return code;
  }

  // EXPRESSIONS

  static CodePack gen(Ast1.Exp n) throws Exception {
    if (n instanceof Ast1.Binop)    return gen((Ast1.Binop) n);
    if (n instanceof Ast1.Unop)     return gen((Ast1.Unop) n);
    if (n instanceof Ast1.Id)       return gen((Ast1.Id) n);
    if (n instanceof Ast1.IntLit)   return gen((Ast1.IntLit) n);
    if (n instanceof Ast1.BoolLit)  return gen((Ast1.BoolLit) n);
    if (n instanceof Ast1.Call)     return gen((Ast1.Call) n);
    if (n instanceof Ast1.NewArray) return gen((Ast1.NewArray) n);
    if (n instanceof Ast1.ArrayElm) return gen((Ast1.ArrayElm) n);
    if (n instanceof Ast1.StrLit)   return gen((Ast1.StrLit) n);
    throw new GenException("Unknown Exp node: " + n);
  }




  static CodePack gen(Ast1.Call n) throws Exception {
  
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    List<IR1.Src> srcs = new ArrayList<IR1.Src>();
    IR1.Global gname = new IR1.Global("_"+n.nm);   
    IR1.Temp t1 = new IR1.Temp();

    for(Ast1.Exp e: n.args) {
      CodePack cp = gen(e);
      code.addAll(cp.code);
      srcs.add(cp.src);
    }
    IR1.Call calls = new IR1.Call(gname, srcs, t1);
    code.add(calls);
    return new CodePack(t1, code);

    

  }


 
  // Ast1.Binop ---
  // Ast1.BOP op;
  // Ast1.Exp e1,e2;
  //
  // AG:
  //   newTemp: t
  //   code: e1.c + e2.c
  //         + "t = e1.v op e2.v"
  //
  static CodePack gen(Ast1.Binop n) throws Exception {
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();

  

    // Setting up codepacks and gathering instructions
    CodePack left = gen(n.e1);
    CodePack right = gen(n.e2);
    IR1.BOP op = gen(n.op);
    IR1.Temp t = new IR1.Temp();

    code.addAll(left.code);
    code.addAll(right.code);
    code.add(new IR1.Binop(op, t, left.src, right.src));
 
    return new CodePack(t, code);
  }

 


  // Ast1.Unop ---
  // Ast1.UOP op;
  // Ast1.Exp e;
  //
  // AG:
  //   newTemp: t
  //   code: e.c + "t = op e.v"
  //
  static CodePack gen(Ast1.Unop n) throws Exception {
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    IR1.UOP op = null;

    CodePack cp = gen(n.e);
    code.addAll(cp.code);
    op = (n.op == Ast1.UOP.NEG) ? IR1.UOP.NEG : IR1.UOP.NOT;
    IR1.Temp t = new IR1.Temp();
    code.add(new IR1.Unop(op, t, cp.src));
    return new CodePack(t, code);
  }


  // Ast1.NewArray
  // Type et
  // int len

  // From Slides IR2UP IR Code Gen Slide 26

  static CodePack gen(Ast1.NewArray n) throws Exception {
    

    IR1.Global gname = new IR1.Global("_malloc");
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    IR1.Temp t1 = new IR1.Temp();
    IR1.Temp t2 = new IR1.Temp();
    List<IR1.Src> srcs = new ArrayList<IR1.Src>();

    code.add(new IR1.Binop(IR1.AOP.MUL, t1, new IR1.IntLit(n.len), new IR1.IntLit(4)));
    srcs.add(t1);
    code.add(new IR1.Call(gname, srcs, t2));
    return new CodePack(t2,code);

  }



  // Ast1.ArrayElem
  // Ast1.Exp ar
  // Ast1.Exp idx;
  static CodePack gen(Ast1.ArrayElm n) throws Exception {
  
    CodePack cp = genAddr(n);
    IR1.Temp t = new IR1.Temp();
    IR1.Addr addr = new IR1.Addr(cp.src);
    cp.code.add(new IR1.Load(t,addr));
    return new CodePack(t, cp.code);
  }



  // Ast1.Id ---
  // String nm;
  //
  static CodePack gen(Ast1.Id n) throws Exception {

    // ... need code ...
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    return new CodePack(new IR1.Id(n.nm), code);
  }


  // Ast1.IntLit ---
  // int i;
  //
  static CodePack gen(Ast1.IntLit n) throws Exception {

    // ... need code ...
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    return new CodePack(new IR1.IntLit(n.i), code);
  }

  static CodePack gen(Ast1.StrLit n) throws Exception {
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    return new CodePack(new IR1.StrLit(n.s), code);
  }  


  // Ast1.BoolLit ---
  // boolean b;
  //
  static CodePack gen(Ast1.BoolLit n) throws Exception {

    // ... need code ...
    List<IR1.Inst> code = new ArrayList<IR1.Inst>();
    return new CodePack(new IR1.BoolLit(n.b), code);
  }


  // OPERATORS

  static IR1.BOP gen(Ast1.BOP op) {
    IR1.BOP irOp = null;
    switch (op) {
    case ADD: irOp = IR1.AOP.ADD; break;
    case SUB: irOp = IR1.AOP.SUB; break;
    case MUL: irOp = IR1.AOP.MUL; break;
    case DIV: irOp = IR1.AOP.DIV; break;
    case AND: irOp = IR1.AOP.AND; break;
    case OR:  irOp = IR1.AOP.OR;  break;
    case EQ:  irOp = IR1.ROP.EQ;  break;
    case NE:  irOp = IR1.ROP.NE;  break;
    case LT:  irOp = IR1.ROP.LT;  break;
    case LE:  irOp = IR1.ROP.LE;  break;
    case GT:  irOp = IR1.ROP.GT;  break;
    case GE:  irOp = IR1.ROP.GE;  break;
    }
    return irOp;
  }

    static IR1.UOP gen(Ast1.UOP op) {
        IR1.UOP irOp = null;
        switch(op) {
            case NEG: irOp = IR1.UOP.NEG; break;
            case NOT: irOp = IR1.UOP.NOT; break;
        }
        return irOp;
    }
}

