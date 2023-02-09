package wacc

import wacc.AbstractSyntaxTree.BaseT._
import wacc.AbstractSyntaxTree.PairElemT._
import wacc.AbstractSyntaxTree._
import wacc.TypeValidator.{isHomogenousList, returnType, declarationTypeToEither}
import wacc.TypeProcessor.fromFunction
import wacc.AbstractSyntaxTree.CmdT.Ret

object SemanticAnalyser {
  private def pairElementType(element: PairElemT.Elem) = simpleExpectation {
    (input) =>
      input match {
        //case PairType(t1, t2) => Right(if (element == Fst) t1 else t2)
        case _ => Left(List("Mismatched type: expected a pair but received: \n".format(input)))
      }
  }

  private def arrayNestedType(array: DeclarationType, indices: Int): Either[List[String], DeclarationType] = array match {
    case ArrayType(innerType) if indices != 0 => arrayNestedType(innerType, indices - 1)
    case someType if indices == 0 => Right(someType)
    case someType if indices != 0 => Left(List("Attempted to dereference non-array type: %s\n".format(someType)))
  }

  def rValType(rVal: RVal)(implicit scopeContext: ScopeContext): Either[List[String], DeclarationType] = rVal match {
    case rVal: Expr => returnType(rVal)
    case ArrayLiteral(exprs) => isHomogenousList(exprs)
    case PairValue(exp1, exp2) => simpleExpectation { (inputs) => Right(PairType(inputs(0), inputs(1))) }
      .matchedWith(List(returnType(exp1), returnType(exp2)))
    case Call(ident, args) => {
      val funcExpectation = scopeContext.findFunc(ident.name)
      funcExpectation.map(_ matchedWith args.map(returnType))
        .getOrElse(Left(List("Function '%s' not defined in scope.")))
    }
    case PairElement(element, pair) => pairElementType(element) matchedWith List(lValType(pair))
  }

  def lValType(lVal: LVal)(implicit scopeContext: ScopeContext): Either[List[String], DeclarationType] = lVal match {
    case PairElement(element, pair) => pairElementType(element) matchedWith List(lValType(pair))
    case IdentLiteral(name) => scopeContext.findVar(name)
    case ArrayElem(name, indices) => {
      if (indices.map(returnType(_)).find((x) => x match {
        case Right(Int_T) => false
        case _ => true
      }).isDefined) Left(List("Non-integer indices in array access."))
      else simpleExpectation(
        (input) => arrayNestedType(input(0), indices.length)) matchedWith List(scopeContext.findVar(name)
      )
    }
  }



  def verifyProgram(program: Program): Either[List[String], ScopeContext] = {
    var topLevelContext = new ScopeContext()
    for (func <- program.funcs) {
      /* Ensure that funcs are added to top level symbol table */
      topLevelContext = 
        topLevelContext.addFunc(func.ident.name, fromFunction(func)) match {
        case Left(err) => return Left(err)
        case Right(newContext) => newContext
      }
    }
    /* Verify each functions code */
    for (func <- program.funcs) {
      verifyFunc(topLevelContext, func) match {
        case Left(err) => return Left(err)
        case Right(value) => 
      }
    }
    verifyStat(topLevelContext, program.stats)
  }

  private def verifyFunc(context: ScopeContext, func: Func): Either[List[String], ScopeContext] = {
    var funcContext = context
    /* Add all arguments to symbol table for func to use */
    func.types.foreach(elem => {
      funcContext = funcContext.addVar(elem._2.name, elem._1) match {
        case Left(err) => return Left(err)
        case Right(newContext) => newContext
      }
    })
    verifyStat(funcContext, func.code)
  }

  private def verifyRValue(rVal: RVal): Unit = {

  }

