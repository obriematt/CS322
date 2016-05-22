// This is supporting software for CS322 Compilers and Language Design II
// Copyright (c) Portland State University
//---------------------------------------------------------------------------
// For CS322 W'16 (J. Li).
//

// IR1 interpreter. (A starter version)
//
//
import java.util.*;
import java.io.*;
import ir.*;

public class IR1Interp {

  static class IntException extends Exception {
    public IntException(String msg) { super(msg); }
  }

  //-----------------------------------------------------------------
  // Value Representation
  //-----------------------------------------------------------------
  //
  abstract static class Val {}

  // -- Integer values
  //
  static class IntVal extends Val {
    int i;
    IntVal(int i) { this.i = i; }
    public String toString() { return "" + i; }
  }

  // -- Boolean values
  //
  static class BoolVal extends Val {
    boolean b;
    BoolVal(boolean b) { this.b = b; }
    public String toString() { return "" + b; }
  }

  // -- String values
  //
  static class StrVal extends Val {
    String s;
    StrVal(String s) { this.s = s; }
    public String toString() { return s; }
  }

  // -- A special "undefined" value
  //
  static class UndVal extends Val {
    public String toString() { return "UndVal"; }
  }

  //-----------------------------------------------------------------
  // Storage Organization
  //-----------------------------------------------------------------
  //

  // -- Global heap memory
  //
  static ArrayList<Val> memory;

  // -- Environment for tracking var, temp, and param's values
  //    (one copy per fuction invocation)
  //
  // Slides suggest putting both label and var maps within Env
  // It also states using a single map for both variables and temps is ok.
  static class Env extends HashMap<String,Val> {
    HashMap<String, Integer> labelMap = new HashMap<String, Integer>();
    HashMap<String, Val> varMap = new HashMap<String, Val>();
  }

  //-----------------------------------------------------------------
  // Other Data Structures
  //-----------------------------------------------------------------
  //
  // GUIDE:
  //  You have control over these. Either define look-up tables for 
  //  functions and labels, or searching functions.
  //

  // -- Useful global variables
  //
  static final int CONTINUE = -1;	// execution status 
  static final int RETURN = -2;		// execution status
  static Val retVal = null;             // for return value passing

  // Added Data Structures
  //static HashMap<String, Integer> labelMap;
  static HashMap<String, IR1.Func> funcMap;

 



  //-----------------------------------------------------------------
  // The Main Method
  //-----------------------------------------------------------------
  //
  public static void main(String [] args) throws Exception {
    if (args.length == 1) {
      FileInputStream stream = new FileInputStream(args[0]);
      IR1.Program p = new IR1Parser(stream).Program();
      stream.close();
      IR1Interp.execute(p);
    } else {
      System.out.println("You must provide an input file name.");
    }
  }

  //-----------------------------------------------------------------
  // Top-Level Nodes
  //-----------------------------------------------------------------
  //

  // Program ---
  //  Func[] funcs;
  //
  // GUIDE:
  // 1. Establish the look-up tables (if you plan to use them).
  // 2. Look up or search for function '_main'.
  // 3. Start interpreting from '_main' with an empty Env.
  //
  public static void execute(IR1.Program n) throws Exception { 

    // ... code needed ...
    // Created maps, lists, and environment
    funcMap = new HashMap<String, IR1.Func>();
    memory = new ArrayList<Val>(); 
    retVal = new UndVal();
    Env env = new Env();
    // Loop gathering functions
    for(IR1.Func f: n.funcs) {
      funcMap.put(f.gname.s, f);
    }
    // Start main.
    execute(funcMap.get("_main"), env);

  }

  // Func ---
  //  Global gname;
  //  Id[] params;
  //  Id[] locals;
  //  Inst[] code;
  //
  // GUIDE:
  //  - Implement the fetch-execute loop.
  //  - The parameter 'env' is the function's initial Env, which
  //    contains its parameters' values.
  //
  static void execute(IR1.Func n, Env env) throws Exception {
    // Gather the labels.
    for(int i = 0; i < n.code.length; i++) {
      if(n.code[i] instanceof IR1.LabelDec) {
        env.labelMap.put(((IR1.LabelDec) n.code[i]).lab.name, i);
      }
    }

    // Given Code 
    int idx = 0;
    while (idx < n.code.length) {
      int next = execute(n.code[idx], env);
      if (next == CONTINUE)
	idx++; 
      else if (next == RETURN)
        break;
      else
	idx = next;
    }
  }

