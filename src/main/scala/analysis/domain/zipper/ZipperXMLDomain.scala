package analysis.domain.zipper

import analysis.domain.{XPathDomain, XMLDomain, Lattice}

/** Just a wrapper for the type aliases */
object ZipperXMLDomain {
  type S = Subtree // type of subtrees
  type P = Set[Path] // type of paths
  type N = (S, P) // a node is a subtree and a path
  type L = ZList[N]

  abstract class NodeDescriptor // TODO: add AnyElementNode (`*`), and similar ... (don't need Option[...] any more)
  case object RootNode extends NodeDescriptor
  case class ElementNode(name: String) extends NodeDescriptor
  case class AttributeNode(name: String, value: String) extends NodeDescriptor
  case class TextNode(value: String) extends NodeDescriptor
  case class CommentNode(value: String) extends NodeDescriptor

  // TODO: seperate attributes and children (also while evaluating templates)
  case class Subtree(desc: Option[Set[NodeDescriptor]], children: ZList[Subtree])

  implicit object SubtreeLattice extends Lattice[Subtree] {
    def top = Subtree(None, ZTop())
    def bottom = Subtree(Some(Set()), ZBottom())
    def join(left: Subtree, right: Subtree): Subtree = Subtree(latD.join(left.desc, right.desc), left.children | right.children)
    def meet(left: Subtree, right: Subtree): Subtree = Subtree(latD.meet(left.desc, right.desc), left.children & right.children)
    def lessThanOrEqual(left: Subtree, right: Subtree): Boolean = latD.lessThanOrEqual(left.desc, right.desc) && left.children <= right.children
  }

  private val latS: Lattice[S] = SubtreeLattice // lattice for subtrees
  private val latP = Path.PathSetLattice // lattice for paths
  private val latD = Lattice.createFromOptionalSet[NodeDescriptor] // lattice for descriptors

  implicit object NodeLattice extends Lattice[N] {
    def top = (latS.top, latP.top)
    def bottom = (latS.bottom, latP.bottom)
    def join(left: N, right: N): N = normalize(latS.join(left._1, right._1), latP.join(left._2, right._2)) // TODO: normalize is probably not required here
    def meet(left: N, right: N): N = normalize(latS.meet(left._1, right._1), latP.meet(left._2, right._2))

    def lessThanOrEqual(left: N, right: N): Boolean =
      latS.lessThanOrEqual(left._1, right._1) && latP.lessThanOrEqual(left._2, right._2)
  }

  private def getDescriptorsFromPaths(path: P): Option[Set[NodeDescriptor]] = {
    def getSingle(desc: PatternStepDescriptor): Option[NodeDescriptor] = desc match {
        // TODO: support more cases in NodeDescriptor
      case AnyElement => None // TOP
      case NamedElement(name) => Some(ElementNode(name))
      case AnyAttribute => None
      case NamedAttribute(name) => None
      case AnyTextNode => None
      case AnyCommentNode => None
    }

    Some(path.map {
      case RootPath => RootNode
      case ChildStep(desc, _) => getSingle(desc) match {
        case Some(res) => res
        case None => return None
      }
      case DescendantStep(desc, _) => getSingle(desc) match {
        case Some(res) => res
        case None => return None
      }
    })
  }

  private def getPathsFromDescriptors(descriptors: Option[Set[NodeDescriptor]]): P = descriptors match {
    case None => latP.top
    case Some(s) => s.map {
      case RootNode => RootPath
      case ElementNode(name) => DescendantStep(NamedElement(name), RootPath)
      case AttributeNode(name, _) => DescendantStep(NamedAttribute(name), RootPath)
      case TextNode(_) => DescendantStep(AnyTextNode, RootPath)
      case CommentNode(_) => DescendantStep(AnyCommentNode, RootPath)
    }
  }

  /** Removes impossible elements (where Path and Subtree descriptors don't agree) */
  private def normalize(node: N) = {
    // TODO: further refinements (e.g. if the descriptor only describes nodes that can't have children, set children to ZNil)
    //       or more general: eliminate all children that can not have the descriptor as their parent (recursively?)
    val (Subtree(desc, children), path) = node
    if (children.isInstanceOf[ZBottom[Subtree]]) {
      NodeLattice.bottom
    } else {
      val meetDesc = latD.meet(getDescriptorsFromPaths(path), desc)
      if (meetDesc == Some(Set())) { // BOTTOM
        NodeLattice.bottom // necessary to make children BOTTOM also (which would not happen in the below case)
      } else {
        (Subtree(meetDesc, children), latP.meet(getPathsFromDescriptors(desc), path))
      }
    }
  }

