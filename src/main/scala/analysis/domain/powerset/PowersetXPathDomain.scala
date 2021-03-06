package analysis.domain.powerset

import analysis.domain.XPathDomain
import xml._
import xpath._

import scala.collection.immutable.TreeSet

/** Just a wrapper for the type alias */
object PowersetXPathDomain {
  type V = Option[Set[XPathValue]] // None represents the infinite set, Some represents finite sets

  /** This is the actual (partial) domain implementation */
  trait D[N, L] extends XPathDomain[V, N, L] {
    /** Get the TOP element */
    override def top: V = None

    /** Get the BOTTOM element */
    override def bottom: V = BOT

    protected val BOT: V = Some(Set())

    // booleans are a finite domain so we don't need to represent an unknown boolean as None
    protected val anyBoolean: V = Some(Set(BooleanValue(true), BooleanValue(false)))

    /** Join two values. This calculates their supremum (least upper bound). */
    override def join(v1: V, v2: V): V = (v1, v2) match {
      case (None, _) => None
      case (_, None) => None
      case (Some(s1), Some(s2)) => Some(s1.union(s2))
    }

    /** Compares two elements of the lattice.
      * Returns true if v1 < v2 or v1 = v2, false if v1 > v2 or if they are incomparable.
      */
    override def lessThanOrEqual(v1: V, v2: V): Boolean = (v1, v2) match {
      case (_, None) => true
      case (None, Some(_)) => false
      case (Some(s1), Some(s2)) => s1.subsetOf(s2)
    }

    /** Get the TOP element of the subdomain of numbers (representing any number). topNumber <= top must hold. */
    override def topNumber: V = None // no type distinction in this domain

    /** Get the TOP element of the subdomain of strings (representing any string). topString <= top must hold. */
    override def topString: V = None // no type distinction in this domain

    protected def liftBinaryOp(v1: V, v2: V, pf: PartialFunction[(XPathValue, XPathValue), XPathValue]): V = (v1, v2) match {
      case (BOT, _) => BOT
      case (_, BOT) => BOT
      case (Some(s1), Some(s2)) => Some(s1.cross(s2).collect(pf).toSet)
      case _ => None
    }

    protected def liftBinaryLogicalOp(v1: V, v2: V, f: (XPathValue, XPathValue) => XPathValue): V =
      liftBinaryOp(v1, v2, { case (l, r) => f(l, r) }) match {
        case None => anyBoolean
        case Some(s) => Some(s)
      }

    protected def liftBinaryNumOp(v1: V, v2: V, f: (Double, Double) => Double): V = (v1, v2) match {
      case (BOT, _) => BOT
      case (_, BOT) => BOT
      case (Some(s1), Some(s2)) => Some(s1.cross(s2)
        .map { case (l, r) => NumberValue(f(l.toNumberValue.value, r.toNumberValue.value))}
        .toSet
      )
      case _ => None
    }

    /** The addition operation. Must convert its operands to numbers first if they aren't. */
    override def add(v1: V, v2: V): V = liftBinaryNumOp(v1, v2,
      (l, r) => l + r
    )

    /** The subtraction operation. Must convert its operands to numbers first if they aren't. */
    override def subtract(v1: V, v2: V): V = liftBinaryNumOp(v1, v2,
      (l, r) => l - r
    )

    /** The multiplication operation. Must convert its operands to numbers first if they aren't. */
    override def multiply(v1: V, v2: V): V = liftBinaryNumOp(v1, v2,
      (l, r) => l * r
    )

    /** The division operation. Must convert its operands to numbers first if they aren't. */
    override def divide(v1: V, v2: V): V = liftBinaryNumOp(v1, v2,
      (l, r) => l / r
    )

    /** The modulo operation. Must convert its operands to numbers first if they aren't. */
    override def modulo(v1: V, v2: V): V = liftBinaryNumOp(v1, v2,
      (l, r) => l % r
    )

    /** Compares two values using a given relational operator (=, !=, <, >, >=, <=).
      * Must behave according to the XPath specification, section 3.4.
      */
    override def compareRelational(v1: V, v2: V, relOp: RelationalOperator): V = liftBinaryLogicalOp(v1, v2,
      (l, r) => BooleanValue(l.compare(r, relOp))
    )

    /** The numeric negation operation (unary minus). Must convert its operand to a number if it isn't. */
    override def negateNum(v: V): V = v.map(_.map(num => NumberValue(-num.toNumberValue.value)))

    /** Convert a value to a string as defined by the XPath specification section 4.2. */
    override def toStringValue(v: V): V = v.map(_.map(_.toStringValue))

    /** Convert a value to a boolean as defined by the XPath specification section 4.3. */
    override def toNumberValue(v: V): V = v.map(_.map(_.toNumberValue))

    /** Convert a value to a number as defined by the XPath specification section 4.4. */
    override def toBooleanValue(v: V): V = v.map(_.map(_.toBooleanValue))

    /** Concatenate two strings. Operands that are not string values are evaluated to BOTTOM. */
    override def concatStrings(v1: V, v2: V): V = (v1, v2) match {
      case (BOT, _) | (_, BOT) => BOT
      case (Some(s1), Some(s2)) => Some(s1.cross(s2)
        .collect { case (StringValue(str1), StringValue(str2)) => StringValue(str1 + str2) }
        .toSet
      )
      case _ => None // in this case, one parameter is TOP and the other is not BOTTOM
    }

    /** Lift a literal string */
    override def liftString(lit: String): V = Some(Set(StringValue(lit)))

    /** Lift a number */
    override def liftNumber(num: Double): V = Some(Set(NumberValue(num)))

    /** Lift a boolean */
    override def liftBoolean(bool: Boolean): V = Some(Set(BooleanValue(bool)))

    /** The union operator for node-sets. If one of the operands is not a node-set, return BOTTOM. */
    override def nodeSetUnion(v1: V, v2: V): V = liftBinaryOp(v1, v2, {
      case (NodeSetValue(lVal), NodeSetValue(rVal)) => NodeSetValue(lVal ++ rVal)
      // NOTE: ignore values that are not node-sets by not including them in the result (essentially evaluating them to bottom)
    })
  }
}