  // Dispatch execution to an individual Inst node.
  //
  static int execute(IR1.Inst n, Env env) throws Exception {
    if (n instanceof IR1.Binop)    return execute((IR1.Binop) n, env);
    if (n instanceof IR1.Unop) 	   return execute((IR1.Unop) n, env);
    if (n instanceof IR1.Move) 	   return execute((IR1.Move) n, env);
    if (n instanceof IR1.Load) 	   return execute((IR1.Load) n, env);
    if (n instanceof IR1.Store)    return execute((IR1.Store) n, env);
    if (n instanceof IR1.Call)     return execute((IR1.Call) n, env);
    if (n instanceof IR1.Return)   return execute((IR1.Return) n, env);
    if (n instanceof IR1.Jump) 	   return execute((IR1.Jump) n, env);
    if (n instanceof IR1.CJump)    return execute((IR1.CJump) n, env);
    if (n instanceof IR1.LabelDec) return CONTINUE;
    throw new IntException("Unknown Inst: " + n);
  }

  //-----------------------------------------------------------------
  // Individual Instruction Nodes
  //-----------------------------------------------------------------
  //
  // - Each execute() routine returns CONTINUE, RETURN, or a new idx 
  //   (target of jump).
  //

  // Binop ---
  //  BOP op;
  //  Dest dst;
  //  Src src1, src2;
  //
  // GUIDE:
  // 1. Evaluate the operands, then perform the operation.
  // 2. Update 'dst's entry in the Env with operation's result.
  //
  static int execute(IR1.Binop n, Env env) throws Exception {

    // ... code needed ...
    // Find the value of the left and right side of the BinOp
    Val valOne = evaluate(n.src1, env);
    Val valTwo = evaluate(n.src2, env);


    // The result of the operation.
    Val results = null;
   
    // Check for the many different operations
    // Bool Operation checks
    if(valOne instanceof BoolVal && valTwo instanceof BoolVal){
    if(n.op == IR1.AOP.AND) {
      boolean one = ((BoolVal)valOne).b;
      boolean two = ((BoolVal)valTwo).b;
      results = new BoolVal(one && two);
    }
    else if(n.op == IR1.AOP.OR) {
      boolean one = ((BoolVal)valOne).b;
      boolean two = ((BoolVal)valTwo).b;
      results = new BoolVal(one || two);
    }
    }
    else if(valOne instanceof IntVal && valTwo instanceof IntVal) {
    // Equality Operation Checks
    if(n.op == IR1.ROP.EQ) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      if(one == two)
        results = new BoolVal(true);
      else
        results = new BoolVal(false);
    }
    else if(n.op == IR1.ROP.NE) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      if(one != two)
        results = new BoolVal(false);
      else
        results = new BoolVal(true);
    }
    else if(n.op == IR1.ROP.LT) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      if(one < two)
        results = new BoolVal(true);
      else
        results = new BoolVal(false);
    }
    else if(n.op == IR1.ROP.LE) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      if(one <= two)
        results = new BoolVal(true);
      else
        results = new BoolVal(false);
    }
    else if(n.op == IR1.ROP.GT) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      if(one > two)
        results = new BoolVal(true);
      else
        results = new BoolVal(false);
    } 
    else if(n.op == IR1.ROP.GE) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      if(one >= two)
        results = new BoolVal(true);
      else
        results = new BoolVal(false);
    }
  
    // Arithmetic Operation checks
    else if(n.op == IR1.AOP.ADD) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      results = new IntVal(one + two);
    }
    else if(n.op == IR1.AOP.SUB) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      results = new IntVal(one - two);
    }
    else if(n.op == IR1.AOP.DIV) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      results = new IntVal(one / two);
    }
    else if(n.op == IR1.AOP.MUL) {
      int one = ((IntVal)valOne).i;
      int two = ((IntVal)valTwo).i;
      results = new IntVal(one * two);
    }
    }
    //else
      //throw new IntException("The OP is not the right Binop: "+n.op);
 
    env.varMap.put(n.dst.toString(), results);
    return CONTINUE;  
  }

  // Unop ---
  //  UOP op;
  //  Dest dst;
  //  Src src;
  //
  // GUIDE:
  // 1. Evaluate the operand, then perform the operation.
  // 2. Update 'dst's entry in the Env with operation's result.
  //
  static int execute(IR1.Unop n, Env env) throws Exception {

    // ... code needed ...
    Val val = evaluate(n.src, env);
    Val results;
    if(n.op == IR1.UOP.NEG) {
      results = new IntVal(-((IntVal)val).i);
    }
    else if(n.op == IR1.UOP.NOT) {
      results = new BoolVal(!((BoolVal)val).b);
    }
    else {
      throw new IntException("Bad Unop: "+n.op);
    }
    env.varMap.put(n.dst.toString(), results);
    return CONTINUE;  
  }

  // Move ---
  //  Dest dst;
  //  Src src;
  //
  // GUIDE:
  //  Evaluate 'src', then update 'dst's entry in the Env.
  //
  static int execute(IR1.Move n, Env env) throws Exception {

    // ... code needed ...
    Val val = evaluate(n.src, env);
 
    env.varMap.put(n.dst.toString(), val);

    return CONTINUE;  
  }

  // Load ---  
  //  Dest dst;
  //  Addr addr;
  //
  // GUIDE:
  //  Evaluate 'addr' to a memory index, then retrieve the stored 
  //  value from memory and update 'dst's entry in the Env.
  //
  static int execute(IR1.Load n, Env env) throws Exception {

    // ... code needed ...
    int dest = evaluate(n.addr, env);
    Val val = memory.get(dest);
    env.varMap.put(n.dst.toString(), val);
    return CONTINUE;  
  }

  // Store ---  
  //  Addr addr;
  //  Src src;
  //
  // GUIDE:
  // 1. Evaluate 'src' to a value.
  // 2. Evaluate 'addr' to a memory index, then store the value
  //    to the memory entry.
  //
  static int execute(IR1.Store n, Env env) throws Exception {

    // ... code needed ...
    // Step 1 and 2
    Val srcs = evaluate(n.src, env);
    int addrs = evaluate(n.addr, env);   

 
    // Add to memory 
    memory.set(addrs, srcs);

    return CONTINUE;  
  }

  // CJump ---
  //  ROP op;
  //  Src src1, src2;
  //  Label lab;
  //
  // GUIDE:
  // 1. Evaluate the cond op.
  // 2. If cond is true, find and return the instruction index 
  //    of the jump target label; otherwise return CONTINUE.
  //
  static int execute(IR1.CJump n, Env env) throws Exception {

    // ... code needed ...
    Val valOne = evaluate(n.src1,env);
    Val valTwo = evaluate(n.src2,env);
    boolean cond = false;   

    if(n.op == IR1.ROP.NE) {
      if(valOne instanceof IntVal && valTwo instanceof IntVal) {
        if(((IntVal)valOne).i != ((IntVal)valTwo).i) {
          return env.labelMap.get(n.lab.name);
        }
      }
      if(valOne instanceof BoolVal && valTwo instanceof BoolVal) {
        if(((BoolVal)valOne).b != ((BoolVal) valTwo).b) {
          return env.labelMap.get(n.lab.name);
        }
      }
    }
    if(n.op == IR1.ROP.LT) {
      if(valOne instanceof IntVal && valTwo instanceof IntVal) {
        if(((IntVal) valOne).i < ((IntVal)valTwo).i) {
          return env.labelMap.get(n.lab.name);
        }
      }
    }
    if(n.op == IR1.ROP.LE) {
      if(valOne instanceof IntVal && valTwo instanceof IntVal) {
        if(((IntVal)valOne).i <= ((IntVal)valTwo).i) {
          return env.labelMap.get(n.lab.name);
        }
      }
    }
    if(n.op == IR1.ROP.GT) {
      if(valOne instanceof IntVal && valTwo instanceof IntVal) {
        if((((IntVal)valOne).i > ((IntVal)valTwo).i)) {
          return env.labelMap.get(n.lab.name);
        }
      }
    }
    if(n.op == IR1.ROP.GE) {
      if(valOne instanceof IntVal && valTwo instanceof IntVal) {
        if((((IntVal) valOne).i >= ((IntVal)valTwo).i)) {
          return env.labelMap.get(n.lab.name);
        }
      }
    }
    if(n.op == IR1.ROP.EQ) {
      if(valOne instanceof IntVal && valTwo instanceof IntVal) {
        if((((IntVal)valOne).i == ((IntVal)valTwo).i)){
          return env.labelMap.get(n.lab.name);
        }
      }
      if(valOne instanceof BoolVal && valTwo instanceof BoolVal) {
        if((((BoolVal)valOne).b == ((BoolVal)valTwo).b)) {
          return env.labelMap.get(n.lab.name);
        }
      }
    }
    return CONTINUE;
  }	

  // Jump ---
  //  Label lab;
  //
  // GUIDE:
  //  Find and return the instruction index of the jump target label.
  //
  static int execute(IR1.Jump n, Env env) throws Exception {

    // ... code needed ...
    return env.labelMap.get(n.lab.name);
  }	

  // Call ---
  //  Global gname;
  //  Src[] args;
  //  Dest rdst;
  //
  // GUIDE:
  // 1. Evaluate the arguments to values.
  // 2. Create a new Env for the callee; pair function's parameter
  //    names with arguments' values, and add them to the new Env.
  // 3. Find callee's Func node and switch to execute it.
  // 4. If 'rdst' is not null, update its entry in the Env with
  //    the return value (should be avaiable in variable 'retVal').
  //
  static int execute(IR1.Call n, Env env) throws Exception {

    // ... code needed ...
    // Case 1. Malloc
    if(n.gname.s.equals("_malloc")) {
//      Val val = evaluate(n.args[0], env);
//      int size = memory.size();
//      memory = new ArrayList<>();
//      for(int i=0;i<Integer.parseInt(n.args[0].toString());i++)
//        memory.add(new UndVal());
//      env.varMap.put(((IR1.Temp)n.rdst).toString(), new IntVal(size));
//      return size;
      int size = ((IntVal) evaluate(n.args[0], env)).i;
      int location = memory.size();
      for(int i = 0; i < size; i++){
        memory.add(new UndVal());
      }
      env.varMap.put(n.rdst.toString(), new IntVal(location));
    }
    // Case 2. printInt
    else if(n.gname.s.equals("_printInt")) {
      Val val = evaluate(n.args[0], env);
      System.out.println(""+val);
    }
    // Case 3. printStr
    else if(n.gname.s.equals("_printStr")) {
      if(n.args == null || n.args.length == 0) {
        System.out.println();
      }
      else {
        Val val = evaluate(n.args[0], env);
        System.out.println(""+val);
      }
    } else {
    // Case 4. The ELSE!
      IR1.Func func = funcMap.get(n.gname.s);
      Env tempEnv = new Env();
      for (int i=0; i<func.params.length; i++) {
        String paramName = ""+ func.params[i];
        Val argVal = evaluate(n.args[i], env);
        tempEnv.varMap.put(paramName, argVal);
      }         
      execute(func, tempEnv);
      env.varMap.put(n.rdst.toString(), retVal);
    }
    return CONTINUE;
  }	

  // Return ---  
  //  Src val;
  //
  // GUIDE:
  //  If 'val' is not null, set it to the variable 'retVal'.
  // 
  static int execute(IR1.Return n, Env env) throws Exception {

    // ... code needed ...
    //Check for not null
    if(n.val != null) {
      retVal = evaluate(n.val, env);
    }
    return RETURN;
  }

  //-----------------------------------------------------------------
  // Address and Operand Nodes.
  //-----------------------------------------------------------------
  //
  // - Each has an evaluate() routine.
  //

  // Address ---
  //  Src base;  
  //  int offset;
  //
  // GUIDE:
  // 1. Evaluate 'base' to an integer, then add 'offset' to it.
  // 2. Return the result (which should be an index to memory).
  //
  static int evaluate(IR1.Addr n, Env env) throws Exception {

    // ... code needed ...
    //Step 1. 
//    Val bVal = env.varMap.get(((IR1.Temp)n.base).num);
//    int bInt = Integer.parseInt(bVal.toString());
     
//    int mVal = Integer.parseInt(memory.get(bInt).toString());

    
//    return mVal;

    Val val = evaluate(n.base, env);
    
    int location = ((IntVal) evaluate(n.base,env)).i;
    return location + n.offset;
  }

  // Src Nodes 
  //  -> Temp | Id | IntLit | BooLit | StrLit
  //
  // GUIDE:
  //  In each case, the evaluate() routine returns a Val object.
  //  - For Temp and Id, look up their value from the Env, wrap 
  //    it in a Val and return.
  //  - For the literals, wrap their value in a Val and return.
  //
  static Val evaluate(IR1.Src n, Env env) throws Exception {
    Val val = null;
    if (n instanceof IR1.Temp)    val = env.varMap.get(n.toString());
    if (n instanceof IR1.Id)      val = env.varMap.get(n.toString());
    if (n instanceof IR1.IntLit)  val = new IntVal(((IR1.IntLit) n).i);
    if (n instanceof IR1.BoolLit) val = new BoolVal(((IR1.BoolLit) n).b);
    if (n instanceof IR1.StrLit)  val = new StrVal(((IR1.StrLit) n).s);
    return val;
  }

  // Dst Nodes 
  //  -> Temp | Id
  //
  // GUIDE:
  //  For both cases, look up their value from the Env, wrap it
  //  in a Val and return.
  //
  static Val evaluate(IR1.Dest n, Env env) throws Exception {
    Val val = null;

    // ... code needed ...
    //Single step
    if(n instanceof IR1.Temp)
      val = env.varMap.get(((IR1.Temp)n).num);
    if(n instanceof IR1.Id)
      val = env.varMap.get(((IR1.Id)n).s);
    return val;
  }

}
