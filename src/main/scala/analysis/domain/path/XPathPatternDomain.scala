package analysis.domain.path

import analysis.domain.XMLDomain

object XPathPatternDomain {
  type N = Option[Set[XPathPattern]]
  type L = Option[Set[XPathPattern]] // TODO: add optional length of list
  type V = Unit

  implicit class Crossable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  /** This is the actual domain implementation */
  object D extends XMLDomain[N, L, V] {
    /** Get the TOP element for XML nodes. */
    override def top: N = None

    /** Gets the BOTTOM element for XML nodes. */
    override def bottom: N = BOT

    private val BOT: N = Some(Set())

    /** Get the TOP element for XML node lists.*/
    override def topList: L = ???

    /** Gets the BOTTOM element for XML node lists. */
    override def bottomList: L = ???

    /** Join two nodes. This calculates their supremum (least upper bound). */
    override def join(n1: N, n2: N): N = (n1, n2) match {
      case (None, _) => None
      case (_, None) => None
      case (BOT, _) => n2
      case (_, BOT) => n1
      case (Some(s1), Some(s2)) => Some(normalizeJoined(s1.union(s2)))
    }

    def normalizeJoined(set: Iterable[XPathPattern]): Set[XPathPattern] = set.toList.foldRight(List[XPathPattern]()) {
      case (next, acc) => if (acc.exists(e => lessThanOrEqualSingle(Some(next), Some(e))))
        acc
      else
        next :: acc.filter(e => !lessThanOrEqualSingle(Some(e), Some(next)))
    }.toSet

    override def meet(n1: N, n2: N): N = (n1, n2) match {
      case (None, _) => n2
      case (_, None) => n1
      case (Some(s1), Some(s2)) =>
        val filtered1 = s1.filter(pat1 => s2.exists(pat2 => lessThanOrEqualSingle(Some(pat1), Some(pat2))))
        val filtered2 = s2.filter(pat1 => s1.exists(pat2 => lessThanOrEqualSingle(Some(pat1), Some(pat2))))
        Some(filtered1.union(filtered2))
    }

    /** Join two node lists. This calculates their supremum (least upper bound). */
    override def joinList(l1: L, l2: L): L = ???

    /** Compares two elements of the lattice of nodes.
      * Returns true if n1 < n2 or n1 = n2, false if n1 > n2 or if they are incomparable.
      */
    override def lessThanOrEqual(n1: N, n2: N): Boolean = (n1, n2) match {
      case (_, None) => true
      case (None, Some(_)) => false
      case (Some(s1), Some(s2)) => s1.forall(pat1 => s2.exists(pat2 => lessThanOrEqualSingle(Some(pat1), Some(pat2))))
    }

    def lessThanOrEqualSingle(left: Option[XPathPattern], right: Option[XPathPattern]): Boolean = (left, right) match {
      case (_, None) => true
      case (None, Some(_)) => false
      case (Some(pat1), Some(pat2)) => (pat1, pat2) match {
        case (Root, Root) => true
        case (AnyElement(prev1), AnyElement(prev2)) => lessThanOrEqualSingle(prev1, prev2)
        case (NamedElement(name1, prev1), NamedElement(name2, prev2)) if name1 == name2 => lessThanOrEqualSingle(prev1, prev2)
        case (NamedElement(_, prev1), AnyElement(prev2)) => lessThanOrEqualSingle(prev1, prev2)
        case (AnyAttribute(prev1), AnyAttribute(prev2)) => lessThanOrEqualSingle(prev1, prev2)
        case (NamedAttribute(name1, prev1), NamedAttribute(name2, prev2)) if name1 == name2 => lessThanOrEqualSingle(prev1, prev2)
        case (NamedAttribute(_, prev1), AnyAttribute(prev2)) => lessThanOrEqualSingle(prev1, prev2)
        case (AnyTextNode(prev1), AnyTextNode(prev2)) => lessThanOrEqualSingle(prev1, prev2)
        case (AnyCommentNode(prev1), AnyCommentNode(prev2)) => lessThanOrEqualSingle(prev1, prev2)
        // TODO: add recursive cases for other node types
        case _ => false
      }
    }

    /** Compares two elements of the lattice of node lists.
      * Returns true if l1 < l2 or l1 = l2, false if l1 > l2 or if they are incomparable.
      */
    override def lessThanOrEqualList(l1: L, l2: L): Boolean = ???

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

    /** Get the list of attributes of a given node.
      * Nodes that are not an element (and therefore don't have attributes) return an empty list, not BOTTOM! */
    override def getAttributes(node: N): L = node match {
      case None => None
      case Some(s) => Some(normalizeJoined(s.collect {
        // NOTE: only element nodes have attributes
        case e: AnyElement => AnyAttribute(Some(e))
        case e: NamedElement => AnyAttribute(Some(e))
        // TODO: other node types should return empty lists, which are currently not representable as L
      }))
    }

    /** Get the list of children of a given node.
      * Root nodes have a single child, element nodes have an arbitrary number of children.
      * Nodes that don't have children return an empty list, not BOTTOM! */
    override def getChildren(node: N): L = node match {
      case None => None
      case Some(s) => Some(normalizeJoined(s.collect {
        case e@AnyElement(_) => List(AnyElement(Some(e)), AnyTextNode(Some(e)), AnyCommentNode(Some(e)))
        case e@NamedElement(_, _) => List(AnyElement(Some(e)), AnyTextNode(Some(e)), AnyCommentNode(Some(e)))
        case e@Root => List(AnyElement(Some(e)))
        // TODO: other node types (text, attribute) should return empty lists, which are currently not representable as L
      }.flatten))
    }

