package analysis.domain.zipper

import analysis.domain.{XPathDomain, XMLDomain, Lattice}

/** Just a wrapper for the type aliases and helper functions/objects. */
object ZipperXMLDomain {
  type S = Subtree // type of subtrees
  type P = Set[Path] // type of paths
  type N = (S, P) // a node is a subtree and a path
  type L = ZList[N] // type of node lists

  private val latP = Path.PathSetLattice // lattice for paths
  private val latD = NodeDescriptor.NodeDescriptorLattice // lattice for descriptors
  private val latS = Subtree.SubtreeLattice


  implicit object NodeLattice extends Lattice[N] {
    override def top = (latS.top, latP.top)
    override def bottom = (latS.bottom, latP.bottom)
    override def join(left: N, right: N): N = normalize(latS.join(left._1, right._1), latP.join(left._2, right._2))
    override def meet(left: N, right: N): N = normalize(latS.meet(left._1, right._1), latP.meet(left._2, right._2))

    override def lessThanOrEqual(left: N, right: N): Boolean =
      latS.lessThanOrEqual(left._1, right._1) && latP.lessThanOrEqual(left._2, right._2)
  }

  private def getDescriptorsFromPaths(path: P): Set[NodeDescriptor] = {
    def getSingle(desc: PathStepDescriptor): NodeDescriptor = desc match {
      case AnyElementStep => AnyElement
      case NamedElementStep(name) => Element(name)
      case AnyAttributeStep => AnyAttribute
      case NamedAttributeStep(name) => NamedAttribute(name)
      case AnyTextNodeStep => AnyText
      case AnyCommentNodeStep => AnyComment
    }

    latD.normalizeDescriptors(path.map {
      case RootPath(_) => Root
      case ChildStep(desc, _) => getSingle(desc)
      case DescendantStep(desc, _) => getSingle(desc)
    })
  }

  private def getPathsFromDescriptors(descriptors: Set[NodeDescriptor]): P = descriptors.flatMap[Path, Set[Path]] {
    case Root => latP.topRoot
    case Element(name) => latP.topRoot.map(DescendantStep(NamedElementStep(name), _))
    case AnyElement => latP.topRoot.map(DescendantStep(AnyElementStep, _))
    case Attribute(name, _) => latP.topRoot.map(DescendantStep(NamedAttributeStep(name), _))
    case NamedAttribute(name) => latP.topRoot.map(DescendantStep(NamedAttributeStep(name), _))
    case AnyAttribute => latP.topRoot.map(DescendantStep(AnyAttributeStep, _))
    case Text(_) | AnyText => latP.topRoot.map(DescendantStep(AnyTextNodeStep, _))
    case Comment(_) | AnyComment => latP.topRoot.map(DescendantStep(AnyCommentNodeStep, _))
  }

  /** Removes impossible elements (where Path and Subtree descriptors don't agree) */
  private def normalize(node: N): N = {
    val (Subtree(desc, attributes, children), path) = node
    if (children.isInstanceOf[ZBottom[Subtree]] || attributes.isInstanceOf[ZBottom[Set[NodeDescriptor]]]) {
      NodeLattice.bottom // return bottom if children or attributes are bottom ...
    } else {
      val meetDesc = latD.meet(getDescriptorsFromPaths(path), desc)
      if (meetDesc == Set()) { // BOTTOM
        NodeLattice.bottom // necessary to make children BOTTOM also (which would not happen in the case below)
      } else {
        val meetPath = latP.meet(getPathsFromDescriptors(desc), path)
        if (meetPath == Set()) {
          NodeLattice.bottom
        } else {
          (Subtree(meetDesc, attributes, children), meetPath)
        }
      }
    }
  }

  /** This is the actual (partial) domain implementation */
  trait D[V] extends XMLDomain[N, L, V] {
    val xpathDom: XPathDomain[V, N, L]

    protected def normalize(node: N): N = ZipperXMLDomain.normalize(node) // "re-export" the normalize function

    /** Get the TOP element for XML nodes. */
    override def top: N = NodeLattice.top

    /** Gets the BOTTOM element for XML nodes. */
    override def bottom: N = NodeLattice.bottom

    /** Get the TOP element for XML node lists. */
    override def topList: L = ZTop()

    /** Gets the BOTTOM element for XML node lists. */
    override def bottomList: L = ZBottom()

    /** Calcucate the join of two abstract nodes. This is the supremum (least upper bound). */
    override def join(n1: N, n2: N): N = NodeLattice.join(n1, n2)

    /** Calculate the meet of two abstract nodes. This is the infimum (greatest lower bound). */
    override def meet(n1: N, n2: N): N = NodeLattice.meet(n1, n2)

