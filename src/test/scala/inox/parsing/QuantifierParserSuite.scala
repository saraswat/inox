package inox
package parsing

import org.scalatest._

class QuantifierParserSuite extends FunSuite {

  import inox.trees._
  import interpolator._
  implicit val symbols = NoSymbols

  test("Parsing forall.") {

    e"forall x. x > 2" match {
      case Forall(Seq(ValDef(id, IntegerType, _)), expr) => 
        assertResult(GreaterThan(Variable(id, IntegerType, Set()), IntegerLiteral(2))) {
          expr
        }
      case e => fail("Unexpected shape: " + e)
    }

    e"forall x: BigInt. false ==> true" match {
      case Forall(Seq(ValDef(id, IntegerType, _)), expr) => 
        assertResult(Implies(BooleanLiteral(false), BooleanLiteral(true))) {
          expr
        }
      case e => fail("Unexpected shape: " + e)
    }

    e"true && forall x: BigInt. false ==> true" match {
      case And(Seq(BooleanLiteral(true), Forall(Seq(ValDef(id, IntegerType, _)), expr))) => 
        assertResult(Implies(BooleanLiteral(false), BooleanLiteral(true))) {
          expr
        }
      case e => fail("Unexpected shape: " + e)
    }

    e"forall f, x: Int, y, z. f(f(x, y), z) == f(x, f(y, z))" match {
      case Forall(Seq(ValDef(idF, FunctionType(Seq(Int32Type, Int32Type), Int32Type), _),
                      ValDef(idX, Int32Type, _),
                      ValDef(idY, Int32Type, _),
                      ValDef(idZ, Int32Type, _)), expr) => {
        val f = Variable(idF, FunctionType(Seq(Int32Type, Int32Type), Int32Type), Set())
        val x = Variable(idX, Int32Type, Set())
        val y = Variable(idY, Int32Type, Set())
        val z = Variable(idZ, Int32Type, Set())

        assertResult(Equals(Application(f, Seq(Application(f, Seq(x, y)), z)),
                            Application(f, Seq(x, Application(f, Seq(y, z)))))) {
          expr
        }
      }
    }
  }

  test("Parsing exists.") {

    e"exists x. x > 2" match {
      case Not(Forall(Seq(ValDef(id, IntegerType, _)), Not(expr))) => 
        assertResult(GreaterThan(Variable(id, IntegerType, Set()), IntegerLiteral(2))) {
          expr
        }
      case e => fail("Unexpected shape: " + e)
    }

    e"exists x: BigInt. false ==> true" match {
      case Not(Forall(Seq(ValDef(id, IntegerType, _)), Not(expr))) => 
        assertResult(Implies(BooleanLiteral(false), BooleanLiteral(true))) {
          expr
        }
      case e => fail("Unexpected shape: " + e)
    }

    e"true && exists x: BigInt. false ==> true" match {
      case And(Seq(BooleanLiteral(true), Not(Forall(Seq(ValDef(id, IntegerType, _)), Not(expr))))) => 
        assertResult(Implies(BooleanLiteral(false), BooleanLiteral(true))) {
          expr
        }
      case e => fail("Unexpected shape: " + e)
    }

    e"exists f, x: Int, y, z. f(f(x, y), z) == f(x, f(y, z))" match {
      case Not(Forall(Seq(ValDef(idF, FunctionType(Seq(Int32Type, Int32Type), Int32Type), _),
                          ValDef(idX, Int32Type, _),
                          ValDef(idY, Int32Type, _),
                          ValDef(idZ, Int32Type, _)), Not(expr))) => {
        val f = Variable(idF, FunctionType(Seq(Int32Type, Int32Type), Int32Type), Set())
        val x = Variable(idX, Int32Type, Set())
        val y = Variable(idY, Int32Type, Set())
        val z = Variable(idZ, Int32Type, Set())

        assertResult(Equals(Application(f, Seq(Application(f, Seq(x, y)), z)),
                            Application(f, Seq(x, Application(f, Seq(y, z)))))) {
          expr
        }
      }
    }
  }
}