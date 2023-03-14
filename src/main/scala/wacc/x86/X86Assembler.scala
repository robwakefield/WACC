package wacc

import wacc.AssemblerTypes._
import wacc.FinalIR._
import wacc.TAC.ReservedPushTAC

import collection.mutable


object X86Assembler {

  def assemble(irCode: List[FinalIR], endFuncs: mutable.Map[String, List[FinalIR]]): String = {
    endFuncsIR = endFuncs
    (irCode.map(assembleIR) ++ endFuncsToList()).mkString("\n")
  }

  def assembleIR(irCode: FinalIR): String = {
    irCode match {
        case Str(cond, src, operand, dst) => assembleStr(cond, src, operand, dst)
        case StrPre(cond, src, operand, dst) => assembleStrPre(cond, src, operand, dst)
        case Ldr(cond, src, operand, dst) => assembleLdr(cond, src, operand, dst)
        case Push(cond, regs) => assemblePush(cond, regs)
        case Pop(cond, regs) => assemblePop(cond, regs)
        case Add(cond, flag, op1, op2, dst) => assembleAdd(cond, flag, op1, op2, dst)
        case Sub(cond, flag, op1, op2, dst) => assembleSub(cond, flag, op1, op2, dst)
        case Rsb(cond, flag, op1, op2, dst) => assembleRsb(cond, flag, op1, op2, dst)
        case Mul(cond, flag, op1, op2, dst) => assembleMul(cond, flag, op1, op2, dst)
        case Smull(cond, flag, src, op1, op2, dst) => assembleSmull(cond, flag, src, op1, op2, dst)
        case Mov(cond, src, dst) => assembleMove(cond, src, dst)
        case Branch(cond, name) => assembleBranch(cond, name)
        case BranchLink(cond, name) => assembleBranchLink(cond, name)
        case Cmp(cond, op1, op2) => assembleCmp(cond, op1, op2)
        case Global(name) => assembleGlobal(name)
        case Lbl(name) => assembleLabel(name)
        case Comment(str) => assembleComment(str)
        case DataSeg() => assembleDataSeg()
        case TextSeg() => assembleTextSeg()
        case AsciiZ(str) => assembleAsciiZ(str)
        case Word(len) => assembleWord(len)
        case Special(str) => str
    }
  }

  var endFuncsIR = collection.mutable.Map[String, List[FinalIR]]()

  def addEndFunc(name: String, code: List[FinalIR]): Unit = {
    if (!endFuncsIR.contains(name)) {
      endFuncsIR.addOne(name, code)
    }
  }

  def endFuncsToList(): List[String] = {
    endFuncsIR.toList.map(entry => entry match {
      case (name, code) => code.map(c => assembleIR(c))
    }).flatten
  }

  //TODO determine if register size change needed (ie q to l or similar)

  //Uses movq and r registers for now
  def assembleStr(condition: String, src: LHSop, operand: LHSop, dst: Register): String = {
    "movq" + ldrStrAssist(condition, src, operand, dst)
  }

  def assembleStrPre(condition: String, src: LHSop, operand: LHSop, dst: Register): String = {
    "movq" + ldrStrAssist(condition, src, operand, dst).toString + "!".toString()
  }

  
  def assembleLdr(condition: String, src: Register, operand: LHSop, dst: Register): String = {
    "movq" + ldrStrAssist(condition, src, operand, dst)
  }

  def ldrStrAssist(condition: String, src: LHSop, operand: LHSop, dst: Register): String = {
    var str = condition + " " + src.toString + ", "
    operand match {
      case ImmediateInt(x) => {
        str = str + x + "(" + src.toString + ")"
      }
      case LabelString(x) => {
        str = "leaq" + x + "(%rip), " + dst.toString //%rip is instruction pointer
      }
    }
    str
  }

  def assemblePush(condition: String, registers: List[Register]): String = {
    "pushq" + pushPopAssist(condition, registers)
  }

  def assemblePop(condition: String, registers: List[Register]): String = {
    "popq" + pushPopAssist(condition, registers)
  }