    /** Join two node lists. This calculates their supremum (least upper bound). */
    override def joinLists(l1: L, l2: L): L = l1 | l2

    /** Compares two elements of the lattice of nodes.
      * Returns true if n1 < n2 or n1 = n2, false if n1 > n2 or if they are incomparable.
      */
    override def lessThanOrEqual(n1: N, n2: N): Boolean = NodeLattice.lessThanOrEqual(n1, n2)

    /** Compares two elements of the lattice of node lists.
      * Returns true if l1 < l2 or l1 = l2, false if l1 > l2 or if they are incomparable.
      */
    override def lessThanOrEqualLists(l1: L, l2: L): Boolean = l1 <= l2

    /** Create an emtpy list containing no nodes */
    override def createEmptyList(): L = ZNil()

    /** Create a list containing a single abstract node */
    override def createSingletonList(node: N): L = ZCons(node, ZNil())

    /** Get the root node of a given node. */
    override def getRoot(node: N): N = {
      val (_, path) = node
      val (selfRoot, selfNotRoot) = isRoot(node) // find out if the node itself is a root node
      if (lessThanOrEqual(selfNotRoot, bottom)) { // if the input node is definitely a root node ...
        node // ... return it, because it is its own root
      } else {
        val mayBelongToFragment = !latP.lessThanOrEqual(latP.meet(path, latP.top(isFragment = true)), latP.bottom)
        val restRoot = if (mayBelongToFragment) {
          // we don't know anything about the children if it is a fragment (because there's no guarantee that the root has only a single child)
          normalize(Subtree(Set(Root), ZNil(), ZUnknownLength(Subtree(Set(AnyElement, AnyComment, AnyText), ZUnknownLength(Set(AnyAttribute)), ZTop()))), latP.topRoot)
        } else {
          // if the node does not belong to a fragment, we know that it only has one element child, which is determined by the path
          val childDescriptor = latD.meet(getDescriptorsFromPaths(latP.getRootChild(selfNotRoot._2)), Set(AnyElement))
          normalize(Subtree(Set(Root), ZNil(), ZCons(Subtree(childDescriptor, ZUnknownLength(Set(AnyAttribute)), ZTop()), ZNil())), Set(RootPath(isFragment = false)))
        }
        join(selfRoot, restRoot)
      }
    }

    /** Get the list of attributes of a given node.
      * Nodes that are not an element (and therefore don't have attributes) return an empty list, not BOTTOM!
      */
    override def getAttributes(node: N): L = {
      val (Subtree(_, attributes, _), path) = node
      val attributePath: Set[Path] = latP.getAttributes(path).joinInner
      attributes.map(a => normalize(Subtree(a, ZNil(), ZNil()), attributePath))
    }

    /** Get the list of children of a given node.
      * Root nodes have a single child, element nodes have an arbitrary number of children.
      * Nodes that don't have children return an empty list, not BOTTOM!
      */
    override def getChildren(node: N): L = {
      val (Subtree(_, _, children), path) = node
      val childrenPath: Set[Path] = latP.getChildren(path).joinInner
      children.map(tree => normalize(tree, childrenPath))
    }

    /** Get a list of all descendants of a node. */
    override def getDescendants(node: N): L = {
      val (Subtree(_, _, children), path) = node
      if (children == ZTop()) {
        val descendantPath: Set[Path] = latP.getDescendants(path).joinInner
        // don't know anything about the subtree, only the path
        ZUnknownLength(normalize((latS.top, descendantPath)))
      } else {
        val childrenPath: Set[Path] = latP.getChildren(path).joinInner
        val normalizedChildren = children.map(tree => normalize(tree, childrenPath))
        flatMapWithIndex(normalizedChildren, {
          case (n, _) => concatLists(createSingletonList(n), getDescendants(n))
        })
      }
    }

    /** Get the parent of given node. If the node has no parent (root node), BOTTOM is returned. */
    override def getParent(node: N): N = {
      val (_, path) = node
      val parent = latP.getParent(path)
      if (parent == Set(RootPath(isFragment = false))) { // parent must be (non-fragment) root node
        // root has exactly one child, which must be the node itself
        val newChildren: ZList[Subtree] = ZCons(node._1, ZNil())
        val newAttributes: ZList[Set[NodeDescriptor]] = ZNil() // root has no attributes
        normalize(Subtree(getDescriptorsFromPaths(parent), newAttributes, newChildren), parent)
      } else { // parent could be any element or root
        val newChildren: ZList[Subtree] = ZUnknownLength(isAttribute(top)._2._1) // don't know anything about siblings of `node`, except that they are not attributes
        val newAttributes: ZList[Set[NodeDescriptor]] = ZUnknownLength(Set(AnyAttribute)) // don't know anything about attributes of parent, except that they are attributes
        normalize(Subtree(getDescriptorsFromPaths(parent), newAttributes, newChildren), parent)
      }
    }

