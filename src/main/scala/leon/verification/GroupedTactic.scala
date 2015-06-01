/* Copyright 2009-2015 EPFL, Lausanne */

package leon
package verification

import leon.utils.NoPosition
import purescala.Expressions._
import purescala.ExprOps._
import purescala.Definitions._
import purescala.Constructors._

class GroupedTactic(vctx: VerificationContext) extends Tactic(vctx) {
    val description = "Default verification condition generation approach"

    def generatePostconditions(fd: FunDef): Seq[VC] = {
      // We collectively generate postconditions for a scc of the call graph.
      // To not repeat vcs, we follow the convention to generate them for the first
      // FunDef of the vcc according to unique name
      val component = vctx.program.callGraph.stronglyConnectedComponent(fd).toSeq sortBy (_.id.uniqueName)
      val perFunc:Seq[Expr] = for {
        generator <- component.find( _.postcondition.isDefined).toSeq
        if generator == fd
        inComp <- component
        p <- inComp.postcondition
        b <- inComp.body
      } yield { implies(
        precOrTrue(inComp),
        // @mk: Don't know which one is better. Inline the body, or have it as a fun. app?
        //application(p, Seq(b))
        application(p, Seq(FunctionInvocation(inComp.typedWithDef, inComp.params map { _.toVariable })))
      )}

      if (perFunc.nonEmpty)
        Seq(VC(andJoin(perFunc.toSeq), fd, VCKinds.Postcondition, this).setPos(fd.postcondition.get))
      else Nil

    }

    def generatePreconditions(fd: FunDef): Seq[VC] = {
      fd.body match {
        case Some(body) =>
          val calls = collectWithPC {
            case c @ FunctionInvocation(tfd, _) if tfd.hasPrecondition => (c, tfd.precondition.get)
          }(body)

          calls.map {
            case ((fi @ FunctionInvocation(tfd, args), pre), path) =>
              val pre2 = replaceFromIDs((tfd.params.map(_.id) zip args).toMap, pre)
              val vc = implies(and(precOrTrue(fd), path), pre2)
              val fiS = sizeLimit(fi.toString, 40)
              VC(vc, fd, VCKinds.Info(VCKinds.Precondition, s"call $fiS"), this).setPos(fi)
          }

        case None =>
          Nil
      }
    }

    def generateCorrectnessConditions(fd: FunDef): Seq[VC] = {
      fd.body match {
        case Some(body) =>
          val calls = collectWithPC {
            case e @ Error(_, "Match is non-exhaustive") =>
              (e, VCKinds.ExhaustiveMatch, BooleanLiteral(false))


            case e @ Error(_, _) =>
              (e, VCKinds.Assert, BooleanLiteral(false))

            case a @ Assert(cond, Some(err), _) => 
              val kind = if (err.startsWith("Map ")) {
                VCKinds.MapUsage
              } else if (err.startsWith("Array ")) {
                VCKinds.ArrayUsage
              } else if (err.startsWith("Division ")) {
                VCKinds.DivisionByZero
              } else {
                VCKinds.Assert
              }

              (a, kind, cond)
            case a @ Assert(cond, None, _) => (a, VCKinds.Assert, cond)
            // Only triggered for inner ensurings, general postconditions are handled by generatePostconditions
            case a @ Ensuring(body, post) => (a, VCKinds.Assert, application(post, Seq(body)))
          }(body)

          calls.map {
            case ((e, kind, errorCond), path) =>
              val vc = implies(and(precOrTrue(fd), path), errorCond)

              VC(vc, fd, kind, this).setPos(e)
          }

        case None =>
          Nil
      }
    }
}