  def pushPopAssist(condition: String, registers: List[Register]): String = {
    var str = condition + " {"
    for (register <- registers) {
      if (register != registers.last) {
        str = str + register.toString + ", "
      } else {
        str = str + register.toString
      }
    }
    str + "}"
  }

  def assembleAdd(condition: String, setflag: Suffi, op1: LHSop, op2: LHSop, dst: LHSop): String = {
    "addq" + addSubMulAssist(condition, setflag, op1, op2, dst)
  }

  def assembleSub(condition: String, setflag: Suffi, op1: LHSop, op2: LHSop, dst: LHSop): String = {
    "subq" + addSubMulAssist(condition, setflag, op1, op2, dst)
  }

  def assembleRsb(condition: String, setflag: Suffi, op1: LHSop, op2: LHSop, dst: LHSop): String = {
    "rsbq" + addSubMulAssist(condition, setflag, op1, op2, dst)
  }

  def assembleMul(condition: String, setflag: Suffi, op1: LHSop, op2: LHSop, dst: LHSop): String = {
    "mulq" + addSubMulAssist(condition, setflag, op1, op2, dst)
  }

  def assembleSmull(condition: String, setflag: Suffi, src: LHSop, op1: LHSop, op2: LHSop, dst: LHSop): String = {
    "smullq" + fourMulAssist(condition, setflag, dst, src, op1, op2)
  }

  def fourMulAssist(condition: String, setflag: Suffi, destinationLow: LHSop, destinationHigh: LHSop,
                    sourceRegister: LHSop, operand: LHSop): String = {
    addEndFunc("_errOverflow", new HelperFunctions().assemble_errOverflow())
    addEndFunc("_prints", new HelperFunctions().assemble_prints())

    condition + setflag + " " + destinationLow + "," + " " + destinationHigh + "," + " " + sourceRegister +
      "," + " " + operand +
      "\ncmp " + destinationHigh + ", " + destinationLow + ", asr #31" +
      "\nbne _errOverflow"
  }


  def addSubMulAssist(condition: String, setflag: Suffi, op1: LHSop, op2: LHSop, dst: LHSop): String = {
    addEndFunc("_errOverflow", new HelperFunctions().assemble_errOverflow())
    addEndFunc("_prints", new HelperFunctions().assemble_prints())
    condition + setflag + " " + dst + ", " + op1 + ", " + op2 + "\nblvs _errOverflow"
  }

  def assembleCmp(condition: String, op1: LHSop, op2: LHSop): String = {
    "cmpq" + condition + " " + op1.toString + ", " + op2.toString
  }

  // Determine when a mov is allowed
  def checkMovCases(i: Int): Boolean = {
    for (j <- 0 to 12) {
      val mask = (0xFF << (j * 2))
      if ((i & ~mask) == 0) return true
    }
    if ((i & ~0xFC000003) == 0) return true
    if ((i & ~0xF000000F) == 0) return true
    if ((i & ~0xC000003F) == 0) return true
    false
  }

  def assembleMove(condition: String, src: LHSop, dst: Register): String = {
    src match { //x86 should only use mov
      // case ImmediateInt(i) if !checkMovCases(i) => "ldr " + condition + " " + dst.toString() + ", =" + i
      case _ => "movq" + condition + " " + dst.toString + ", " + src.toString()
    }
  }

  def assembleBranch(condition: String, name: String): String = {
    "b" + condition + " " + name
  }

  def assembleBranchLink(condition: String, name: LHSop): String = {
    "call" + condition + " " + name
  }

  def assembleGlobal(name: String) = {
    ".globl " + name
  }

  def assembleLabel(name: String): String = {
      name + ":"
  }

  def assembleComment(comment: String): String = {
    "# " + comment
  }

  def assembleDataSeg(): String = {
    ".section .rodata"
  }

  def assembleTextSeg(): String = {
    ".text"
  }

  def assembleAsciiZ(str: String): String = {
    ".asciz " + str
  }

  def assembleWord(len: Int): String = {
    ".word " + len.toString()
  }

}