  val topTree = latS.top

  /** This is the actual (partial) domain implementation */
  trait D[V] extends XMLDomain[N, L, V] {
    val xpathDom: XPathDomain[V, N, L]

    /** Get the TOP element for XML nodes. */
    override def top: N = NodeLattice.top

    /** Gets the BOTTOM element for XML nodes. */
    override def bottom: N = NodeLattice.bottom

    /** Get the TOP element for XML node lists. */
    override def topList: L = ZTop()

    // TODO: this is currently never used

    /** Gets the BOTTOM element for XML node lists. */
    override def bottomList: L = ZBottom()

    /** Calcucate the join of two abstract nodes. This is the supremum (least upper bound). */
    override def join(n1: N, n2: N): N = NodeLattice.join(n1, n2)

    /** Calculate the meet of two abstract nodes. This is the infimum (greatest lower bound). */
    override def meet(n1: N, n2: N): N = NodeLattice.meet(n1, n2)

    /** Join two node lists. This calculates their supremum (least upper bound). */
    override def joinList(l1: L, l2: L): L = l1 | l2

    /** Compares two elements of the lattice of nodes.
      * Returns true if n1 < n2 or n1 = n2, false if n1 > n2 or if they are incomparable.
      */
    override def lessThanOrEqual(n1: N, n2: N): Boolean = NodeLattice.lessThanOrEqual(n1, n2)

    /** Compares two elements of the lattice of node lists.
      * Returns true if l1 < l2 or l1 = l2, false if l1 > l2 or if they are incomparable.
      */
    override def lessThanOrEqualList(l1: L, l2: L): Boolean = l1 <= l2

    // TODO: is this operation really needed (could be replaced with isBottom)?

    /** Create an element node with the given name, attributes and children.
      * The output is created bottom-up, so children are always created before their parent nodes.
      */
    override def createElement(name: String, attributes: L, children: L): N = {
      // TODO: attributes should be a map (name -> value), not a list
      // TODO: empty text nodes should be filtered out and multiple consecutive ones should be merged
      val tree = Subtree(Some(Set(ElementNode(name))), attributes.map(_._1) ++ children.map(_._1))
      val path = Set[Path](DescendantStep(AnyElement, RootPath))
      (tree, path)
    }

    /** Create an emtpy list containing no nodes */
    override def createEmptyList(): L = ZNil()

    /** Create a list containing a single abstract node */
    override def createSingletonList(node: N): L = ZCons(node, ZNil())

    /** Get the root node of a given node */
    override def getRoot(node: N): N = {
      val (Subtree(desc, children), path) = node
      val root: P = Set(RootPath)
      normalize(Subtree(latD.meet(desc, getDescriptorsFromPaths(root)), children), latP.meet(path, root))
    }

    // TODO: this might be implementable using getParent() and isRoot()

    /** Get the list of attributes of a given node.
      * Nodes that are not an element (and therefore don't have attributes) return an empty list, not BOTTOM! */
    override def getAttributes(node: N): L = {
      val (Subtree(desc, children), path) = node
      val attributePath: Set[Path] = latP.getAttributes(path).joinInner
      children.map(tree => normalize(tree, attributePath)) // normalize throws out subtrees that are not attributes (TODO: check that)
    }

    /** Get the list of children of a given node.
      * Root nodes have a single child, element nodes have an arbitrary number of children.
      * Nodes that don't have children return an empty list, not BOTTOM! */
    override def getChildren(node: N): L = {
      val (Subtree(desc, children), path) = node
      val childrenPath: Set[Path] = latP.getChildren(path).joinInner
      children.map(tree => normalize(tree, childrenPath)) // normalize throws out subtrees that are attributes (TODO: check that)
    }

    /** Get the parent of given node. If the node has no parent (root node), BOTTOM is returned. */
    override def getParent(node: N): N = {
      val (Subtree(desc, children), path) = node
      val parent = latP.getParent(path)
      val newChildren: ZList[Subtree] = ZTop() // don't know anything about siblings of `node`
      normalize(Subtree(getDescriptorsFromPaths(parent), newChildren), parent)
    }