    /** Predicate function that checks whether a node is in a given list of nodes.
      * The first result is a node that is known to be in that list (this is BOTTOM if the node definitely
      * is not in the list), the second result is a node that might not be in the list (this is
      * BOTTOM if the node definitely is contained in the list). The two results are not necessarily disjoint.
      */
    override def isContainedIn(node: N, list: L): (N, N) = (list.contains(node), node)

    /** Concatenates two lists. */
    override def concatLists(list1: L, list2: L): L = list1 ++ list2

    /** Creates a root node with the given children. The second parameter specifies whether the root represents a
      * (result tree) fragment or a complete document (the latter can only have a single element child).
      */
    def createRoot(children: L, isResultTreeFragment: Boolean): N = {
      val verifiedChildren = if (isResultTreeFragment) {
        children.map(_._1) // just extract the Subtree part of all children (their paths are ignored)
      } else {
        // if the root node is not a fragment, it can only have a single element child
        children.map(_._1) & ZList(Subtree(Set(AnyElement), ZTop(), ZTop()))
      }
      normalize(Subtree(Set(Root), ZNil(), verifiedChildren), Set(RootPath(isResultTreeFragment)))
    }

    /** Copies a list of nodes, so that they can be used in the output.
      * A root node is copied by copying its child (not wrapped in a root node).
      */
    override def copyToOutput(list: L): L = flatMapWithIndex(list, (n, _) => {
      val (root, notRoot) = isRoot(n)
      if (!lessThanOrEqual(root, bottom)) { // root is not BOTTOM -> node might be a root node -> copy children
        val (_, path) = n
        val isFragmentRoot = !latP.lessThanOrEqual(latP.meet(path, Set(RootPath(isFragment = true))), latP.bottom)
        if (!isFragmentRoot) {
          // it's not a fragment, so we can restrict the result to a single element child
          ZList(normalize(notRoot._1, latP.top)) | ZList(getChildren(root).first)
        } else {
          ZList(normalize(notRoot._1, latP.top)) | getChildren(root)
        }
      } else {
        ZList(normalize(n._1, latP.top))
      }
    })

    /** Evaluates a function for every element in the given list, providing also the index of each element in the list.
      * The resulting lists are flattened into a single list by concatenation.
      */
    override def flatMapWithIndex(list: L, f: (N, V) => L): L = {
      def flatMapWithIndexInternal(list: L, currentIndex: V, f: (N, V) => L): L = list match {
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
        val Subtree(desc, _, children) = tree
        xpathDom.joinAll(desc.map {
          case Root => getStringValueFromSubtree(children.first)
          case Element(name) => concatStringValuesFromZList(children)
          case Attribute(name, value) => xpathDom.liftString(value)
          case Text(value) => xpathDom.liftString(value)
          case Comment(value) => xpathDom.liftString(value)
          case AnyElement | AnyAttribute | NamedAttribute(_) | AnyText | AnyComment => xpathDom.topString
        })
      }

      /** Concatenates the string values of all nodes (subtrees) in a ZList */
      def concatStringValuesFromZList(list: ZList[Subtree]): V = list match {
        case ZBottom() => xpathDom.bottom
        case ZTop() | ZUnknownLength(_) => xpathDom.topString
        case ZNil() => xpathDom.liftString("")
        case ZCons(first, rest) =>
          xpathDom.concatStrings(getStringValueFromSubtree(first), concatStringValuesFromZList(rest))
        case ZMaybeNil(first, rest) =>
          xpathDom.join(xpathDom.liftString(""), xpathDom.concatStrings(getStringValueFromSubtree(first), concatStringValuesFromZList(rest)))
      }
      getStringValueFromSubtree(node._1)
    }

