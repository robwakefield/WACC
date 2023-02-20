package wacc

import wacc.AbstractSyntaxTree.{ASTNode, Stat, SkipStat, BeginEndStat, Command, Program, Func}
import wacc.AbstractSyntaxTree.CmdT
import wacc.Assembler.Register

object Translator {
  //TODO: Translate each ASTNode into ARM
  
  def delegateASTNode(node: ASTNode, context : ScopeContext) : List[String] = {
    node match {
      case Program(funcs, stat) => translateProgram(funcs, stat, context)
      case BeginEndStat(stat) => translateBeginEnd(stat)
      case SkipStat() => translateSkip()
      case Command(cmd, expr) => translateCommand(cmd, expr)
      case Func(returnType, ident, types, code) => translateFunction(returnType, ident, types, code)
      case _ => List("")
    }
  }

  def translateProgram(l: List[Func], s: Stat, context: ScopeContext): List[String] = {
    var str = List("")
    for (function: Func <- l) {
      str = str ++ delegateASTNode(function, context) // Not actually sure about the structure of this thing
    }
    str = str ++ delegateASTNode(s, context)
    return str
  }

  def translateBeginEnd(stat : Stat) : List[String] = {
    List("")
  }

  def translateSkip() : List[String] = {
    List("")
  }

  // Compute the expression and return it to the first elem of the register list
  def translateExpr(expr : AbstractSyntaxTree.Expr, freeRegs : Array[Register]) : List[String] = {
    List("")
  }

  def translateCommand(cmd : AbstractSyntaxTree.CmdT.Cmd, expr : AbstractSyntaxTree.Expr, freeRegs : Array[Register]) : List[String] = {
    cmd match {
      case CmdT.Free => List("")
      case CmdT.Ret => translateExpr(expr, Register.r0 +: (freeRegs.filter(! _.equals(Register.r0))))
      case CmdT.Exit => translateExpr(expr, Register.r0 +: (freeRegs.filter(! _.equals(Register.r0)))).appended(List("bl exit"))
      case CmdT.Print => List("")
      case CmdT.PrintLn => List("")
    }
  }

  def translateFunction(returnType : AbstractSyntaxTree.DeclarationType, 
                          ident : AbstractSyntaxTree.IdentLiteral, 
                          types : List[(AbstractSyntaxTree.DeclarationType, 
                            AbstractSyntaxTree.IdentLiteral)], 
                          code : Stat) : List[String] = {
    List("")
  }
}