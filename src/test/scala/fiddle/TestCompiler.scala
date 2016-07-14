package fiddle

import org.junit.Test

class TestCompiler {
  
    val source = 
      """
package example

import scala.scalajs.js
import js.annotation._
import org.scalajs.dom

@JSExport
class Foo(val x: Int) {
  override def toString(): String = s"Foo($x)"
}

@JSExportAll
object HelloWorld extends js.JSApp {

  def main(): Unit = {
    println("Hello world!")
  }
  
  def alert = dom.window.alert("Hello!")
}
    """
    
  @Test
  def testCompilerFast : Unit = {
    val compiler = new ScalaJsCompiler
    val script = compiler.compileScalaJsString(source, Optimizer.Fast, "../")
    println(s"Fast script size ${script.length}B")
  }    
    
  @Test
  def testCompilerFull : Unit = {
    val compiler = new ScalaJsCompiler
    val script = compiler.compileScalaJsString(source, Optimizer.Full, "../")
    println(s"Full script size ${script.length}B")
  }
  
  @Test
  def testCompilationError : Unit = {
    val bug =
      """
        import scala.scalajs.js
        import js.annotation.JSExport
        
        object Buggish extends js.JSApp {
          @JSExport
          def main = println("Bug") + println("Me")
        }
      """
    try {
      val compiler = new ScalaJsCompiler
      compiler.compileScalaJsString(bug, Optimizer.Fast, "../")
    } catch {
      case e : Throwable =>
        println(e.getMessage)
        println("Bug found!")
    }
  }
}