    /** Predicate function that checks whether a node is a root node.
      * The first result is a node that is known to be a root node (this is BOTTOM if the node definitely
      * is not a root node), the second result is a node that might not be a root node (this is
      * BOTTOM if the node definitely is a root node). The two results are not necessarily disjoint.
      */
    override def isRoot(node: N, allowResultTreeFragments: Boolean): (N, N) = {
      val (Subtree(desc, attributes, children), path) = node
      val isFragmentRoot = !latP.lessThanOrEqual(latP.meet(path, Set(RootPath(isFragment = true))), latP.bottom)
      val newChildren: ZList[S] = if (!allowResultTreeFragments || !isFragmentRoot) {
        // we know that this node is not the root of a fragment (it's either not a root at all or the root of a non-fragment document)
        val firstChild = children.first // so wa can restrict the result to a single element child
        if (latS.lessThanOrEqual(firstChild, latS.bottom)) // if there was no first child ...
          ZBottom() // return bottom
        else
          ZCons(firstChild, ZNil()) // otherwise create a singleton list from that child
      } else {
        children
      }
      val (newPathPos, newPathNeg): (P, P) = if (!allowResultTreeFragments || !isFragmentRoot) {
        (latP.meet(path, Set(RootPath(isFragment = false))), path.diff(Set(RootPath(isFragment = false))))
      } else {
        (latP.meet(path, latP.topRoot), path.diff(latP.topRoot))
      }
      // NOTE: root node can't have attributes, so we set it to ZNil
      val positiveResult: N = normalize(Subtree(latD.meet(desc, Set(Root)), ZNil(), newChildren), newPathPos)
      val negativeResult: N = normalize(Subtree(desc.diff(Set(Root)), attributes, children), newPathNeg)
      (positiveResult, negativeResult)
    }

    /** Predicate function that checks whether a node is an element node.
      * The first result is a node that is known to be an element node (this is BOTTOM if the node definitely
      * is not an element node), the second result is a node that might not be an element node (this is
      * BOTTOM if the node definitely is an element node). The two results are not necessarily disjoint.
      */
    override def isElement(node: N): (N, N) = {
      val (Subtree(desc, attributes, children), path) = node
      val (pathYes, pathNo) = latP.isElement(path)
      val (descYes, descNo) = desc.partition {
        case Element(_) => true
        case AnyElement => true
        case _ => false
      }
      val yes = normalize(Subtree(latD.normalizeDescriptors(descYes), attributes, children), pathYes)
      // NOTE: only elements can have attributes, therefore we use the empty attribute set in the negative result
      val no = normalize(Subtree(latD.normalizeDescriptors(descNo), ZNil(), children), pathNo)
      (yes, no)
    }

    /** Predicate function that checks whether a node is a text node.
      * The first result is a node that is known to be a text node (this is BOTTOM if the node definitely
      * is not a text node), the second result is a node that might not be a text node (this is
      * BOTTOM if the node definitely is a text node). The two results are not necessarily disjoint.
      */
    override def isTextNode(node: N): (N, N) = {
      val (Subtree(desc, attributes, children), path) = node
      val (pathYes, pathNo) = latP.isTextNode(path)
      val (descYes, descNo) = desc.partition {
        case Text(_) => true
        case AnyText => true
        case _ => false
      }
      // NOTE: text nodes cannot have attributes or children, therefore we use the empty attribute set in the positive result
      val yes = normalize(Subtree(latD.normalizeDescriptors(descYes), ZNil(), ZNil()), pathYes)
      val no = normalize(Subtree(latD.normalizeDescriptors(descNo), attributes, children), pathNo)
      (yes, no)
    }

    /** Predicate function that checks whether a node is a comment node.
      * The first result is a node that is known to be a comment node (this is BOTTOM if the node definitely
      * is not a comment node), the second result is a node that might not be a comment node (this is
      * BOTTOM if the node definitely is a comment node). The two results are not necessarily disjoint.
      */
    override def isComment(node: N): (N, N) = {
      val (Subtree(desc, attributes, children), path) = node
      val (pathYes, pathNo) = latP.isComment(path)
      val (descYes, descNo) = desc.partition {
        case Comment(_) => true
        case AnyComment => true
        case _ => false
      }
      // NOTE: comment nodes cannot have attributes or children, therefore we use the empty attribute set in the positive result
      val yes = normalize(Subtree(latD.normalizeDescriptors(descYes), ZNil(), ZNil()), pathYes)
      val no = normalize(Subtree(latD.normalizeDescriptors(descNo), attributes, children), pathNo)
      (yes, no)
    }

    /** Predicate function that checks whether a node is an attribute node.
      * The first result is a node that is known to be an attribute node (this is BOTTOM if the node definitely
      * is not an attribute node), the second result is a node that might not be an attribute node (this is
      * BOTTOM if the node definitely is an attribute node). The two results are not necessarily disjoint.
      */
    override def isAttribute(node: N): (N, N) = {
      val (Subtree(desc, attributes, children), path) = node
      val (pathYes, pathNo) = latP.isAttribute(path)
      val (descYes, descNo) = desc.partition {
        case Attribute(_, _) => true
        case NamedAttribute(_) => true
        case AnyAttribute => true
        case _ => false
      }
      // NOTE: attribute nodes cannot have attributes or children, therefore we use the empty attribute set in the positive result
      val yes = normalize(Subtree(latD.normalizeDescriptors(descYes), ZNil(), ZNil()), pathYes)
      val no = normalize(Subtree(latD.normalizeDescriptors(descNo), attributes, children), pathNo)
      (yes, no)
    }

