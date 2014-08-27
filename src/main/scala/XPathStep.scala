import scala.collection.JavaConversions._
import org.jaxen.expr.{Step, Predicate}
import org.jaxen.expr.{AllNodeStep => JAllNodeStep,
                       CommentNodeStep => JCommentNodeStep,
                       ProcessingInstructionNodeStep => JProcessingInstructionNodeStep,
                       TextNodeStep => JTextNodeStep,
                       NameStep => JNameStep}

/** An XPath location path step (see XPath spec section 2.1) */
case class XPathStep(axis: XPathAxis, test: NodeTest, predicates: Seq[XPathExpr])

/** Base class for node tests (see XPath spec section 2.3) */
abstract class NodeTest
case object AllNodeTest extends NodeTest
case object CommentNodeTest extends NodeTest
case object TextNodeTest extends NodeTest
case class NameTest(name: String) extends NodeTest

/** Factory for [[XPathStep]] instances */
object XPathStep {
  /** Parses a Jaxen [[Step]] and returns an equivalent [[XPathStep]] */
  def parse(step: Step): XPathStep = {
    val axis = XPathAxis(step.getAxis)
    val predicates = step.getPredicates.map(p => XPathExpr.parse(p.asInstanceOf[Predicate].getExpr)).toList
    val nodeTest = step match {
      // ::node()
      case allNode: JAllNodeStep => AllNodeTest
      // ::comment()
      case commentNode: JCommentNodeStep => CommentNodeTest
      // ::text()
      case textNode: JTextNodeStep => TextNodeTest
      // any name (might also be '*')
      case nameStep: JNameStep =>
        assert(nameStep.getPrefix == null || nameStep.getPrefix.length == 0, "Prefixed names are not supported")
        NameTest(nameStep.getLocalName)
      // ::processing-instruction() OR ::processing-instruction('name')
      case piNode: JProcessingInstructionNodeStep => throw new NotImplementedError("Processing instructions are not implemented")
    }
    XPathStep(axis, nodeTest, predicates)
  }
}