package analysis

import analysis.domain.XMLDomain
import xpath._

import scala.collection.mutable.MutableList

class AbstractPatternMatcher[N, L, V](xmlDom: XMLDomain[N, L, V]) {

  /** Returns a value indicating whether a given node matches a location path pattern.
    * NOTE: only supports location path patterns (XSLT spec section 5.2) without predicates
    *
    * First result: nodes for which the path WILL definitely match. When this is BOTTOM it means
    *               that the path can't match the given input.
    * Second result: nodes for which the path WILL definitely NOT match. When this is BOTTOM it
    *                means that the path will always match the given input.
    */
  def matches(node: N, path: LocationPath): (N, N) = {
    // match recursively from right to left
    if (path.steps.isEmpty) {
      // an empty path is always a match, but when it is an absolute path, the current node must be the root node
      if (path.isAbsolute) xmlDom.isRoot(node) else (node, xmlDom.bottom)
    } else {
      val lastStep = path.steps.last
      val restPath = LocationPath(path.steps.dropRight(1), path.isAbsolute)
      if (lastStep.predicates.nonEmpty) throw new NotImplementedError("predicates in paths are not implemented")
      val (lastStepMatches, notLastStepMatches) = lastStep match {
        // child::node()
        case XPathStep(ChildAxis, AllNodeTest, Nil) =>
          val matches = xmlDom.joinAll(List(xmlDom.isElement(node)._1, xmlDom.isTextNode(node)._1, xmlDom.isComment(node)._1))
          val notMatches = xmlDom.join(xmlDom.isRoot(node)._1, xmlDom.isAttribute(node)._1) // TODO: is this always correct?
          (matches, notMatches)
        // child::comment()
        case XPathStep(ChildAxis, CommentNodeTest, Nil) => xmlDom.isComment(node)
        // child::text()
        case XPathStep(ChildAxis, TextNodeTest, Nil) => xmlDom.isTextNode(node)
        // child::*
        case XPathStep(ChildAxis, NameTest(None, "*"), Nil) => xmlDom.isElement(node)
        // child::name
        case XPathStep(ChildAxis, NameTest(None, name), Nil) =>
          val (element, notElement) = xmlDom.isElement(node)
          val (hasName, notHasName) = xmlDom.hasName(element, name)
          (hasName, xmlDom.join(notElement, notHasName))
        // attribute::* OR attribute::node()
        case XPathStep(AttributeAxis, NameTest(None, "*") | AllNodeTest, Nil) => xmlDom.isAttribute(node)
        // attribute::name
        case XPathStep(AttributeAxis, NameTest(None, name), Nil) =>
          val (attr, notAttr) = xmlDom.isAttribute(node)
          val (hasName, notHasName) = xmlDom.hasName(attr, name)
          (hasName, xmlDom.join(notAttr, notHasName))
        // attribute::comment() OR attribute::text() [these can never match anything]
        case XPathStep(AttributeAxis, CommentNodeTest | TextNodeTest, _) => (xmlDom.bottom, node)
        // any step using a name test with a prefixed name
        case XPathStep(_, NameTest(Some(_), _), Nil) => throw new NotImplementedError("Prefixed names are not implemented")
      }

      if (xmlDom.lessThanOrEqual(lastStepMatches, xmlDom.bottom)) {
        (xmlDom.bottom, notLastStepMatches)
      } else {
        // this node could match, but what about the rest of the path?
        if (restPath.steps.nonEmpty && restPath.steps.last == XPathStep(DescendantOrSelfAxis, AllNodeTest, Nil)) {
          // the next step is '//' and must be handled separately (does any ancestor match the rest of the path?)
          val nextRestPath = LocationPath(restPath.steps.dropRight(1), path.isAbsolute)
          var current = lastStepMatches
          var ancestorMatches = xmlDom.bottom
          var notAncestorMatches = node
          var (root, notRoot) = xmlDom.isRoot(current)
          val nodeStack = MutableList(notRoot)
          while (!xmlDom.lessThanOrEqual(notRoot, xmlDom.bottom)) {
            // TODO: this may not terminate (search for fixed point)
            val parent = xmlDom.getParent(notRoot)
            val (parentMatchesRest, _) = matches(parent, nextRestPath)
            var parentMatches = parentMatchesRest

            for (d <- (nodeStack.size - 1) to 1 by -1) {
              // go down in the tree, back to the original level
              val (newParentMatches, _) = hasParent(nodeStack(d), parentMatches)
              parentMatches = newParentMatches
            }

            val (newParentMatches, newNotParentMatches) = hasParent(nodeStack(0), parentMatches)
            ancestorMatches = xmlDom.join(ancestorMatches, newParentMatches)
            notAncestorMatches = xmlDom.meet(notAncestorMatches, newNotParentMatches)

            current = parent
            val (newRoot, newNotRoot) = xmlDom.isRoot(current) // instead of returning two results here, one could add isNotRoot
            root = newRoot
            notRoot = newNotRoot
            nodeStack += notRoot
          }
          (ancestorMatches, xmlDom.join(notAncestorMatches, notLastStepMatches))
        }
        else {
          // does the parent match the rest of the path?
          val parent = xmlDom.getParent(lastStepMatches)
          val (parentMatchesRest, _) = matches(parent, restPath)
          val (parentMatches, notParentMatches) = hasParent(lastStepMatches, parentMatchesRest)
          (parentMatches, xmlDom.join(notParentMatches, notLastStepMatches))
        }
      }
    }
  }

  /** Predicate function that checks whether a node has a specified node as its parent.
    * The first result is a node that is known to have that parent (this is BOTTOM if the node definitely
    * doesn't have that parent), the second result is a node that might not have that parent (this is
    * BOTTOM if the node definitely does have that parent). The two results are not necessarily disjoint.
    */
  private def hasParent(node: N, parent: N): (N, N) = {
    val (isChild, isNotChild) = xmlDom.isContainedIn(node, xmlDom.getChildren(parent))
    val (isAttribute, isNeither) = xmlDom.isContainedIn(isNotChild, xmlDom.getAttributes(parent))
    (xmlDom.join(isChild, isAttribute), isNeither)
  }
}