    /** Predicate function that checks whether a node has a specified name.
      * The first result is a node that is known to have that name (this is BOTTOM if the node definitely
      * doesn't have that name), the second result is a node that might not have that name (this is
      * BOTTOM if the node definitely does have that name). The two results are not necessarily disjoint.
      * Nodes that don't have a name (any node except element and attribute nodes) are evaluated to BOTTOM.
      */
    override def hasName(node: N, name: String): (N, N) = {
      val (Subtree(desc, attributes, children), path) = node
      val (pathYes, pathNo) = latP.hasName(path, name)
      val descYes: Set[NodeDescriptor] = desc.collect {
        case e@Element(n) if name == n => e
        case AnyElement => Element(name)
        case a@Attribute(n, _) if name == n => a
        case a@NamedAttribute(n) if name == n => a
        case AnyAttribute => NamedAttribute(name)
      }

      val descNo: Set[NodeDescriptor] = desc.collect {
        case e@Element(n) if name != n => e
        case AnyElement => AnyElement // can't represent element that does *not* have specified name
        case a@Attribute(n, _) if name != n => a
        case a@NamedAttribute(n) if name != n => a
        case AnyAttribute => AnyAttribute // can't represent attribute that does *not* have specified name
      }

      val yes = normalize(Subtree(latD.normalizeDescriptors(descYes), attributes, children), pathYes)
      val no = normalize(Subtree(latD.normalizeDescriptors(descNo), attributes, children), pathNo)

      (yes, no)
    }

    /** Get the name for a given node. Nodes that don't have a name (i.e. are not an element or attribute node)
      * are evaluated to the empty string, not BOTTOM!
      */
    override def getNodeName(node: N): V = {
      val (Subtree(desc, _, _), _) = node
      xpathDom.joinAll(desc.map {
        case Element(name) => xpathDom.liftString(name)
        case AnyElement => xpathDom.topString
        case Attribute(name, _) => xpathDom.liftString(name)
        case NamedAttribute(name) => xpathDom.liftString(name)
        case AnyAttribute => xpathDom.topString
        case _ => xpathDom.liftString("")
      })
    }

    /** Concatenates the values of all text nodes in the list. List elements that are not text nodes are ignored. */
    override def getConcatenatedTextNodeValues(list: L): V = {
      /** Helper function to get the string value of a single node.
        * Nodes that are not text nodes are ignored (evaluate to empty string). */
      def getTextNodeValue(node: N): V = {
        if (lessThanOrEqual(node, bottom)) {
          xpathDom.bottom
        } else {
          val (text, notText) = isTextNode(node)
          val result = getStringValue(text)
          if (!lessThanOrEqual(notText, bottom)) {
            // there is something that is not a text node -> add empty string to result
            xpathDom.join(result, xpathDom.liftString(""))
          } else {
            result
          }
        }
      }

      list match {
        case ZBottom() => xpathDom.bottom
        case ZTop() => xpathDom.topString
        case ZUnknownLength(_) =>
          xpathDom.topString // text content could be repeated an unknown number of time, so we can only return TOP
        case ZCons(first, rest) =>
          xpathDom.concatStrings(getTextNodeValue(first), getConcatenatedTextNodeValues(rest))
        case ZMaybeNil(first, rest) =>
          xpathDom.join(xpathDom.liftString(""), xpathDom.concatStrings(getTextNodeValue(first), getConcatenatedTextNodeValues(rest)))
        case ZNil() => xpathDom.liftString("")
      }
    }

    /** Filters a list using a given predicate function. The predicate function should never return a node
      * (as its first result) that is less precise than the input node.
      */
    override def filter(list: L, predicate: N => (N, N)): L = list.filter(n => predicate(n)._1)

    /** Takes the longest prefix of a list where all elements fulfill a given predicate function.
      * The predicate function should never return a node (as its first result) that is less precise than the input node.
      */
    override def takeWhile(list: L, predicate: N => (N, N)): L = list.takeWhile(n => predicate(n)._1)

    /** Gets the first node out of a node list. BOTTOM is returned if the list is empty or BOTTOM. */
    override def getFirst(list: L): N = list.first
  }
}
