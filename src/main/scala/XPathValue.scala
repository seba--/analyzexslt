abstract class XPathValue {
  def toBooleanValue: BooleanValue = {
    // this matches the boolean() function specified in the XPath spec section 4.3
    this match {
      case bool@BooleanValue(_) => bool
      case NumberValue(num) => BooleanValue(num == 0 || num.isNaN)
      case NodeSetValue(nodes) => BooleanValue(!nodes.isEmpty)
      case StringValue(str) => BooleanValue(str.length > 0)
    }
  }

  def toNumberValue: NumberValue = {
    // except for unimplemented cases this matches the number() function specified in the XPath spec section 4.4
    this match {
      case num@NumberValue(_) => num
      case BooleanValue(bool) => if (bool) NumberValue(1) else NumberValue(0)
      case _ => throw new UnsupportedOperationException(f"Conversion of $this to number is not implemented.")
    }
  }

  def toStringValue: StringValue = {
    // except for unimplemented cases this matches the string() function specified in the XPath spec section 4.2
    this match {
      case str@StringValue(_) => str
      case BooleanValue(bool) => StringValue(
        if(bool) "true"
        else "false"
      )
      case NumberValue(num) => StringValue(
        if (num.isNaN) "NaN"
        else if (num.isPosInfinity) "Infinity"
        else if (num.isNegInfinity) "-Infinity"
        else num.toString
      )
      case NodeSetValue(_) => throw new UnsupportedOperationException("Converting node sets to strings is not implemented")
    }
  }
}
case class NodeSetValue(nodes: Seq[XMLNode]) extends XPathValue
case class BooleanValue(value: Boolean) extends XPathValue
case class NumberValue(value: Double) extends XPathValue
case class StringValue(value: String) extends XPathValue