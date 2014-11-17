package analysis.domain.zipper

import analysis.domain.XMLDomain

abstract class NodeDescriptor
case object RootNode extends NodeDescriptor
case class ElementNode(name: String) extends NodeDescriptor
case class AttributeNode(name: String, value: String) extends NodeDescriptor
case class TextNode(value: String) extends NodeDescriptor
case class CommentNode(value: String) extends NodeDescriptor

case class ZipperTree(desc: Option[Set[NodeDescriptor]], children: ZListLattice[ZipperTree])

// left siblings are in reverse order
abstract class ZipperPath {
  def getDescriptor: Option[Set[NodeDescriptor]]
  def getLeftSiblings: ZListLattice[ZipperTree]
  def getRightSiblings: ZListLattice[ZipperTree]
  def getParent: Option[Set[ZipperPath]]
}

case class ChildPath(desc: Option[Set[NodeDescriptor]], left: ZListLattice[ZipperTree], parent: Option[Set[ZipperPath]], right: ZListLattice[ZipperTree]) extends ZipperPath {
  override def getDescriptor: Option[Set[NodeDescriptor]] = desc
  override def getLeftSiblings: ZListLattice[ZipperTree] = right
  override def getRightSiblings: ZListLattice[ZipperTree] = left
  override def getParent: Option[Set[ZipperPath]] = parent
}
// TODO: add DescendantPath (and replace parent: Option[Set[ChildPath]] with Set[...Path] because None is representable as DescendantPath(..., RootPath)

/** Just a wrapper for the type aliases */
object ZipperXMLDomain {
  type N = (ZipperTree, Set[ZipperPath])
  type L = Unit

  private def getDescriptorFlat(path: Option[Set[ZipperPath]]): Option[Set[NodeDescriptor]] = path match {
    case None => None
    case Some(s) => Some(s.map(_.getDescriptor).map {
      case None => return None
      case Some(s) => s
    }.flatten)
  }

  private def getParentFlat(path: Option[Set[ZipperPath]]): Set[ZipperPath] = path match {
    case None => topPath
    case Some(s) => s.map(_.getParent).map {
      case None => return topPath
      case Some(s) => s
    }.flatten
  }

  private def joinTree(left: ZipperTree, right: ZipperTree): ZipperTree = ???

  private def getLeftSiblingsFlat(path: Option[Set[ZipperPath]]): ZListLattice[ZipperTree] = path match {
    case None => ZTop
    case Some(s) => ZListLattice.join(s.map(_.getLeftSiblings), joinTree)
  }

  private def getRightSiblingsFlat(path: Option[Set[ZipperPath]]): ZListLattice[ZipperTree] = path match {
    case None => ZTop
    case Some(s) => ZListLattice.join(s.map(_.getRightSiblings), joinTree)
  }

  def assertConsistency(node: N) = {
    val (tree, path) = node
    assert(tree.desc == getDescriptorFlat(Some(path)))
    path.foreach {
      case ChildPath(Some(desc), _, Some(parent), _) => if (desc.size == 0) assert(parent.size == 0)
    }
  }

  val topTree = ZipperTree(None, ZTop)
  val topPath: Set[ZipperPath] = Set(ChildPath(None, ZTop, None, ZTop))

  /** This is the actual (partial) domain implementation */
  trait D[V] extends XMLDomain[N, L, V] {
    /** Get the TOP element for XML nodes. */
    override def top: N = (topTree, topPath)

    /** Gets the BOTTOM element for XML nodes. */
    override def bottom: N = (ZipperTree(Some(Set()), ZBottom), Set())

    /** Get the TOP element for XML node lists.*/
    override def topList: L = ???
    // TODO: this is currently never used

    /** Gets the BOTTOM element for XML node lists. */
    override def bottomList: L = ???

    /** Calcucate the join of two abstract nodes. This is the supremum (least upper bound). */
    override def join(n1: N, n2: N): N = ???

    /** Calculate the meet of two abstract nodes. This is the infimum (greatest lower bound). */
    override def meet(n1: N, n2: N): N = ???

    /** Join two node lists. This calculates their supremum (least upper bound). */
    override def joinList(l1: L, l2: L): L = ???

    /** Compares two elements of the lattice of nodes.
      * Returns true if n1 < n2 or n1 = n2, false if n1 > n2 or if they are incomparable.
      */
    override def lessThanOrEqual(n1: N, n2: N): Boolean = ???
    
    /** Compares two elements of the lattice of node lists.
      * Returns true if l1 < l2 or l1 = l2, false if l1 > l2 or if they are incomparable.
      */
    override def lessThanOrEqualList(l1: L, l2: L): Boolean = ???
    // TODO: is this operation really needed (could be replaced with isBottom)?

    /** Create an element node with the given name, attributes and children.
      * The output is created bottom-up, so children are always created before their parent nodes.
      */
    override def createElement(name: String, attributes: L, children: L): N = ???
    
    /** Create an attribute node with the given name and text value.
      * Values that are not strings evaluate to BOTTOM.
      */
    override def createAttribute(name: String, value: V): N = ???

    /** Create a text node with the given text value.
      * Values that are not strings evaluate to BOTTOM.
      */
    override def createTextNode(value: V): N = ???

    /** Create an emtpy list containing no nodes */
    override def createEmptyList(): L = ???

    /** Create a list containing a single abstract node */
    override def createSingletonList(node: N): L = ???

    /** Get the root node of a given node */
    override def getRoot(node: N): N = ???
    // TODO: this might be implementable using getParent() and isRoot()