    /** Predicate function that checks whether a node is in a given list of nodes.
      * The first result is a node that is known to be in that list (this is BOTTOM if the node definitely
      * is not in the list), the second result is a node that might not be in the list (this is
      * BOTTOM if the node definitely is contained in the list). The two results are not necessarily disjoint.
      */
    override def isContainedIn(node: N, list: L): (N, N) = (list.contains(node), node)

    /** Concatenates two lists. */
    override def concatLists(list1: L, list2: L): L = list1 ++ list2

    /** Partitions a node list in such a way that the first result contains all attribute nodes from the beginning of
      * the list (as soon as there are other node types in the list, attributes are ignored) and the second result
      * contains all other nodes.
      */
    override def partitionAttributes(list: L): (L, L) = {
      def createLists(elems: N): (L, L) = {
        val attrs = isAttribute(elems)._1
        val children = join(List(isElement(elems)._1, isTextNode(elems)._1, isComment(elems)._1))
        val attrList: L = if (lessThanOrEqual(attrs, NodeLattice.bottom))
          ZNil()
        else if (lessThanOrEqual(NodeLattice.top, attrs))
          ZTop()
        else
          ZUnknownLength(attrs)

        val childList: L = if (lessThanOrEqual(children, NodeLattice.bottom))
          ZNil()
        else if (lessThanOrEqual(NodeLattice.top, children))
          ZTop()
        else
          ZUnknownLength(children)

        (attrList, childList)
    }

    list match {
      case ZBottom() => (ZBottom(), ZBottom())
      case ZTop() => (ZTop(), ZTop()) // TODO: this can be more specific using ZUnknownLength
      case ZUnknownLength(elems) => createLists(elems)
      case ZCons(first, rest) => createLists(list.joinInner) // TODO: this can be more specific (use ZCons but make first result MaybeNil if there is something that's not an attribute)
      case ZMaybeNil(first, rest) => createLists(list.joinInner) // TODO: see above
      case ZNil() => (ZNil(), ZNil())
    }
  }

    /** Wraps a list of nodes in a document/root node. Lists that don't have exactly one element evaluate to BOTTOM. */
    override def wrapInRoot(list: L): N = {
      val firstChild: N = list match {
        case ZTop() => top
        case ZBottom() => bottom
        case ZUnknownLength(elems) => elems
        case ZCons(first, ZCons(_, _)) => bottom // list with more than one element
        case ZCons(first, _) => first // list with at least one element
        case ZMaybeNil(first, ZCons(_, _)) => bottom // list with 0 or more than one element (at least 2)
        case ZMaybeNil(first, _) => first // list with 0 or more elements (can't know exactly)
        case ZNil() => bottom // list with 0 elements
      }
      val (firstChildElement, _) = isElement(firstChild)
      normalize(Subtree(Some(Set(RootNode)), ZList(List(firstChildElement._1))), Set(RootPath))
    }

    /** Copies a list of nodes, so that they can be used in the output.
      * A root node is copied by copying its child (not wrapped in a root node). */
    override def copyToOutput(list: L): L = list.map {
      case in@(Subtree(desc, children), path) =>
        val (root, notRoot) = isRoot(in)
        if (!lessThanOrEqual(root, bottom)) { // isRoot is not BOTTOM -> node might be a root node
          val child = getChildren(in).first
          val (tree, _) = join(notRoot, child)
          normalize(tree, latP.top)
        } else {
          normalize(Subtree(desc, children), latP.top)
        }
    }

    /** Evaluates a function for every element in the given list, providing also the index of each element in the list.
      * The resulting lists are flattened into a single list.
      */
    override def flatMapWithIndex(list: L, f: (N, V) => L): L = {
      def flatMapWithIndexInternal(rest: L, currentIndex: V, f: (N, V) => L): L = rest match {
        case ZBottom() => ZBottom()
        case ZTop() => ZTop()
        case ZUnknownLength(elems) => ZUnknownLength(f(elems, xpathDom.topNumber).joinInner)
        case ZCons(first, rest) =>
          f(first, currentIndex) ++ flatMapWithIndexInternal(rest, xpathDom.add(currentIndex, xpathDom.liftNumber(1)), f)
        case ZMaybeNil(first, rest) =>
          ZNil() | f(first, currentIndex) ++ flatMapWithIndexInternal(rest, xpathDom.add(currentIndex, xpathDom.liftNumber(1)), f)
        case ZNil() => ZNil()
      }

      flatMapWithIndexInternal(list, xpathDom.liftNumber(0), f)
    }

