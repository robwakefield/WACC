package wacc

import wacc.AssemblerTypes._

object ARM11AssemblerTypes {
  case class ARM11StackOffset(offset: Int) extends StackOffset(offset: Int) {
    override def toString(): String = "STACK" + offset.toString()
  }

  case class ARM11ImmediateInt(i: Int) extends ImmediateInt(i: Int) {
    override def toString(): String = "#" + i.toString()
  }

  case class ARM11LabelString(name: String) extends LabelString(name: String) {
    override def toString(): String = "=" + name
  }

  case class ARM11BranchString(name: String) extends BranchString(name: String) {
    override def toString(): String = name
  }

  sealed trait ARM11Register extends GeneralRegister {
    import scala.math.Ordered.orderingToOrdered
    def compare(that: ARM11Register): Int = listOfRegisters.get(this) compare listOfRegisters.get(that)
    val listOfRegisters = Map[GeneralRegister, Int](r0 -> 0, r1 -> 1, r2 -> 2, r3 -> 3, r4 -> 4, r5 -> 5, r6 -> 6,
    r7 -> 7, r8 -> 8, r9 -> 9, r10 -> 10, r11 -> 11, r12 -> 12, r13 -> 13, r14 -> 14)
  }

  object r0 extends ARM11Register {
    override def toString(): String = "r0"
  }

  object r1 extends ARM11Register {
    override def toString(): String = "r1"
  }

  object r2 extends ARM11Register {
    override def toString(): String = "r2"
  }

  object r3 extends ARM11Register {
    override def toString(): String = "r3"
  }

  object r4 extends ARM11Register {
    override def toString(): String = "r4"
  }

  object r5 extends ARM11Register {
    override def toString(): String = "r5"
  }

  object r6 extends ARM11Register {
    override def toString(): String = "r6"
  }

  object r7 extends ARM11Register {
    override def toString(): String = "r7"
  }

  object r8 extends ARM11Register {
    override def toString(): String = "r8"
  }

  object r9 extends ARM11Register {
    override def toString(): String = "r9"
  }

  object r10 extends ARM11Register {
    override def toString(): String = "r10"
  }

  object r11 extends ARM11Register {
    override def toString(): String = "r11"
  }

  object r12 extends ARM11Register {
    override def toString(): String = "r12"
  }

  object r13 extends ARM11Register {
    override def toString(): String = "r13" // is this used?
  }

  object r14 extends ARM11Register {
    override def toString(): String = "r14" // is this used?
  }

  object sp extends SPRegister {
    override def toString(): String = "sp"
  }

  object fp extends FPRegister {
    override def toString(): String = "fp"
  }

  object lr extends LinkRegister {
    override def toString(): String = "lr"
  }

  object pc extends PCRegister {
    override def toString(): String = "pc"
  }

  case class ImmediateValueOrRegister(operand: Either[ARM11Register, Int]) extends LHSop {
    @Override
    override def toString: String = {
      operand match {
        case Left(value) => {
          value.toString
        }
        case Right(value) => {
          "#" + value
        }
      }
    }
  }

  case class LogicalShiftLeft(sourceRegister: ARM11Register, operand: Either[ARM11Register, Int]) extends LHSop {
    override def toString: String = {
      operand match {
        case Left(x) => {
          sourceRegister + ", " + "lsl " + x
        }
        case Right(value) => {
          sourceRegister + ", " + "lsl " + "#" + value
        }
      }
    }
  }

  case class LogicalShiftRight(sourceRegister: ARM11Register, operand: Either[ARM11Register, Int]) extends LHSop {
    override def toString: String = {
      operand match {
        case Left(x) => {
          sourceRegister + ", " + "lsr " + x
        }
        case Right(value) => {
          sourceRegister + ", " + "lsr " + " " + "#" + value
        }
      }
    }
  }

  case class ArithmeticShiftRight(sourceRegister: ARM11Register, operand: Either[ARM11Register, Int]) extends LHSop {
    override def toString: String = {
      operand match {
        case Left(x) => {
          sourceRegister + ", " + "asr " + x
        }
        case Right(value) => {
          sourceRegister + ", " + "asr " + "#" + value
        }
      }
    }
  }

  case class RotateRight(sourceRegister: ARM11Register, operand: Either[ARM11Register, Int]) extends LHSop {
    override def toString: String = {
      operand match {
        case Left(x) => {
          sourceRegister + ", " + "ror " + x
        }
        case Right(value) => {
          sourceRegister + ", " + "ror " + "#" + value
        }
      }
    }
  }

  case class RotateRightExtended(sourceRegister: ARM11Register) extends LHSop {
    override def toString: String = {
      sourceRegister + ", " + "rrx"
    }
  }

  case class ARM11Control() extends Control {
    override def toString: String = {
      "c"
    }
  }

  case class ARM11Extension() extends Extension {
    override def toString: String = {
      "x"
    }
  }

  case class ARM11Status() extends Status {
    override def toString: String = {
      "s"
    }
  }

  case class ARM11Flags() extends Flags {
    override def toString: String = {
      "f"
    }
  }

  case class ARM11None() extends None {
    override def toString: String = {
      ""
    }
  }
}
