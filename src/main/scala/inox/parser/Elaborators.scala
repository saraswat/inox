package inox
package parser

import elaboration._
import elaborators._

trait Elaborators
  extends Trees
     with IRs
     with Constraints
     with SimpleTypes
     with SimpleBindings
     with BindingElaborators
     with ExprElaborators
     with TypeElaborators
     with IdentifierElaborators
     with NumberUtils {

  trait Store {
    def getIdentifier(name: String): Option[inox.Identifier]
    def getField(name: String): Seq[(inox.Identifier, inox.Identifier)]
    def getField(identifier: Identifier): Seq[inox.Identifier]
    def getTypeOfField(sortId: inox.Identifier, fieldId: inox.Identifier): Seq[SimpleTypes.Type] => SimpleTypes.Type
    def getVariable(identifier: inox.Identifier): Option[(SimpleTypes.Type, Eventual[trees.Type])]
    def getType(identifier: inox.Identifier): Option[(SimpleTypes.Type, Eventual[trees.Type])]
    def getTypeConstructor(identifier: inox.Identifier): Option[Int]
    def getFunction(identifier: inox.Identifier): Option[(Int, Seq[SimpleTypes.Type] => (Seq[SimpleTypes.Type], SimpleTypes.Type))]
    def getConstructor(identifier: inox.Identifier): Option[(Int, Seq[SimpleTypes.Type] => (Seq[SimpleTypes.Type], SimpleTypes.Type))]
    def getSortOfConstructor(identifier: inox.Identifier): Option[inox.Identifier]
    def getHole[A: Manifest](index: Int): Option[A]
    val getSymbols: trees.Symbols

    def addVariable(id: inox.Identifier, simpleType: SimpleTypes.Type, eventualType: Eventual[trees.Type]): Store
    def addVariables(triples: Seq[(inox.Identifier, SimpleTypes.Type, Eventual[trees.Type])]): Store
  }

  trait Elaborator[-A, +R] {
    def elaborate(template: A)(implicit store: Store): Constrained[R]
  }

  abstract class HSeqE[-A <: IR, H: Manifest, +R] extends Elaborator[HSeq[A], Seq[R]] {
    val elaborator: Elaborator[A, R]

    def wrap(value: H)(implicit store: Store): Constrained[R]

    override def elaborate(template: HSeq[A])(implicit store: Store): Constrained[Seq[R]] = {
      val elems = template.elems

      Constrained.sequence(elems.map {
        case Left(index) => store.getHole[Seq[H]](index) match {
          case None => Constrained.fail("TODO: Error")
          case Some(xs) => Constrained.sequence(xs.map(wrap(_)))
        }
        case Right(t) => elaborator.elaborate(t).map(Seq(_))
      }).map(_.flatten)
    }
  }
}