  private def verifyStat(context: ScopeContext, stat: Stat): Either[List[String], ScopeContext] = {
    //println(stat)
    stat match {
      case SkipStat() => Right(context)
      case dec: Declaration => verifyDeclaration(context, dec)
      case assignment: Assignment => verifyAssignment(context, assignment)
      case ifStat: IfStat => verifyIf(context, ifStat)
      case whileLoop: WhileLoop => verifyWhile(context, whileLoop)
      case statList: StatList => verifyStatList(context, statList)
      case Read(lvalue) => {
        /*Not sure what this is*/
        lValType(lvalue)(context) match {
          case Left(err) => Left(err)
          case Right(lType) => {
            if (lType == BaseType(Int_T) || lType == BaseType(Char_T)) {
              Right(context)
            } else {
              Left(List("Left value not character or integer"))
            }
          }
        }
      }
      case Command(command, input) => {
        /*Not sure what this is*/
        // TODO: don't allow return in main func
        if (command == Ret) {
          /* Ensure correct value type is returned from function */
          returnType(input)(context) match {
            case Left(err) => Left(err)
            case Right(iType) => {
              val retType = Option(context.returnType())
              if (retType.isEmpty) {
                /* Don't allow return statement in main program */
                return Right(context)
              }
              retType.get matchedWith(List(declarationTypeToEither(iType))) match {
                case Left(err) => Left(List("Return type %s does not match function type %s".format(iType, context.returnType())))
                case Right(mType) => Right(context)
              }
            }
          }
        }
        Right(context)
      }
      case BeginEndStat(stat) => {
        /* Ensure scoping is confined */
        verifyStat(context, stat) match {
          case Left(err) => Left(err)
          case Right(newContext) => context 
        }
      }
      
    }
  }