    /** Gets the size of a node list */
    override def getNodeListSize(list: L): V = list match {
      case ZBottom() => xpathDom.bottom
      case ZTop() => xpathDom.topNumber
      case ZUnknownLength(elems) => xpathDom.topNumber
      case ZCons(first, rest) => xpathDom.add(xpathDom.liftNumber(1), getNodeListSize(rest))
      case ZMaybeNil(first, rest) => xpathDom.join(xpathDom.liftNumber(0), xpathDom.add(xpathDom.liftNumber(1), getNodeListSize(rest)))
      case ZNil() => xpathDom.liftNumber(0)
    }

    /** Gets the string-value of a node, as specified in the XSLT specification */
    override def getStringValue(node: N): V = {
      def getStringValueFromSubtree(tree: Subtree): V = {
        val Subtree(desc, children) = tree
        desc match {
          case None => xpathDom.topString
          case Some(s) => xpathDom.join(s.map {
            case RootNode => getStringValueFromSubtree(children.first)
            case ElementNode(name) => xpathDom.topString // TODO: concatenate the string values of all (non-attribute) children
            case AttributeNode(name, value) => xpathDom.liftLiteral(value)
            case TextNode(value) => xpathDom.liftLiteral(value)
            case CommentNode(value) => xpathDom.liftLiteral(value)
          })
        }
      }
      getStringValueFromSubtree(node._1)
    }

    /** Predicate function that checks whether a node is a root node.
      * The first result is a node that is known to be a root node (this is BOTTOM if the node definitely
      * is not a root node), the second result is a node that might not be a root node (this is
      * BOTTOM if the node definitely is a root node). The two results are not necessarily disjoint.
      */
    override def isRoot(node: N): (N, N) = {
      val (Subtree(desc, children), path) = node
      // TODO: this might be problematic because we don't gain any information about the children
      val positiveResult: N = normalize(Subtree(latD.meet(desc, Some(Set(RootNode))), children), latP.meet(path, Set(RootPath)))
      val negativeDesc = desc match {
        case None => None
        case Some(s) => Some(s.diff(Set(RootNode)))
      }
      val negativeResult: N = normalize(Subtree(negativeDesc, children), path.diff(Set(RootPath)))
      (positiveResult, negativeResult)
    }

    /** Predicate function that checks whether a node is an element node.
      * The first result is a node that is known to be an element node (this is BOTTOM if the node definitely
      * is not an element node), the second result is a node that might not be an element node (this is
      * BOTTOM if the node definitely is an element node). The two results are not necessarily disjoint.
      */
    override def isElement(node: N): (N, N) = {
      val (Subtree(desc, children), path) = node
      val (pathYes, pathNo) = latP.isElement(path)
      val (descYes, descNo) = desc match {
        case None => (None, None)
        case Some(s) =>
          val (y, n) = s.partition {
            case ElementNode(_) => true
            case _ => false
          }
          (Some(y), Some(n))
      }
      (normalize(Subtree(descYes, children), pathYes), normalize(Subtree(descNo, children), pathNo))
    }

    /** Predicate function that checks whether a node is a text node.
      * The first result is a node that is known to be a text node (this is BOTTOM if the node definitely
      * is not a text node), the second result is a node that might not be a text node (this is
      * BOTTOM if the node definitely is a text node). The two results are not necessarily disjoint.
      */
    override def isTextNode(node: N): (N, N) = {
      val (Subtree(desc, children), path) = node
      val (pathYes, pathNo) = latP.isTextNode(path)
      val (descYes, descNo) = desc match {
        case None => (None, None)
        case Some(s) =>
          val (y, n) = s.partition {
            case TextNode(_) => true
            case _ => false
          }
          (Some(y), Some(n))
      }
      (normalize(Subtree(descYes, children), pathYes), normalize(Subtree(descNo, children), pathNo))
    }

    /** Predicate function that checks whether a node is a comment node.
      * The first result is a node that is known to be a comment node (this is BOTTOM if the node definitely
      * is not a comment node), the second result is a node that might not be a comment node (this is
      * BOTTOM if the node definitely is a comment node). The two results are not necessarily disjoint.
      */
    override def isComment(node: N): (N, N) = {
      val (Subtree(desc, children), path) = node
      val (pathYes, pathNo) = latP.isComment(path)
      val (descYes, descNo) = desc match {
        case None => (None, None)
        case Some(s) =>
          val (y, n) = s.partition {
            case CommentNode(_) => true
            case _ => false
          }
          (Some(y), Some(n))
      }
      (normalize(Subtree(descYes, children), pathYes), normalize(Subtree(descNo, children), pathNo))
    }

