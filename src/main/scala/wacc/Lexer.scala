package wacc

import parsley.Parsley

object Lexer {

  import parsley.token.descriptions.numeric.NumericDesc
  import parsley.token.descriptions.text.{EscapeDesc, TextDesc}
  import parsley.token.descriptions.{LexicalDesc, NameDesc, SpaceDesc, SymbolDesc}
  import parsley.token.{Lexer, predicate}

  private val desc = LexicalDesc.plain.copy(
    nameDesc = NameDesc.plain.copy(
      identifierStart = predicate.Basic((c: Char) => (c == '_') || c.isLetter),
      identifierLetter = predicate.Basic((c: Char) => (c == '_') || c.isLetterOrDigit)
    ),
    symbolDesc = SymbolDesc.plain.copy(
      hardKeywords = Set(
        "true", "false", "null", "len", "ord", "chr", "skip", "read", "free", "return", "exit",
        "print", "println", "if", "then", "else", "fi", "while", "do", "done", "begin", "end",
        "int", "bool", "char", "string", "fst", "snd", "call", "pair", "newpair"
      ),
      hardOperators = Set("!", "*", "/", "%", "+", "-", ">=", ">", "<", "<=", "==", "!=", "&&", "||", ";")
    ),
    numericDesc = NumericDesc.plain.copy(
    ),
    textDesc = TextDesc.plain.copy(
      escapeSequences = EscapeDesc.plain.copy() //TODO: Add escape characters
    ),
    spaceDesc = SpaceDesc.plain.copy(
      commentLine = "#",
      space = predicate.Basic(Character.isWhitespace)
    )
  )

  private val lexer = new Lexer(desc)
  val integer: Parsley[Int] = lexer.lexeme.numeric.signed.decimal32
  val character: Parsley[Char] = lexer.lexeme.text.character.ascii
  val boolean: Parsley[Boolean] = (lexer.lexeme.symbol.apply("true", "true") #> true) <|>
    (lexer.lexeme.symbol.apply("false", "false") #> false)
  val string: Parsley[String] = lexer.lexeme.text.string.ascii
  val emptyPair: Parsley[Unit] = lexer.lexeme.symbol.apply("null", "null")
  val identifier: Parsley[String] = lexer.lexeme.names.identifier

  def fully[A](p: Parsley[A]): Parsley[A] = lexer.fully(p)

  val implicits = lexer.lexeme.symbol.implicits
}