    /** Get the list of attributes of a given node.
      * Nodes that are not an element (and therefore don't have attributes) return an empty list, not BOTTOM! */
    override def getAttributes(node: N): L = ???

    /** Get the list of children of a given node.
      * Root nodes have a single child, element nodes have an arbitrary number of children.
      * Nodes that don't have children return an empty list, not BOTTOM! */
    override def getChildren(node: N): L = ???

    /** Get the parent of given node. If the node has no parent (root node), BOTTOM is returned. */
    override def getParent(node: N): N = {
      val (ZipperTree(desc, children), path) = node
      val (l, r) = (getLeftSiblingsFlat(Some(path)), getRightSiblingsFlat(Some(path)))
      val newChildren: ZListLattice[ZipperTree] = ZListLattice.concat(ZListLattice.concat(l, ZCons(ZipperTree(desc, children), ZNil), joinTree), r, joinTree)
      val parent = Some(getParentFlat(Some(path)))
      val newTree = ZipperTree(getDescriptorFlat(parent), newChildren)
      val newParent = getParentFlat(parent)
      (newTree, newParent)
    }

    /** Predicate function that checks whether a node has a specified node as its parent.
      * The first result is a node that is known to have that parent (this is BOTTOM if the node definitely
      * doesn't have that parent), the second result is a node that might not have that parent (this is
      * BOTTOM if the node definitely does have that parent). The two results are not necessarily disjoint.
      */
    override def hasParent(node: N, parent: N): (N, N) = ???

    /** Concatenates two lists. */
    override def concatLists(list1: L, list2: L): L = ???

    /** Partitions a node list in such a way that the first result contains all attribute nodes from the beginning of
      * the list (as soon as there are other node types in the list, attributes are ignored) and the second result
      * contains all other nodes.
      */
    override def partitionAttributes(list: L): (L, L) = ???

    /** Wraps a list of nodes in a document/root node. Lists that don't have exactly one element evaluate to BOTTOM. */
    override def wrapInRoot(list: L): N = ???

    /** Copies a list of nodes, so that they can be used in the output.
      * A root node is copied by copying its child (not wrapped in a root node). */
    override def copyToOutput(list: L): L = ???

    /** Evaluates a function for every element in the given list, providing also the index of each element in the list.
      * The resulting lists are flattened into a single list.
      */
    override def flatMapWithIndex(list: L, f: (N, V) => L): L = ???

    /** Gets the size of a node list */
    override def getNodeListSize(list: L): V = ???

    /** Gets the string-value of a node, as specified in the XSLT specification */
    override def getStringValue(node: N): V = ???

    /** Predicate function that checks whether a node is a root node.
      * The first result is a node that is known to be a root node (this is BOTTOM if the node definitely
      * is not a root node), the second result is a node that might not be a root node (this is
      * BOTTOM if the node definitely is a root node). The two results are not necessarily disjoint.
      */
    override def isRoot(node: N): (N, N) = ???

    /** Predicate function that checks whether a node is an element node.
      * The first result is a node that is known to be an element node (this is BOTTOM if the node definitely
      * is not an element node), the second result is a node that might not be an element node (this is
      * BOTTOM if the node definitely is an element node). The two results are not necessarily disjoint.
      */
    override def isElement(node: N): (N, N) = ???

    /** Predicate function that checks whether a node is a text node.
      * The first result is a node that is known to be a text node (this is BOTTOM if the node definitely
      * is not a text node), the second result is a node that might not be a text node (this is
      * BOTTOM if the node definitely is a text node). The two results are not necessarily disjoint.
      */
    override def isTextNode(node: N): (N, N) = ???

    /** Predicate function that checks whether a node is a comment node.
      * The first result is a node that is known to be a comment node (this is BOTTOM if the node definitely
      * is not a comment node), the second result is a node that might not be a comment node (this is
      * BOTTOM if the node definitely is a comment node). The two results are not necessarily disjoint.
      */
    override def isComment(node: N): (N, N) = ???

    /** Predicate function that checks whether a node is an attribute node.
      * The first result is a node that is known to be an attribute node (this is BOTTOM if the node definitely
      * is not an attribute node), the second result is a node that might not be an attribute node (this is
      * BOTTOM if the node definitely is an attribute node). The two results are not necessarily disjoint.
      */
    override def isAttribute(node: N): (N, N) = ???

    /** Predicate function that checks whether a node has a specified name.
      * The first result is a node that is known to have that name (this is BOTTOM if the node definitely
      * doesn't have that name), the second result is a node that might not have that name (this is
      * BOTTOM if the node definitely does have that name). The two results are not necessarily disjoint.
      * Nodes that don't have a name (any node except element and attribute nodes) are evaluated to BOTTOM.
      */
    override def hasName(node: N, name: String): (N, N) = ???

    /** Get the name for a given node. Nodes that don't have a name (i.e. are not an element or attribute node)
      * are evaluated to the empty string, not BOTTOM!
      */
    override def getNodeName(node: N): V = ???

    /** Concatenates the values of all text nodes in the list. List elements that are not text nodes are ignored. */
    override def getConcatenatedTextNodeValues(list: L): V = ???

    /** Filters a list using a given predicate function. The predicate function should never return a node
      * (as its first result) that is less precise than the input node.
      */
    override def filter(list: L, predicate: N => (N, N)): L = ???

    /** Gets the first node out of a node list.
      * Second return value is true if the list may be empty, false otherwise.
      */
    override def getFirst(list: L): (N, Boolean) = ???
  }
}