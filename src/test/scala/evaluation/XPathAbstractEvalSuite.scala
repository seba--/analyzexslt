package evaluation

import analysis.domain.powerset.{PowersetDomain, PowersetXPathDomain, PowersetXMLDomain}
import analysis.{AbstractXPathContext, XPathAnalyzer}
import xpath._
import xml.XMLNode

class XPathAbstractEvalSuite extends XPathEvalSuiteBase {
  def eval(expr: String, ctxNode: XMLNode): XPathValue = {
    // evaluate with an empty context
    val analyzer = new XPathAnalyzer(PowersetDomain)
    val result = analyzer.evaluate(XPathParser.parse(expr), AbstractXPathContext(analyzer.xmlDom.lift(ctxNode), None, None, Map())).get
    assertResult(1)(result.size)
    result.toList.head
  }
}