  private def verifyDeclaration(context: ScopeContext, dec: Declaration): Either[List[String], ScopeContext] = {
    dec match {
      case Declaration(dataType, ident, rvalue) => {
        if (context.findVar(ident.name).isRight) {
          val decType = context.findVar(ident.name)
          if (decType.equals(dataType)) {
            Left(List("Variable " + ident.name + " exists in this scope already"))
          }
        }
        dataType match {
          case BaseType(baseType) => {
            // int i = 0
            rvalue match {
              case IntLiteral(x) => {
                dataType match {
                  case BaseType(Int_T) => context.addVar(ident.name, BaseType(Int_T))
                  case _ => Left(List("Incorrect types during assignment {%s, %s}".format(dataType, BaseType(Int_T))))
                }
              }
              case BoolLiteral(x) => {
                dataType match {
                  case BaseType(Bool_T) => context.addVar(ident.name, BaseType(Bool_T))
                  case _ => Left(List("Incorrect types during assignment {%s, %s}".format(dataType, BaseType(Bool_T))))
                }
              }
              case CharLiteral(x) => {
                dataType match {
                  case BaseType(Char_T) => context.addVar(ident.name, BaseType(Char_T))
                  case _ => Left(List("Incorrect types during assignment {%s, %s}".format(dataType, BaseType(Char_T))))
               }
              }
              case StringLiteral(x) => {
                dataType match {
                  case BaseType(String_T) => context.addVar(ident.name, BaseType(String_T))
                  case _ => Left(List("Incorrect types during assignment {%s, %s}".format(dataType, BaseType(String_T))))
               }
              }
              // int i = n
              case IdentLiteral(name) => {
                context.findVar(name) match {
                  case Left(err) => return Left(List("Identifier %s not in scope".format(name)))
                  case Right(iType) => {
                    if (iType != dataType) {
                      Left(List("Identifier %s does not match type of %s {%s, %s}".format(name, ident.name, iType, dataType)))
                    } else {
                      Right(context)
                    }
                  }
                }
              }
              // int i = i + 1
              case binOp@BinaryOp(op, expr1, expr2) => {
                returnType(binOp)(context) match {
                  case Left(err) => Left(err)
                  case Right(opType) => {
                    if (!dataType.equals(opType)) {
                      return Left(List("Incorrect type assignment"))
                    }
                    context.addVar(ident.name, opType)
                  }
                }
              }
              // int i = ord 'a'
              case unOp@UnaryOp(op, expr) => {
                returnType(unOp)(context) match {
                  case Left(err) => Left(err)
                  case Right(opType) => {
                    if (dataType.equals(opType)) {
                      return Left(List("Incorrect type assignment")) 
                    }
                    context.addVar(ident.name, opType)
                  }
                }
              }
              // int i = f()
              case call@Call(funcIdent, args) => {
                context.findFunc(funcIdent.name) match {
                  case Left(err) => Left(List("Function %s not in scope".format(funcIdent.name)))
                  case Right(exp) => {
                    exp matchedWith List(declarationTypeToEither(dataType)) match {
                      case Left(err) => Left(err)
                      case Right(opType) => {
                        context.addVar(ident.name, opType)
                      }
                    }
                  }
                }
              }
              case any => Left(List("rvalue %s not implemented".format(any)))
            }
          }
          case NestedPair() => {
            rvalue match {
              case IdentLiteral(x) => {
                if (context.findVar(x) != PairType) {
                  return Left(List("Not a pair"))
                }
              }
              case default => {
                return Left(List("Not a pair"))
              }
            }
            Left(List("Not Yet Implemented"))
          }

          case PairType(fstType, sndType) => {
            rvalue match {
              case PairValue(exp1, exp2) => {
                returnType(exp1)(context) match {
                  case Left(err) => Left(err)
                  case Right(e1Type) => {
                    returnType(exp2)(context) match {
                      case Left(err) => Left(err)
                      case Right(e2Type) => {
                        if (!fstType.equals(e1Type) || !sndType.equals(e2Type)) {
                          Left(List("Pair types do not match {(%s, %s), (%s, %s)}"
                          .format(fstType, sndType, e1Type, e2Type)))
                        } else {
                          context.addVar(ident.name, PairType(e1Type, e2Type))
                        }
                      }
                    }
                  }
                }
              }
              case PairLiteral() => {
                // TODO: adjust PairType to allow for nested pairs
                context.addVar(ident.name, PairType(fstType, sndType))
              }
              case IdentLiteral(name) => {
                context.findVar(name) match {
                  case Left(err) => Left(err)
                  case Right(rType) => {
                    if (rType.equals(PairType(fstType, sndType))) {
                      context.addVar(ident.name, PairType(fstType, sndType))
                    } else {
                      Left(List("Pair types do not match {(%s, %s), %s}"
                      .format(fstType, sndType, rType)))
                    }
                  }
                }
              }
              case Call(ident, args) => {
                context.findFunc(ident.name) match {
                  case Left(err) => Left(err)
                  case Right(exp) => {
                    exp matchedWith(List(declarationTypeToEither(PairType(fstType, sndType)))) match {
                      case Left(err) => Left(err)
                      case Right(rType) => {
                        if (rType.equals(PairType(fstType, sndType))) {
                          context.addVar(ident.name, PairType(fstType, sndType))
                        } else {
                          Left(List("Pair types do not match {(%s, %s), %s}"
                          .format(fstType, sndType, rType)))
                        }
                      }
                    }
                  }
                }
              }
              case PairElement(elem, lvalue) => Left(List("PairElement not implemented"))
              case any => Left(List("RHS is not a pair {%s}".format(any)))
            }
          }
          case ArrayType(dataType) => {
            rvalue match {
               case ArrayLiteral(elements) => {
                  var newContext = context
                 for (element <- elements) {
                  returnType(element)(context) match {
                    case Left(err) => Left(err)
                    case Right(elementType) => {
                      if (elementType != dataType) {
                        return Left(List("Invalid Array Typing"))
                      }
                      context.addVar(ident.name, ArrayType(elementType)) match {
                        case Left(err) => return Left(err)
                        case Right(value) => newContext = value
                      }
                    }
                  }
                 }
                 Right(newContext)
               }
              case default => {
                return Left(List("Right side not array literal"))
              }
            }
          }
        }
        /*and make sure dataType and rvalue have same type*/
      }
    }
  }

