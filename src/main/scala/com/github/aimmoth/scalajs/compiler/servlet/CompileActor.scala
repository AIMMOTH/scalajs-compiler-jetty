package com.github.aimmoth.scalajs.compiler.servlet

import scala.collection.mutable
import scala.reflect.io.VirtualFile

import org.scalajs.core.tools.io.VirtualScalaJSIRFile
import java.util.logging.Logger

case class CompileSource(envId : String, templateId : String, sourceCode : String, optimizer : Optimizer)

case class CompleteSource(envId : String, templateId : String, sourceCode : String, flag : String, offset : Int)

object CompileActor {

  type Source = List[VirtualFile]

}

class CompileActor(classPath : Classpath, envId : String, sourceCode : CompileActor.Source, optimizer : Optimizer) {

  val log = Logger.getLogger(getClass.getName)
  val compiler = new Compiler(classPath, envId)
  val opt = optimizer match {
    case Optimizer.Fast => compiler.fastOpt _
    case Optimizer.Full => compiler.fullOpt _
  }

  val errorStart = """^\w+.scala:(\d+): *(\w+): *(.*)""".r
  val errorEnd = """ *\^ *$""".r

  def parseErrors(log : String) : Seq[EditorAnnotation] = {
    val lines = log.split('\n').toSeq.map(_.replaceAll("[\\n\\r]", ""))
    val (annotations, _) = lines.foldLeft((Seq.empty[EditorAnnotation], Option.empty[EditorAnnotation])) {
      case ((acc, current), line) =>
        line match {
          case errorStart(lineNo, severity, msg) =>
            val ann = EditorAnnotation(lineNo.toInt, 0, Seq(msg), severity)
            (acc, Some(ann))
          case errorEnd() if current.isDefined =>
            val ann = current.map(ann => ann.copy(col = line.length, text = ann.text :+ line)).get
            (acc :+ ann, None)
          case errLine =>
            (acc, current.map(ann => ann.copy(text = ann.text :+ errLine)))
        }
    }
    annotations
  }

  implicit class Pipeable[T](t : T) {
    def |>[V](f : T => V) : V = f(t)
  }

  def doCompile = {
    compile(compiler, sourceCode, _ |> opt |> compiler.export)
  }

  private def compile(compiler : Compiler, code : CompileActor.Source, processor : Seq[VirtualScalaJSIRFile] => String) : CompilerResponse = {
    val output = mutable.Buffer.empty[String]

    val res = compiler.compile(code, output.append(_))
    if (output.nonEmpty)
      log.info(s"Compiler errors: $output")

    val logSpam = output.mkString
    log.info(s"logSpam:$logSpam")
    CompilerResponse(res.map(processor), parseErrors(logSpam), logSpam)
  }
}

sealed abstract class Optimizer

object Optimizer {
  case object Fast extends Optimizer

  case object Full extends Optimizer
}

case class EditorAnnotation(row: Int, col: Int, text: Seq[String], tpe: String)

case class CompilerResponse(jsCode: Option[String], annotations: Seq[EditorAnnotation], log: String)
