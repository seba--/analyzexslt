package analysis.domain.concrete

import analysis.domain.XPathDomain
import xml.XMLNode
import xpath._

import scala.collection.immutable.TreeSet

/** Just a wrapper for the type alias */
object ConcreteXPathDomain {
  type V = SingleValueLattice[XPathValue]

  /** This is the actual (partial) domain implementation */
  trait D[N, L] extends XPathDomain[V, N, L] {
    /** Get the TOP element */
    override def top: V = Top

    /** Get the BOTTOM element */
    override def bottom: V = Bottom

    /** Join two values. This calculates their supremum (least upper bound). */
    override def join(v1: V, v2: V): V = v1.join(v2)

    /** Compares two elements of the lattice.
      * Returns true if v1 < v2 or v1 = v2, false if v1 > v2 or if they are incomparable.
      */
    override def lessThanOrEqual(v1: V, v2: V): Boolean = v1.lessThanOrEqual(v2)

    /** Get the TOP element of the subdomain of numbers (representing any number). topNumber <= top must hold. */
    override def topNumber: V = Top // no type distinction in this domain

    /** Get the TOP element of the subdomain of strings (representing any string). topString <= top must hold. */
    override def topString: V = Top // no type distinction in this domain

    protected def liftBinaryNumOp(v1: V, v2: V)(f: (Double, Double) => Double): V =
        v1.liftBinaryOp(v2) {(l, r) => NumberValue(f(l.toNumberValue.value, r.toNumberValue.value))}

    /** The addition operation. Must convert its operands to numbers first if they aren't. */
    override def add(v1: V, v2: V): V = liftBinaryNumOp(v1, v2) {(l, r) => l + r}

    /** The subtraction operation. Must convert its operands to numbers first if they aren't. */
    override def subtract(v1: V, v2: V): V = liftBinaryNumOp(v1, v2) {(l, r) => l - r}

    /** The multiplication operation. Must convert its operands to numbers first if they aren't. */
    override def multiply(v1: V, v2: V): V = liftBinaryNumOp(v1, v2) {(l, r) => l * r}

    /** The division operation. Must convert its operands to numbers first if they aren't. */
    override def divide(v1: V, v2: V): V = liftBinaryNumOp(v1, v2) {(l, r) => l / r}

    /** The modulo operation. Must convert its operands to numbers first if they aren't. */
    override def modulo(v1: V, v2: V): V = liftBinaryNumOp(v1, v2) {(l, r) => l % r}

    /** Compares two values using a given relational operator (=, !=, <, >, >=, <=).
      * Must behave according to the XPath specification, section 3.4.
      */
    override def compareRelational(v1: V, v2: V, relOp: RelationalOperator): V = v1.liftBinaryOp(v2) {
      (l, r) => BooleanValue(l.compare(r, relOp))
    }

    /** The numeric negation operation (unary minus). Must convert its operand to a number if it isn't. */
    override def negateNum(v: V): V = v.map(n => NumberValue(-n.toNumberValue.value))

    /** Concatenate two strings. Operands that are not string values are evaluated to BOTTOM. */
    override def concatStrings(v1: V, v2: V): V = (v1, v2) match {
      case (Value(l), Value(r)) => (l, r) match {
        case (StringValue(str1), StringValue(str2)) => Value(StringValue(str1 + str2))
        case _ => Bottom // wrong argument types
      }
      case (Bottom, _) => Bottom
      case (_, Bottom) => Bottom
      case _ => Top
    }

    /** Lift a literal string */
    override def liftString(lit: String): V = Value(StringValue(lit))

    /** Lift a number */
    override def liftNumber(num: Double): V = Value(NumberValue(num))

    /** Lift a boolean */
    override def liftBoolean(bool: Boolean): V = Value(BooleanValue(bool))

    /** The union operator for node-sets. If one of the operands is not a node-set, return BOTTOM. */
    override def nodeSetUnion(v1: V, v2: V): V = (v1, v2) match {
      case (Value(NodeSetValue(lVal)), Value(NodeSetValue(rVal))) =>
        Value(NodeSetValue(lVal ++ rVal))
      case (Value(_), Value(_)) => Bottom // values of incompatible type -> error/bottom
      case (Bottom, _) => Bottom
      case (_, Bottom) => Bottom
      case _ => Top
    }

    /** Convert a value to a string as defined by the XPath specification section 4.2. */
    override def toStringValue(v: V): V = v.map(_.toStringValue)

    /** Convert a value to a boolean as defined by the XPath specification section 4.3. */
    override def toBooleanValue(v: V): V = v.map(_.toBooleanValue)

    /** Convert a value to a number as defined by the XPath specification section 4.4. */
    override def toNumberValue(v: V): V = v.map(_.toNumberValue)
  }
}