    /** Get the parent of given node. */
    override def getParent(node: N): N = node match {
      case None => Some(Set(AnyElement(None), Root))
      case Some(s) => Some(normalizeJoined(s.toList.filter(e => e != Root).map { e =>
        e.prev match {
          case None => e match {
            // NOTE: only elements can have Root as their parent
            case AnyElement(_) | NamedElement(_, _) => List[XPathPattern](AnyElement(None), Root)
            case _ => List[XPathPattern](AnyElement(None))
          }
          case Some(p) => List[XPathPattern](p)
        }
      }.flatten))
    }

    /** Predicate function that checks whether a node has a specified node as its parent.
      * The first result is a node that is known to have that parent (this is BOTTOM if the node definitely
      * doesn't have that parent), the second result is a node that might not have that parent (this is
      * BOTTOM if the node definitely does have that parent). The two results are not necessarily disjoint.
      */
    override def hasParent(node: N, parent: N): (N, N) = (node, parent) match {
      case (BOT, _) => (BOT, BOT)
      case (_, BOT) => (BOT, node) // parent is BOTTOM -> can't match
      case (None, _) => (None, None) // don't know anything about the node
      case (Some(_), None) => (node, node) // parent is TOP -> don't know anything
      case ((Some(nodes), Some(parents))) =>
        val result = nodes.cross(parents).map {
          case (n, p) =>
            meet(getParent(Some(Set(n))), Some(Set(p))).get.toList match {
              case Nil => Nil
              case List(e) => List(n.withPrev(e))
            }
        }.flatten.toSet
        (Some(result), node)
    }

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
    override def isRoot(node: N): (N, N) = {
      val rootSet: Set[XPathPattern] = Set(Root)
      node match
      {
        case None => (Some(rootSet), node)
        case Some(s) => (Some(s.intersect(rootSet)), Some(s.diff(rootSet)))
      }
    }

    /** Predicate function that checks whether a node is an element node.
      * The first result is a node that is known to be an element node (this is BOTTOM if the node definitely
      * is not an element node), the second result is a node that might not be an element node (this is
      * BOTTOM if the node definitely is an element node). The two results are not necessarily disjoint.
      */
    override def isElement(node: N): (N, N) = node match {
      case None => (Some(Set(AnyElement(None))), node)
      case Some(s) => (Some(s.collect {
        case e@NamedElement(_, _) => e
        case e@AnyElement(_) => e
      }), node)
    }

    /** Predicate function that checks whether a node is a text node.
      * The first result is a node that is known to be a text node (this is BOTTOM if the node definitely
      * is not a text node), the second result is a node that might not be a text node (this is
      * BOTTOM if the node definitely is a text node). The two results are not necessarily disjoint.
      */
    override def isTextNode(node: N): (N, N) = node match {
      case None => (Some(Set(AnyTextNode(None))), node)
      case Some(s) => (Some(s.collect {
        case e@AnyTextNode(_) => e
      }), node)
    }

    /** Predicate function that checks whether a node is a comment node.
      * The first result is a node that is known to be a comment node (this is BOTTOM if the node definitely
      * is not a comment node), the second result is a node that might not be a comment node (this is
      * BOTTOM if the node definitely is a comment node). The two results are not necessarily disjoint.
      */
    override def isComment(node: N): (N, N) = node match {
      case None => (Some(Set(AnyCommentNode(None))), node)
      case Some(s) => (Some(s.collect {
        case e@AnyCommentNode(_) => e
      }), node)
    }

    /** Predicate function that checks whether a node is an attribute node.
      * The first result is a node that is known to be an attribute node (this is BOTTOM if the node definitely
      * is not an attribute node), the second result is a node that might not be an attribute node (this is
      * BOTTOM if the node definitely is an attribute node). The two results are not necessarily disjoint.
      */
    override def isAttribute(node: N): (N, N) = node match {
      case None => (Some(Set(AnyAttribute(None))), node)
      case Some(s) => (Some(s.collect {
        case e@NamedAttribute(_, _) => e
        case e@AnyAttribute(_) => e
      }), node)
    }

    /** Predicate function that checks whether a node has a specified name.
      * The first result is a node that is known to have that name (this is BOTTOM if the node definitely
      * doesn't have that name), the second result is a node that might not have that name (this is
      * BOTTOM if the node definitely does have that name). The two results are not necessarily disjoint.
      * Nodes that don't have a name (any node except element and attribute nodes) are evaluated to BOTTOM.
      */
    override def hasName(node: N, name: String): (N, N) = node match {
      case None => (Some(Set(NamedElement(name, None), NamedAttribute(name, None))), node)
      case Some(s) => (Some(s.collect {
        case e@NamedElement(n, _) if name == n => e
        case AnyElement(p) => NamedElement(name, p)
        case a@NamedAttribute(n, _) if name == n => a
        case AnyAttribute(p) => NamedAttribute(name, p)
      }), node)
    }

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
  }
}