package evaluation

import analysis.domain.zipper.{ZipperXMLDomain, ZipperDomain}
import analysis.domain.XMLParser
import data.TestData
import org.scalatest.FunSuite

import scala.xml.Elem

class ZipperTransformSuite extends FunSuite {
  val xmlDom = ZipperDomain.xmlDom
  val xpathDom = ZipperDomain.xpathDom
  val parser = new XMLParser(ZipperDomain)

  def transform(xslt: Elem, source: ZipperXMLDomain.N = xmlDom.top) =
    TransformHelper.transformAbstract(xslt: Elem, source, ZipperDomain, true)

  test("Simple literal result element") {
    val xslt =
      <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
        <xsl:template match='/'>
          <result/>
        </xsl:template>
      </xsl:stylesheet>

    assertResult(parser.parseDocument(<result/>)) { transform(xslt) }
  }

  test("No templates") {
    val xslt =
      <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
      </xsl:stylesheet>

    // built-in templates are disabled, so this should evaluate to BOTTOM
    assertResult(xmlDom.bottom) { transform(xslt) }
  }

  test("No matching template (for /)") {
    val xslt =
      <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
        <xsl:template match="/foo"><bar/></xsl:template>
      </xsl:stylesheet>

    // no template matches the root node (/), because built-in templates are disabled, so this should evaluate to BOTTOM
    assertResult(xmlDom.bottom) { transform(xslt) }
  }

  // TODO: fix this test
  /*test("Wikipedia (XSLT #2 simplified)") {
    assertResult(???) { transform(TestData.WikipediaStylesheet2) }
  }*/

  test("Simple recursive template") {
    val xslt =
      <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
        <xsl:template match='/'>
          <result>
            <xsl:apply-templates/>
          </result>
        </xsl:template>
        <xsl:template match="/root">
          <child/>
        </xsl:template>
      </xsl:stylesheet>

    assertResult(parser.parseDocument(<result><child/></result>)) { transform(xslt) }
  }
}