    /** Predicate function that checks whether a node is an attribute node.
      * The first result is a node that is known to be an attribute node (this is BOTTOM if the node definitely
      * is not an attribute node), the second result is a node that might not be an attribute node (this is
      * BOTTOM if the node definitely is an attribute node). The two results are not necessarily disjoint.
      */
    override def isAttribute(node: N): (N, N) = {
      val (Subtree(desc, children), path) = node
      val (pathYes, pathNo) = latP.isAttribute(path)
      val (descYes, descNo) = desc match {
        case None => (None, None)
        case Some(s) =>
          val (y, n) = s.partition {
            case AttributeNode(_, _) => true
            case _ => false
          }
          (Some(y), Some(n))
      }
      (normalize(Subtree(descYes, children), pathYes), normalize(Subtree(descNo, children), pathNo))
    }

    /** Predicate function that checks whether a node has a specified name.
      * The first result is a node that is known to have that name (this is BOTTOM if the node definitely
      * doesn't have that name), the second result is a node that might not have that name (this is
      * BOTTOM if the node definitely does have that name). The two results are not necessarily disjoint.
      * Nodes that don't have a name (any node except element and attribute nodes) are evaluated to BOTTOM.
      */
    override def hasName(node: N, name: String): (N, N) = {
      val (Subtree(desc, children), path) = node
      val (pathYes, pathNo) = latP.hasName(path, name)
      val (descYes, descNo) = desc match {
        case None => (None, None) // TODO: these can probably be expressed more precisely with `NamedElement`, etc
        case Some(s) =>
          val (y, n) = s.partition {
            case AttributeNode(n, _) if name == n => true
            case ElementNode(n) if name == n => true
            case _ => false
          }
          (Some(y), Some(n))
      }

      (normalize(Subtree(descYes, children), pathYes), normalize(Subtree(descNo, children), pathNo))
    }

    /** Get the name for a given node. Nodes that don't have a name (i.e. are not an element or attribute node)
      * are evaluated to the empty string, not BOTTOM!
      */
    override def getNodeName(node: N): V = {
      val (Subtree(desc, children), path) = node
      desc match {
        case None => xpathDom.topString
        case Some(s) => xpathDom.join(s.map {
          case ElementNode(name) => xpathDom.liftLiteral(name)
          case AttributeNode(name, _) => xpathDom.liftLiteral(name)
          case _ => xpathDom.liftLiteral("")
        })
      }
    }

    /** Concatenates the values of all text nodes in the list. List elements that are not text nodes are ignored. */
    override def getConcatenatedTextNodeValues(list: L): V = xpathDom.topString // TODO

    /** Filters a list using a given predicate function. The predicate function should never return a node
      * (as its first result) that is less precise than the input node.
      */
    override def filter(list: L, predicate: N => (N, N)): L = list match {
      case ZBottom() => ZBottom()
      case ZTop() =>
        ZUnknownLength(predicate(top)._1)
      case ZUnknownLength(elems) => ZUnknownLength(predicate(elems)._1)
      case ZCons(first, rest) =>
        val result = predicate(first)._1
        val restResult = filter(rest, predicate)
        if (lessThanOrEqual(first, result)) { // first == result (because predicate application should never yield something greater)
          ZCons(first, restResult.asInstanceOf[ZListElement[(S, P)]]) // ... so nothing is filtered out
        } else if (lessThanOrEqual(result, bottom)) { // result == BOTTOM
          restResult // current node is filtered out completely
        } else {
          restResult | ZCons(first, restResult.asInstanceOf[ZListElement[(S, P)]])
        }
      case ZMaybeNil(first, rest) =>
        val result = predicate(first)._1
        val restResult = filter(rest, predicate)
        if (lessThanOrEqual(first, result)) { // first == result (because predicate application should never yield something greater)
          ZMaybeNil(first, restResult.asInstanceOf[ZListElement[(S, P)]]) // ... so nothing is filtered out
        } else if (lessThanOrEqual(result, bottom)) { // result == BOTTOM
          restResult // current node is filtered out completely
        } else {
          restResult | ZMaybeNil(first, restResult.asInstanceOf[ZListElement[(S, P)]])
        }
      case ZNil() => ZNil()
    }

    /** Gets the first node out of a node list. BOTTOM is returned if the list is empty or BOTTOM. */
    override def getFirst(list: L): N = list.first
  }
}