  private def verifyAssignment(context: ScopeContext, assignment: Assignment): Either[List[String], ScopeContext] = {
    assignment match {
      case Assignment(lvalue, rvalue) => {
        /* Check if LHS is in scope */
        val name = lvalue match {
          case IdentLiteral(name) => name
          case ArrayElem(name, indicies) => name
        }
        val lType = context.findVar(name) match {
          case Left(err) => return Left(List("Identifier %s not in scope".format(name)))
          case Right(t) => t
        }
        /* Check LHS and RHS are same type */
        val rType = rvalue match {
          case exp:Expr => returnType(exp)(context)
          case ArrayLiteral(elements) => {
            if (elements.isEmpty) {
              // TODO: Can emoty array exist?
              Left(List("Empty array on RHS"))
            } else {
              returnType(elements.head)(context) match {
                case Left(err) => return Left(err)
                case Right(aType) => {
                  for (elem <- elements) {
                    returnType(elem)(context) match {
                      case Left(err) => return Left(err)
                      case Right(eType) => {
                        if (eType != aType) {
                          return Left(List("Inconsistent types in RHS array assignment"))
                        }
                      }
                    }
                  }
                  Right(ArrayType(aType))
                }
              }
            }
          }
          case call@Call(funcIdent, args) => {
            context.findFunc(funcIdent.name) match {
              case Left(err) => Left(List("Function %s not in scope".format(funcIdent.name)))
              case Right(exp) => {
                exp matchedWith List(declarationTypeToEither(lType)) match {
                  case Left(err) => Left(err)
                  case Right(opType) => Right(lType)
                }
              }
            }
          }
          case PairElement(elem, lvalue) => Left(List("Not implemented"))
          case PairValue(exp1, exp2) => Left(List("Not implemented"))
        }
        rType match {
          case Left(err) => Left(err)
          case Right(t) => {
            if (!lType.equals(t)) {
              Left(List("Assignment types are not the same {%s, %s}".format(lType, rType)))
            }
            Right(context)
          }
        }
      }
    }
  }

  private def verifyIf(context: ScopeContext, ifStat: IfStat): Either[List[String], ScopeContext] = {
    ifStat match {
      case IfStat(cond, stat1, stat2) => {
      /*Make sure cond is boolean, verify stat1 and stat2 and make sure there is fi*/
      returnType(cond)(context) match {
        case Left(err) => Left(err)
        case Right(sType) => {
          sType match {
            case BaseType(Bool_T) => {
              verifyStat(context, stat1) match {
                case Left(err) => return Left(err)
                case Right(_) => return verifyStat(context, stat2)
              }
            }
            case _ => Left(List("Semantic Error: if condition is not of type Bool"))
          }
        }
      }
    }
    }
  }

  private def verifyWhile(context: ScopeContext, whileLoop: WhileLoop): Either[List[String], ScopeContext] = {
    whileLoop match {
      case WhileLoop(cond, stat) => {
        /*Make sure cond is boolean, verify stat*/
        returnType(cond)(context) match {
          case Left(err) => Left(err)
          case Right(sType) => {
            sType match {
              case BaseType(Bool_T) => verifyStat(context, stat)
              case _ => Left(List("Semantic Error: while condition is not of type Bool"))
            }
          }
        }
      }
    }
  }

  private def verifyStatList(context: ScopeContext, statList: StatList): Either[List[String], ScopeContext] = {
    statList match {
      case StatList(statList) => {
        /*verify stat in list*/
        var newContext = context
        for (stat <- statList) {
          verifyStat(newContext, stat) match {
            case Left(err) => return Left(err)
            case Right(c) => newContext = c
          }
        }
        Right(newContext)
      }
    }
  }
}

/*
  sealed trait Errors
  case class DeclarationError(errorMessage: String) extends Errors
  case class AssignmentError(errorMessage: String) extends Errors
  case class ReadError(errorMessage: String) extends Errors
  case class CommandError(errorMessage: String) extends Errors
  case class ConditionError(errorMessage: String) extends Errors
  case class ArrayError(errorMessage: String) extends Errors
*/
