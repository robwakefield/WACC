package wacc

import parsley.{Failure, Success}
import wacc.Parser.ProgramParser.program
import wacc.SemanticAnalyser.verifyProgram
import wacc.Translator.delegateASTNode
import wacc.ARM11Assembler

import java.io.{BufferedWriter, File, FileNotFoundException, FileWriter}
import scala.io.Source


object Main {
  val OutputAssemblyFile = true

  val SyntaxErrorCode = 100
  val SemanticErrorCode = 200
  val SuccessCode = 0

  def main(args: Array[String]): Unit = {
    if (args.length != 2) throw new IllegalArgumentException(
      "Incorrect number of arguments provided. Received: " + args.length + ", Expected 2."
    )
    val file = Option(Source.fromFile(args.head))
      .getOrElse(throw new FileNotFoundException("File: " + args.head + " does not exist."))
    val inputProgram = file.mkString
    file.close

    println(inputProgram + "\n\n")

    val target = getArchitecture(args(2))
      .getOrElse(throw new FileNotFoundException("Architecture: " + args(2) + " does not exist."))

    /* Compile */
    val ast = program.parse(inputProgram)
    ast match {
      case Failure(err) => {
        println("Syntax Error: %s".format(err))
        sys.exit(SyntaxErrorCode)
      }
      case Success(x) =>
    }

    val verified = verifyProgram(ast.get)
    if (verified.isLeft) {
      print("Semantic Error: ")
      verified.left.foreach(errList => {
        errList.reverse.foreach(err => {
          if (err != null && err.nonEmpty) println(err)
        })
      })
      sys.exit(SemanticErrorCode)
    }
    
    // Translate the ast to TAC
    val tac = delegateASTNode(ast.get)._1
    println("--- TAC ---")
    tac.foreach(l => println(l))

    // Convert the TAC to IR
    val assembler = new Assembler()
    val (result, funcs) = assembler.assembleProgram(tac)

    target match {
      case ARM11 => {
        // Convert the IR to ARM11
        val arm = ARM11Assembler.assemble(result, funcs)
        println("--- ARM ---")
        print(arm)
      }
      case X86 => {
        // Convert the IR to X86_64
        val x86 = X86Assembler.assemble(result, funcs)
        println("--- X86_64 ---")
        print(x86)
      }
    }

    

    /* Output the assembly file */
    if(OutputAssemblyFile) {
      val inputFilename = args.last.split("/").last
      val outputFilename = inputFilename.replace(".wacc", ".s")
      val outputFile = new File(outputFilename)
      val fileWriter = new BufferedWriter(new FileWriter(outputFile))
      fileWriter.write(arm + "\n")
      fileWriter.close()
    }
    println("\n\nCompilation Successful!")
    sys.exit(SuccessCode)
  }
}



