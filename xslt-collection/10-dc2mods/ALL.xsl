﻿<?xml version="1.0" encoding="UTF-8"?>
	<!--
	    Version 1.0 created 2006-11-01 by Clay Redding <cred@loc.gov>
	    
	    This stylesheet will transform simple Dublin Core (DC) expressed in either OAI DC [1] or SRU DC [2] schemata to MODS 
	    version 3.2.
	    
	    Reasonable attempts have been made to automatically detect and process the textual content of DC elements for the purposes 
	    of outputting to MODS.  Because MODS is more granular and expressive than simple DC, transforming a given DC element to the 
            proper MODS element(s) is difficult and may result in imprecise or incorrect tagging.  Undoubtedly local customizations will 
            have to be made by those who utilize this stylesheet in order to achieve deisred results.  No attempt has been made to 
            ignore empty DC elements.  If your DC contains empty elements, they should either be removed, or local customization to 
            detect the existence of text for each element will have to be added to this stylesheet.
	    
	    MODS also often encourages content adhering to various data value standards.  The contents of some of the more widely used value 
	    standards, such as IANA MIME types, ISO 3166-1, ISO 639-2, etc., have been added into the stylesheet to facilitate proper 
	    mapping of simple DC to the proper MODS elements.  A crude attempt at detecting the contents of DC identifiers and outputting them
	    to the proper MODS elements has been made as well.  Common persistent identifier schemes, standard numbers, etc., have been included.
	    To truly detect these efficiently, XSL/XPath 2.0 or XQuery may be needed in order to utilize regular expressions.
	    
	    [1] http://www.openarchives.org/OAI/openarchivesprotocol.html#MetadataNamespaces
	    [2] http://www.loc.gov/standards/sru/record-schemas.html
	        
	-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:sru_dc="info:srw/schema/1/dc-schema" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" exclude-result-prefixes="sru_dc oai_dc dc" version="1.0">
    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    <!--<xsl:include href="inc/dcmiType.xsl"/>-->
    <xsl:variable name="types">
        <!-- DCMI Types (normalized versions appear in lower case and with spaces) http://dublincore.org/documents/dcmi-type-vocabulary/ -->
        <xsl:text>Collection</xsl:text>
        <xsl:text>collection</xsl:text>
        <xsl:text>Dataset</xsl:text>
        <xsl:text>dataset</xsl:text>
        <xsl:text>Event</xsl:text>
        <xsl:text>event</xsl:text>
        <xsl:text>Image</xsl:text>
        <xsl:text>image</xsl:text>
        <xsl:text>InteractiveResource</xsl:text>
        <xsl:text>interactiveresource</xsl:text>
        <xsl:text>Interactive Resource</xsl:text>
        <xsl:text>interactive resource</xsl:text>
        <xsl:text>MovingImage</xsl:text>
        <xsl:text>movingimage</xsl:text>
        <xsl:text>Moving Image</xsl:text>
        <xsl:text>moving image</xsl:text>
        <xsl:text>PhysicalObject</xsl:text>
        <xsl:text>physicalobject</xsl:text>
        <xsl:text>Physical Object</xsl:text>
        <xsl:text>physical object</xsl:text>
        <xsl:text>Service</xsl:text>
        <xsl:text>service</xsl:text>
        <xsl:text>Software</xsl:text>
        <xsl:text>software</xsl:text>
        <xsl:text>Sound</xsl:text>
        <xsl:text>sound</xsl:text>
        <xsl:text>StillImage</xsl:text>
        <xsl:text>stillimage</xsl:text>
        <xsl:text>Still Image</xsl:text>
        <xsl:text>stillimage</xsl:text>
        <xsl:text>Text</xsl:text>
        <xsl:text>text</xsl:text>
    </xsl:variable>
    <!--<xsl:include href="inc/mimeType.xsl"/>-->
    <xsl:variable name="mimeTypeDirectories">
        <xsl:text>application</xsl:text>
        <xsl:text>audio</xsl:text>
        <xsl:text>example</xsl:text>
        <xsl:text>image</xsl:text>
        <xsl:text>message</xsl:text>
        <xsl:text>model</xsl:text>
        <xsl:text>multipart</xsl:text>
        <xsl:text>text</xsl:text>
        <xsl:text>video</xsl:text>
    </xsl:variable>
    <!--<xsl:include href="inc/csdgm.xsl"/>-->
    <xsl:variable name="projections">
        <!-- begin USGS values http://erg.usgs.gov/isb/pubs/MapProjections/projections.html -->
        <xsl:text>Globe</xsl:text>
        <xsl:text>Mercator</xsl:text>
        <xsl:text>Transverse Mercator</xsl:text>
        <xsl:text>Oblique Mercator</xsl:text>
        <xsl:text>Space Oblique Mercator</xsl:text>
        <xsl:text>Miller Cylindrical</xsl:text>
        <xsl:text>Robinson</xsl:text>
        <xsl:text>Sinusoidal Equal</xsl:text>
        <xsl:text>Area</xsl:text>
        <xsl:text>Orthographic</xsl:text>
        <xsl:text>Stereographic</xsl:text>
        <xsl:text>Gnomonic</xsl:text>
        <xsl:text>Azimuthal Equalidistant</xsl:text>
        <xsl:text>Lambert Azimuthal Equal Area</xsl:text>
        <xsl:text>Albers Equal Area Conic</xsl:text>
        <xsl:text>Lambert Conformal Conic</xsl:text>
        <xsl:text>Equidistant Conic</xsl:text>
        <xsl:text>Polyonic</xsl:text>
        <xsl:text>Biplolar Oblique Conic Conformal</xsl:text>
        <!-- begin CSDGM values http://fgdc.er.usgs.gov/metadata/csdgm/04.html .  Normalized values have removed the underscore character -->
        <xsl:text>Albers_Conical_Equal_Area</xsl:text>
        <xsl:text>Albers Conical Equal Area</xsl:text>
        <xsl:text>Azimuthal_Equidistant</xsl:text>
        <xsl:text>Azimuthal Equidistant</xsl:text>
        <xsl:text>Equidistant_Conic</xsl:text>
        <xsl:text>Equidistant Conic</xsl:text>
        <xsl:text>Equirectangular</xsl:text>
        <xsl:text>General_Vertical_Near-sided_Perspective</xsl:text>
        <xsl:text>General Vertical Near-sided Perspective</xsl:text>
        <xsl:text>Gnomonic</xsl:text>
        <xsl:text>Lambert_Azimuthal_Equal_Area </xsl:text>
        <xsl:text>Lambert Azimuthal Equal Area </xsl:text>
        <xsl:text>Lambert_Conformal_Conic</xsl:text>
        <xsl:text>Lambert Conformal Conic</xsl:text>
        <xsl:text>Mercator</xsl:text>
        <xsl:text>Modified_Stereographic_for_Alaska</xsl:text>
        <xsl:text>Modified Stereographic for Alaska</xsl:text>
        <xsl:text>Miller_Cylindrical</xsl:text>
        <xsl:text>Miller Cylindrical</xsl:text>
        <xsl:text>Oblique_Mercator</xsl:text>
        <xsl:text>Oblique Mercator</xsl:text>
        <xsl:text>Orthographic</xsl:text>
        <xsl:text>Polar_Stereographic</xsl:text>
        <xsl:text>Polar Stereographic</xsl:text>
        <xsl:text>Polyconic</xsl:text>
        <xsl:text>Robinson</xsl:text>
        <xsl:text>Sinusoidal</xsl:text>
        <xsl:text>Space_Oblique_Mercator_(Landsat)</xsl:text>
        <xsl:text>Space Oblique Mercator (Landsat)</xsl:text>
        <xsl:text>Stereographic</xsl:text>
        <xsl:text>Transverse_Mercator</xsl:text>
        <xsl:text>Transverse Mercator</xsl:text>
        <xsl:text>van_der_Grinten</xsl:text>
        <xsl:text>van der Grinten</xsl:text>
        <xsl:text>Map_Projection_Parameters</xsl:text>
        <xsl:text>Map Projection Parameters</xsl:text>
    </xsl:variable>
    <!--<xsl:include href="inc/forms.xsl"/>-->
    <xsl:variable name="forms">
        <!-- from MARC Value List for Form http://www.loc.gov/marc/sourcecode/form/formlist.html -->
        <xsl:text>braille</xsl:text>
        <xsl:text>electronic</xsl:text>
        <xsl:text>microfiche</xsl:text>
        <xsl:text>microfilm</xsl:text>
        <xsl:text>print</xsl:text>
        <xsl:text>large print</xsl:text>
        <!-- from Special material designation for form http://www.loc.gov/marc/sourcecode/form/smdlist.html -->
        <xsl:text>chip cartridge</xsl:text>
        <xsl:text>computer optical disc cartridge</xsl:text>
        <xsl:text>magnetic disk</xsl:text>
        <xsl:text>magneto-optical disc</xsl:text>
        <xsl:text>optical disc</xsl:text>
        <xsl:text>remote</xsl:text>
        <xsl:text>tape cartridge</xsl:text>
        <xsl:text>tape cassette</xsl:text>
        <xsl:text>tape reel</xsl:text>
        <xsl:text>celestial globe</xsl:text>
        <xsl:text>earth moon globe</xsl:text>
        <xsl:text>planetary or lunar globe</xsl:text>
        <xsl:text>terrestrial globe</xsl:text>
        <xsl:text>atlas</xsl:text>
        <xsl:text>diagram</xsl:text>
        <xsl:text>map</xsl:text>
        <xsl:text>model</xsl:text>
        <xsl:text>profile</xsl:text>
        <xsl:text>remote-sensing image</xsl:text>
        <xsl:text>section</xsl:text>
        <xsl:text>view</xsl:text>
        <xsl:text>aperture card</xsl:text>
        <xsl:text>microfiche</xsl:text>
        <xsl:text>microfiche cassette</xsl:text>
        <xsl:text>microfilm cartridge</xsl:text>
        <xsl:text>microfilm cassette</xsl:text>
        <xsl:text>microfilm reel</xsl:text>
        <xsl:text>microopaque</xsl:text>
        <xsl:text>film cartridge</xsl:text>
        <xsl:text>film cassette</xsl:text>
        <xsl:text>film reel</xsl:text>
        <xsl:text>chart</xsl:text>
        <xsl:text>collage</xsl:text>
        <xsl:text>drawing</xsl:text>
        <xsl:text>flash card</xsl:text>
        <xsl:text>painting</xsl:text>
        <xsl:text>photomechanical print</xsl:text>
        <xsl:text>photonegative</xsl:text>
        <xsl:text>photoprint</xsl:text>
        <xsl:text>picture</xsl:text>
        <xsl:text>print</xsl:text>
        <xsl:text>technical drawing</xsl:text>       
        <xsl:text>filmslip</xsl:text>
        <xsl:text>filmstrip cartridge</xsl:text>
        <xsl:text>filmstrip roll</xsl:text>
        <xsl:text>other filmstrip type</xsl:text>
        <xsl:text>slide</xsl:text>
        <xsl:text>transparency </xsl:text>       
        <xsl:text>cylinder</xsl:text>
        <xsl:text>roll</xsl:text>
        <xsl:text>sound cartridge</xsl:text>
        <xsl:text>sound cassette</xsl:text>
        <xsl:text>sound disc</xsl:text>
        <xsl:text>sound-tape reel</xsl:text>
        <xsl:text>sound-track film</xsl:text>
        <xsl:text>wire recording </xsl:text>      
        <xsl:text>braille</xsl:text>
        <xsl:text>combination</xsl:text>
        <xsl:text>moon</xsl:text>
        <xsl:text>tactile, with no writing system </xsl:text>       
        <xsl:text>braille</xsl:text>
        <xsl:text>large print</xsl:text>
        <xsl:text>regular print</xsl:text>
        <xsl:text>text in looseleaf binder</xsl:text>
        <xsl:text>videocartridge</xsl:text>
        <xsl:text>videocassette</xsl:text>
        <xsl:text>videodisc</xsl:text>
        <xsl:text>videoreel</xsl:text>
    </xsl:variable>
    <!--<xsl:include href="inc/iso3166-1.xsl"/>-->
    <xsl:variable name="iso3166-1">
        <!-- valid ISO 3166-1 2-letter country codes -->
        <xsl:text>AF</xsl:text>
        <xsl:text>AL</xsl:text>
        <xsl:text>DZ</xsl:text>
        <xsl:text>AS</xsl:text>
        <xsl:text>AD</xsl:text>
        <xsl:text>AO</xsl:text>
        <xsl:text>AI</xsl:text>
        <xsl:text>AQ</xsl:text>
        <xsl:text>AG</xsl:text>
        <xsl:text>AR</xsl:text>
        <xsl:text>AM</xsl:text>
        <xsl:text>AW</xsl:text>
        <xsl:text>AU</xsl:text>
        <xsl:text>AT</xsl:text>
        <xsl:text>AZ</xsl:text>
        <xsl:text>BS</xsl:text>
        <xsl:text>BH</xsl:text>
        <xsl:text>BD</xsl:text>
        <xsl:text>BB</xsl:text>
        <xsl:text>BY</xsl:text>
        <xsl:text>BE</xsl:text>
        <xsl:text>BZ</xsl:text>
        <xsl:text>BJ</xsl:text>
        <xsl:text>BM</xsl:text>
        <xsl:text>BT</xsl:text>
        <xsl:text>BO</xsl:text>
        <xsl:text>BA</xsl:text>
        <xsl:text>BW</xsl:text>
        <xsl:text>BV</xsl:text>
        <xsl:text>BR</xsl:text>
        <xsl:text>IO</xsl:text>
        <xsl:text>VG</xsl:text>
        <xsl:text>BN</xsl:text>
        <xsl:text>BG</xsl:text>
        <xsl:text>BF</xsl:text>
        <xsl:text>BI</xsl:text>
        <xsl:text>KH</xsl:text>
        <xsl:text>CM</xsl:text>
        <xsl:text>CA</xsl:text>
        <xsl:text>CV</xsl:text>
        <xsl:text>KY</xsl:text>
        <xsl:text>CF</xsl:text>
        <xsl:text>TD</xsl:text>
        <xsl:text>CL</xsl:text>
        <xsl:text>CN</xsl:text>
        <xsl:text>CX</xsl:text>
        <xsl:text>CC</xsl:text>
        <xsl:text>CO</xsl:text>
        <xsl:text>KM</xsl:text>
        <xsl:text>CD</xsl:text>
        <xsl:text>CG</xsl:text>
        <xsl:text>CK</xsl:text>
        <xsl:text>CR</xsl:text>
        <xsl:text>CI</xsl:text>
        <xsl:text>CU</xsl:text>
        <xsl:text>CY</xsl:text>
        <xsl:text>CZ</xsl:text>
        <xsl:text>DK</xsl:text>
        <xsl:text>DJ</xsl:text>
        <xsl:text>DM</xsl:text>
        <xsl:text>DO</xsl:text>
        <xsl:text>EC</xsl:text>
        <xsl:text>EG</xsl:text>
        <xsl:text>SV</xsl:text>
        <xsl:text>GQ</xsl:text>
        <xsl:text>ER</xsl:text>
        <xsl:text>EE</xsl:text>
        <xsl:text>ET</xsl:text>
        <xsl:text>FO</xsl:text>
        <xsl:text>FK</xsl:text>
        <xsl:text>FJ</xsl:text>
        <xsl:text>FI</xsl:text>
        <xsl:text>FR</xsl:text>
        <xsl:text>GF</xsl:text>
        <xsl:text>PF</xsl:text>
        <xsl:text>TF</xsl:text>
        <xsl:text>GA</xsl:text>
        <xsl:text>GM</xsl:text>
        <xsl:text>GE</xsl:text>
        <xsl:text>DE</xsl:text>
        <xsl:text>GH</xsl:text>
        <xsl:text>GI</xsl:text>
        <xsl:text>GR</xsl:text>
        <xsl:text>GL</xsl:text>
        <xsl:text>GD</xsl:text>
        <xsl:text>GP</xsl:text>
        <xsl:text>GU</xsl:text>
        <xsl:text>GT</xsl:text>
        <xsl:text>GN</xsl:text>
        <xsl:text>GW</xsl:text>
        <xsl:text>GY</xsl:text>
        <xsl:text>HT</xsl:text>
        <xsl:text>HM</xsl:text>
        <xsl:text>VA</xsl:text>
        <xsl:text>HN</xsl:text>
        <xsl:text>HK</xsl:text>
        <xsl:text>HR</xsl:text>
        <xsl:text>HU</xsl:text>
        <xsl:text>IS</xsl:text>
        <xsl:text>IN</xsl:text>
        <xsl:text>ID</xsl:text>
        <xsl:text>IR</xsl:text>
        <xsl:text>IQ</xsl:text>
        <xsl:text>IE</xsl:text>
        <xsl:text>IL</xsl:text>
        <xsl:text>IT</xsl:text>
        <xsl:text>JM</xsl:text>
        <xsl:text>JP</xsl:text>
        <xsl:text>JO</xsl:text>
        <xsl:text>KZ</xsl:text>
        <xsl:text>KE</xsl:text>
        <xsl:text>KI</xsl:text>
        <xsl:text>KP</xsl:text>
        <xsl:text>KR</xsl:text>
        <xsl:text>KW</xsl:text>
        <xsl:text>KG</xsl:text>
        <xsl:text>LA</xsl:text>
        <xsl:text>LV</xsl:text>
        <xsl:text>LB</xsl:text>
        <xsl:text>LS</xsl:text>
        <xsl:text>LR</xsl:text>
        <xsl:text>LY</xsl:text>
        <xsl:text>LI</xsl:text>
        <xsl:text>LT</xsl:text>
        <xsl:text>LU</xsl:text>
        <xsl:text>MO</xsl:text>
        <xsl:text>MK</xsl:text>
        <xsl:text>MG</xsl:text>
        <xsl:text>MW</xsl:text>
        <xsl:text>MY</xsl:text>
        <xsl:text>MV</xsl:text>
        <xsl:text>ML</xsl:text>
        <xsl:text>MT</xsl:text>
        <xsl:text>MH</xsl:text>
        <xsl:text>MQ</xsl:text>
        <xsl:text>MR</xsl:text>
        <xsl:text>MU</xsl:text>
        <xsl:text>YT</xsl:text>
        <xsl:text>MX</xsl:text>
        <xsl:text>FM</xsl:text>
        <xsl:text>MD</xsl:text>
        <xsl:text>MC</xsl:text>
        <xsl:text>MN</xsl:text>
        <xsl:text>MS</xsl:text>
        <xsl:text>MA</xsl:text>
        <xsl:text>MZ</xsl:text>
        <xsl:text>MM</xsl:text>
        <xsl:text>NA</xsl:text>
        <xsl:text>NR</xsl:text>
        <xsl:text>NP</xsl:text>
        <xsl:text>AN</xsl:text>
        <xsl:text>NL</xsl:text>
        <xsl:text>NC</xsl:text>
        <xsl:text>NZ</xsl:text>
        <xsl:text>NI</xsl:text>
        <xsl:text>NE</xsl:text>
        <xsl:text>NG</xsl:text>
        <xsl:text>NU</xsl:text>
        <xsl:text>NF</xsl:text>
        <xsl:text>MP</xsl:text>
        <xsl:text>NO</xsl:text>
        <xsl:text>OM</xsl:text>
        <xsl:text>PK</xsl:text>
        <xsl:text>PW</xsl:text>
        <xsl:text>PS</xsl:text>
        <xsl:text>PA</xsl:text>
        <xsl:text>PG</xsl:text>
        <xsl:text>PY</xsl:text>
        <xsl:text>PE</xsl:text>
        <xsl:text>PH</xsl:text>
        <xsl:text>PN</xsl:text>
        <xsl:text>PL</xsl:text>
        <xsl:text>PT</xsl:text>
        <xsl:text>PR</xsl:text>
        <xsl:text>QA</xsl:text>
        <xsl:text>RE</xsl:text>
        <xsl:text>RO</xsl:text>
        <xsl:text>RU</xsl:text>
        <xsl:text>RW</xsl:text>
        <xsl:text>SH</xsl:text>
        <xsl:text>KN</xsl:text>
        <xsl:text>LC</xsl:text>
        <xsl:text>PM</xsl:text>
        <xsl:text>VC</xsl:text>
        <xsl:text>WS</xsl:text>
        <xsl:text>SM</xsl:text>
        <xsl:text>ST</xsl:text>
        <xsl:text>SA</xsl:text>
        <xsl:text>SN</xsl:text>
        <xsl:text>CS</xsl:text>
        <xsl:text>SC</xsl:text>
        <xsl:text>SL</xsl:text>
        <xsl:text>SG</xsl:text>
        <xsl:text>SK</xsl:text>
        <xsl:text>SI</xsl:text>
        <xsl:text>SB</xsl:text>
        <xsl:text>SO</xsl:text>
        <xsl:text>ZA</xsl:text>
        <xsl:text>GS</xsl:text>
        <xsl:text>ES</xsl:text>
        <xsl:text>LK</xsl:text>
        <xsl:text>SD</xsl:text>
        <xsl:text>SR</xsl:text>
        <xsl:text>SJ</xsl:text>
        <xsl:text>SZ</xsl:text>
        <xsl:text>SE</xsl:text>
        <xsl:text>CH</xsl:text>
        <xsl:text>SY</xsl:text>
        <xsl:text>TW</xsl:text>
        <xsl:text>TJ</xsl:text>
        <xsl:text>TZ</xsl:text>
        <xsl:text>TH</xsl:text>
        <xsl:text>TL</xsl:text>
        <xsl:text>TG</xsl:text>
        <xsl:text>TK</xsl:text>
        <xsl:text>TO</xsl:text>
        <xsl:text>TT</xsl:text>
        <xsl:text>TN</xsl:text>
        <xsl:text>TR</xsl:text>
        <xsl:text>TM</xsl:text>
        <xsl:text>TC</xsl:text>
        <xsl:text>TV</xsl:text>
        <xsl:text>VI</xsl:text>
        <xsl:text>UG</xsl:text>
        <xsl:text>UA</xsl:text>
        <xsl:text>AE</xsl:text>
        <xsl:text>GB</xsl:text>
        <xsl:text>UM</xsl:text>
        <xsl:text>US</xsl:text>
        <xsl:text>UY</xsl:text>
        <xsl:text>UZ</xsl:text>
        <xsl:text>VU</xsl:text>
        <xsl:text>VE</xsl:text>
        <xsl:text>VN</xsl:text>
        <xsl:text>WF</xsl:text>
        <xsl:text>EH</xsl:text>
        <xsl:text>YE</xsl:text>
        <xsl:text>ZM</xsl:text>
        <xsl:text>ZW</xsl:text>
    </xsl:variable>
    <!--<xsl:include href="inc/iso639-2.xsl"/>-->
    <xsl:variable name="iso639-2">
        <xsl:text>aar</xsl:text>
        <xsl:text>abk</xsl:text>
        <xsl:text>ace</xsl:text>
        <xsl:text>ach</xsl:text>
        <xsl:text>ada</xsl:text>
        <xsl:text>ady</xsl:text>
        <xsl:text>afa</xsl:text>
        <xsl:text>afh</xsl:text>
        <xsl:text>afr</xsl:text>
        <xsl:text>ain</xsl:text>
        <xsl:text>aka</xsl:text>
        <xsl:text>akk</xsl:text>
        <xsl:text>alb</xsl:text>
        <xsl:text>ale</xsl:text>
        <xsl:text>alg</xsl:text>
        <xsl:text>alt</xsl:text>
        <xsl:text>amh</xsl:text>
        <xsl:text>ang</xsl:text>
        <xsl:text>anp</xsl:text>
        <xsl:text>apa</xsl:text>
        <xsl:text>ara</xsl:text>
        <xsl:text>arc</xsl:text>
        <xsl:text>arg</xsl:text>
        <xsl:text>arm</xsl:text>
        <xsl:text>arn</xsl:text>
        <xsl:text>arp</xsl:text>
        <xsl:text>art</xsl:text>
        <xsl:text>arw</xsl:text>
        <xsl:text>asm</xsl:text>
        <xsl:text>ast</xsl:text>
        <xsl:text>ath</xsl:text>
        <xsl:text>aus</xsl:text>
        <xsl:text>ava</xsl:text>
        <xsl:text>ave</xsl:text>
        <xsl:text>awa</xsl:text>
        <xsl:text>aym</xsl:text>
        <xsl:text>aze</xsl:text>
        <xsl:text>bad</xsl:text>
        <xsl:text>bai</xsl:text>
        <xsl:text>bak</xsl:text>
        <xsl:text>bal</xsl:text>
        <xsl:text>bam</xsl:text>
        <xsl:text>ban</xsl:text>
        <xsl:text>baq</xsl:text>
        <xsl:text>bas</xsl:text>
        <xsl:text>bat</xsl:text>
        <xsl:text>bej</xsl:text>
        <xsl:text>bel</xsl:text>
        <xsl:text>bem</xsl:text>
        <xsl:text>ben</xsl:text>
        <xsl:text>ber</xsl:text>
        <xsl:text>bho</xsl:text>
        <xsl:text>bih</xsl:text>
        <xsl:text>bik</xsl:text>
        <xsl:text>bin</xsl:text>
        <xsl:text>bis</xsl:text>
        <xsl:text>bla</xsl:text>
        <xsl:text>bnt</xsl:text>
        <xsl:text>tib</xsl:text>
        <xsl:text>bos</xsl:text>
        <xsl:text>bra</xsl:text>
        <xsl:text>bre</xsl:text>
        <xsl:text>btk</xsl:text>
        <xsl:text>bua</xsl:text>
        <xsl:text>bug</xsl:text>
        <xsl:text>bul</xsl:text>
        <xsl:text>bur</xsl:text>
        <xsl:text>byn</xsl:text>
        <xsl:text>cad</xsl:text>
        <xsl:text>cai</xsl:text>
        <xsl:text>car</xsl:text>
        <xsl:text>cat</xsl:text>
        <xsl:text>cau</xsl:text>
        <xsl:text>ceb</xsl:text>
        <xsl:text>cel</xsl:text>
        <xsl:text>cze</xsl:text>
        <xsl:text>cha</xsl:text>
        <xsl:text>chb</xsl:text>
        <xsl:text>che</xsl:text>
        <xsl:text>chg</xsl:text>
        <xsl:text>chi</xsl:text>
        <xsl:text>chk</xsl:text>
        <xsl:text>chm</xsl:text>
        <xsl:text>chn</xsl:text>
        <xsl:text>cho</xsl:text>
        <xsl:text>chp</xsl:text>
        <xsl:text>chr</xsl:text>
        <xsl:text>chu</xsl:text>
        <xsl:text>chv</xsl:text>
        <xsl:text>chy</xsl:text>
        <xsl:text>cmc</xsl:text>
        <xsl:text>cop</xsl:text>
        <xsl:text>cor</xsl:text>
        <xsl:text>cos</xsl:text>
        <xsl:text>cpe</xsl:text>
        <xsl:text>cpf</xsl:text>
        <xsl:text>cpp</xsl:text>
        <xsl:text>cre</xsl:text>
        <xsl:text>crh</xsl:text>
        <xsl:text>crp</xsl:text>
        <xsl:text>csb</xsl:text>
        <xsl:text>cus</xsl:text>
        <xsl:text>wel</xsl:text>
        <xsl:text>cze</xsl:text>
        <xsl:text>dak</xsl:text>
        <xsl:text>dan</xsl:text>
        <xsl:text>dar</xsl:text>
        <xsl:text>day</xsl:text>
        <xsl:text>del</xsl:text>
        <xsl:text>den</xsl:text>
        <xsl:text>ger</xsl:text>
        <xsl:text>dgr</xsl:text>
        <xsl:text>din</xsl:text>
        <xsl:text>div</xsl:text>
        <xsl:text>doi</xsl:text>
        <xsl:text>dra</xsl:text>
        <xsl:text>dsb</xsl:text>
        <xsl:text>dua</xsl:text>
        <xsl:text>dum</xsl:text>
        <xsl:text>dut</xsl:text>
        <xsl:text>dyu</xsl:text>
        <xsl:text>dzo</xsl:text>
        <xsl:text>efi</xsl:text>
        <xsl:text>egy</xsl:text>
        <xsl:text>eka</xsl:text>
        <xsl:text>gre</xsl:text>
        <xsl:text>elx</xsl:text>
        <xsl:text>eng</xsl:text>
        <xsl:text>enm</xsl:text>
        <xsl:text>epo</xsl:text>
        <xsl:text>est</xsl:text>
        <xsl:text>baq</xsl:text>
        <xsl:text>ewe</xsl:text>
        <xsl:text>ewo</xsl:text>
        <xsl:text>fan</xsl:text>
        <xsl:text>fao</xsl:text>
        <xsl:text>per</xsl:text>
        <xsl:text>fat</xsl:text>
        <xsl:text>fij</xsl:text>
        <xsl:text>fil</xsl:text>
        <xsl:text>fin</xsl:text>
        <xsl:text>fiu</xsl:text>
        <xsl:text>fon</xsl:text>
        <xsl:text>fre</xsl:text>
        <xsl:text>fre</xsl:text>
        <xsl:text>frm</xsl:text>
        <xsl:text>fro</xsl:text>
        <xsl:text>frr</xsl:text>
        <xsl:text>frs</xsl:text>
        <xsl:text>fry</xsl:text>
        <xsl:text>ful</xsl:text>
        <xsl:text>fur</xsl:text>
        <xsl:text>gaa</xsl:text>
        <xsl:text>gay</xsl:text>
        <xsl:text>gba</xsl:text>
        <xsl:text>gem</xsl:text>
        <xsl:text>geo</xsl:text>
        <xsl:text>ger</xsl:text>
        <xsl:text>gez</xsl:text>
        <xsl:text>gil</xsl:text>
        <xsl:text>gla</xsl:text>
        <xsl:text>gle</xsl:text>
        <xsl:text>glg</xsl:text>
        <xsl:text>glv</xsl:text>
        <xsl:text>gmh</xsl:text>
        <xsl:text>goh</xsl:text>
        <xsl:text>gon</xsl:text>
        <xsl:text>gor</xsl:text>
        <xsl:text>got</xsl:text>
        <xsl:text>grb</xsl:text>
        <xsl:text>grc</xsl:text>
        <xsl:text>gre</xsl:text>
        <xsl:text>grn</xsl:text>
        <xsl:text>gsw</xsl:text>
        <xsl:text>guj</xsl:text>
        <xsl:text>gwi</xsl:text>
        <xsl:text>hai</xsl:text>
        <xsl:text>hat</xsl:text>
        <xsl:text>hau</xsl:text>
        <xsl:text>haw</xsl:text>
        <xsl:text>heb</xsl:text>
        <xsl:text>her</xsl:text>
        <xsl:text>hil</xsl:text>
        <xsl:text>him</xsl:text>
        <xsl:text>hin</xsl:text>
        <xsl:text>hit</xsl:text>
        <xsl:text>hmn</xsl:text>
        <xsl:text>hmo</xsl:text>
        <xsl:text>scr</xsl:text>
        <xsl:text>hsb</xsl:text>
        <xsl:text>hun</xsl:text>
        <xsl:text>hup</xsl:text>
        <xsl:text>arm</xsl:text>
        <xsl:text>iba</xsl:text>
        <xsl:text>ibo</xsl:text>
        <xsl:text>ice</xsl:text>
        <xsl:text>ido</xsl:text>
        <xsl:text>iii</xsl:text>
        <xsl:text>ijo</xsl:text>
        <xsl:text>iku</xsl:text>
        <xsl:text>ile</xsl:text>
        <xsl:text>ilo</xsl:text>
        <xsl:text>ina</xsl:text>
        <xsl:text>inc</xsl:text>
        <xsl:text>ind</xsl:text>
        <xsl:text>ine</xsl:text>
        <xsl:text>inh</xsl:text>
        <xsl:text>ipk</xsl:text>
        <xsl:text>ira</xsl:text>
        <xsl:text>iro</xsl:text>
        <xsl:text>ice</xsl:text>
        <xsl:text>ita</xsl:text>
        <xsl:text>jav</xsl:text>
        <xsl:text>jbo</xsl:text>
        <xsl:text>jpn</xsl:text>
        <xsl:text>jpr</xsl:text>
        <xsl:text>jrb</xsl:text>
        <xsl:text>kaa</xsl:text>
        <xsl:text>kab</xsl:text>
        <xsl:text>kac</xsl:text>
        <xsl:text>kal</xsl:text>
        <xsl:text>kam</xsl:text>
        <xsl:text>kan</xsl:text>
        <xsl:text>kar</xsl:text>
        <xsl:text>kas</xsl:text>
        <xsl:text>geo</xsl:text>
        <xsl:text>kau</xsl:text>
        <xsl:text>kaw</xsl:text>
        <xsl:text>kaz</xsl:text>
        <xsl:text>kbd</xsl:text>
        <xsl:text>kha</xsl:text>
        <xsl:text>khi</xsl:text>
        <xsl:text>khm</xsl:text>
        <xsl:text>kho</xsl:text>
        <xsl:text>kik</xsl:text>
        <xsl:text>kin</xsl:text>
        <xsl:text>kir</xsl:text>
        <xsl:text>kmb</xsl:text>
        <xsl:text>kok</xsl:text>
        <xsl:text>kom</xsl:text>
        <xsl:text>kon</xsl:text>
        <xsl:text>kor</xsl:text>
        <xsl:text>kos</xsl:text>
        <xsl:text>kpe</xsl:text>
        <xsl:text>krc</xsl:text>
        <xsl:text>krl</xsl:text>
        <xsl:text>kro</xsl:text>
        <xsl:text>kru</xsl:text>
        <xsl:text>kua</xsl:text>
        <xsl:text>kum</xsl:text>
        <xsl:text>kur</xsl:text>
        <xsl:text>kut</xsl:text>
        <xsl:text>lad</xsl:text>
        <xsl:text>lah</xsl:text>
        <xsl:text>lam</xsl:text>
        <xsl:text>lao</xsl:text>
        <xsl:text>lat</xsl:text>
        <xsl:text>lav</xsl:text>
        <xsl:text>lez</xsl:text>
        <xsl:text>lim</xsl:text>
        <xsl:text>lin</xsl:text>
        <xsl:text>lit</xsl:text>
        <xsl:text>lol</xsl:text>
        <xsl:text>loz</xsl:text>
        <xsl:text>ltz</xsl:text>
        <xsl:text>lua</xsl:text>
        <xsl:text>lub</xsl:text>
        <xsl:text>lug</xsl:text>
        <xsl:text>lui</xsl:text>
        <xsl:text>lun</xsl:text>
        <xsl:text>luo</xsl:text>
        <xsl:text>lus</xsl:text>
        <xsl:text>mac</xsl:text>
        <xsl:text>mad</xsl:text>
        <xsl:text>mag</xsl:text>
        <xsl:text>mah</xsl:text>
        <xsl:text>mai</xsl:text>
        <xsl:text>mak</xsl:text>
        <xsl:text>mal</xsl:text>
        <xsl:text>man</xsl:text>
        <xsl:text>mao</xsl:text>
        <xsl:text>map</xsl:text>
        <xsl:text>mar</xsl:text>
        <xsl:text>mas</xsl:text>
        <xsl:text>may</xsl:text>
        <xsl:text>mdf</xsl:text>
        <xsl:text>mdr</xsl:text>
        <xsl:text>men</xsl:text>
        <xsl:text>mga</xsl:text>
        <xsl:text>mic</xsl:text>
        <xsl:text>min</xsl:text>
        <xsl:text>mis</xsl:text>
        <xsl:text>mac</xsl:text>
        <xsl:text>mkh</xsl:text>
        <xsl:text>mlg</xsl:text>
        <xsl:text>mlt</xsl:text>
        <xsl:text>mnc</xsl:text>
        <xsl:text>mni</xsl:text>
        <xsl:text>mno</xsl:text>
        <xsl:text>moh</xsl:text>
        <xsl:text>mol</xsl:text>
        <xsl:text>mon</xsl:text>
        <xsl:text>mos</xsl:text>
        <xsl:text>mao</xsl:text>
        <xsl:text>may</xsl:text>
        <xsl:text>mul</xsl:text>
        <xsl:text>mun</xsl:text>
        <xsl:text>mus</xsl:text>
        <xsl:text>mwl</xsl:text>
        <xsl:text>mwr</xsl:text>
        <xsl:text>bur</xsl:text>
        <xsl:text>myn</xsl:text>
        <xsl:text>myv</xsl:text>
        <xsl:text>nah</xsl:text>
        <xsl:text>nai</xsl:text>
        <xsl:text>nap</xsl:text>
        <xsl:text>nau</xsl:text>
        <xsl:text>nav</xsl:text>
        <xsl:text>nbl</xsl:text>
        <xsl:text>nde</xsl:text>
        <xsl:text>ndo</xsl:text>
        <xsl:text>nds</xsl:text>
        <xsl:text>nep</xsl:text>
        <xsl:text>new</xsl:text>
        <xsl:text>nia</xsl:text>
        <xsl:text>nic</xsl:text>
        <xsl:text>niu</xsl:text>
        <xsl:text>dut</xsl:text>
        <xsl:text>nno</xsl:text>
        <xsl:text>nob</xsl:text>
        <xsl:text>nog</xsl:text>
        <xsl:text>non</xsl:text>
        <xsl:text>nor</xsl:text>
        <xsl:text>nqo</xsl:text>
        <xsl:text>nso</xsl:text>
        <xsl:text>nub</xsl:text>
        <xsl:text>nwc</xsl:text>
        <xsl:text>nya</xsl:text>
        <xsl:text>nym</xsl:text>
        <xsl:text>nyn</xsl:text>
        <xsl:text>nyo</xsl:text>
        <xsl:text>nzi</xsl:text>
        <xsl:text>oci</xsl:text>
        <xsl:text>oji</xsl:text>
        <xsl:text>ori</xsl:text>
        <xsl:text>orm</xsl:text>
        <xsl:text>osa</xsl:text>
        <xsl:text>oss</xsl:text>
        <xsl:text>ota</xsl:text>
        <xsl:text>oto</xsl:text>
        <xsl:text>paa</xsl:text>
        <xsl:text>pag</xsl:text>
        <xsl:text>pal</xsl:text>
        <xsl:text>pam</xsl:text>
        <xsl:text>pan</xsl:text>
        <xsl:text>pap</xsl:text>
        <xsl:text>pau</xsl:text>
        <xsl:text>peo</xsl:text>
        <xsl:text>per</xsl:text>
        <xsl:text>phi</xsl:text>
        <xsl:text>phn</xsl:text>
        <xsl:text>pli</xsl:text>
        <xsl:text>pol</xsl:text>
        <xsl:text>pon</xsl:text>
        <xsl:text>por</xsl:text>
        <xsl:text>pra</xsl:text>
        <xsl:text>pro</xsl:text>
        <xsl:text>pus</xsl:text>
        <xsl:text>qaa</xsl:text>
        <xsl:text>que</xsl:text>
        <xsl:text>raj</xsl:text>
        <xsl:text>rap</xsl:text>
        <xsl:text>rar</xsl:text>
        <xsl:text>roa</xsl:text>
        <xsl:text>roh</xsl:text>
        <xsl:text>rom</xsl:text>
        <xsl:text>rum</xsl:text>
        <xsl:text>rum</xsl:text>
        <xsl:text>run</xsl:text>
        <xsl:text>rup</xsl:text>
        <xsl:text>rus</xsl:text>
        <xsl:text>sad</xsl:text>
        <xsl:text>sag</xsl:text>
        <xsl:text>sah</xsl:text>
        <xsl:text>sai</xsl:text>
        <xsl:text>sal</xsl:text>
        <xsl:text>sam</xsl:text>
        <xsl:text>san</xsl:text>
        <xsl:text>sas</xsl:text>
        <xsl:text>sat</xsl:text>
        <xsl:text>scc</xsl:text>
        <xsl:text>scn</xsl:text>
        <xsl:text>sco</xsl:text>
        <xsl:text>scr</xsl:text>
        <xsl:text>sel</xsl:text>
        <xsl:text>sem</xsl:text>
        <xsl:text>sga</xsl:text>
        <xsl:text>sgn</xsl:text>
        <xsl:text>shn</xsl:text>
        <xsl:text>sid</xsl:text>
        <xsl:text>sin</xsl:text>
        <xsl:text>sio</xsl:text>
        <xsl:text>sit</xsl:text>
        <xsl:text>sla</xsl:text>
        <xsl:text>slo</xsl:text>
        <xsl:text>slo</xsl:text>
        <xsl:text>slv</xsl:text>
        <xsl:text>sma</xsl:text>
        <xsl:text>sme</xsl:text>
        <xsl:text>smi</xsl:text>
        <xsl:text>smj</xsl:text>
        <xsl:text>smn</xsl:text>
        <xsl:text>smo</xsl:text>
        <xsl:text>sms</xsl:text>
        <xsl:text>sna</xsl:text>
        <xsl:text>snd</xsl:text>
        <xsl:text>snk</xsl:text>
        <xsl:text>sog</xsl:text>
        <xsl:text>som</xsl:text>
        <xsl:text>son</xsl:text>
        <xsl:text>sot</xsl:text>
        <xsl:text>spa</xsl:text>
        <xsl:text>alb</xsl:text>
        <xsl:text>srd</xsl:text>
        <xsl:text>srn</xsl:text>
        <xsl:text>scc</xsl:text>
        <xsl:text>srr</xsl:text>
        <xsl:text>ssa</xsl:text>
        <xsl:text>ssw</xsl:text>
        <xsl:text>suk</xsl:text>
        <xsl:text>sun</xsl:text>
        <xsl:text>sus</xsl:text>
        <xsl:text>sux</xsl:text>
        <xsl:text>swa</xsl:text>
        <xsl:text>swe</xsl:text>
        <xsl:text>syr</xsl:text>
        <xsl:text>tah</xsl:text>
        <xsl:text>tai</xsl:text>
        <xsl:text>tam</xsl:text>
        <xsl:text>tat</xsl:text>
        <xsl:text>tel</xsl:text>
        <xsl:text>tem</xsl:text>
        <xsl:text>ter</xsl:text>
        <xsl:text>tet</xsl:text>
        <xsl:text>tgk</xsl:text>
        <xsl:text>tgl</xsl:text>
        <xsl:text>tha</xsl:text>
        <xsl:text>tib</xsl:text>
        <xsl:text>tig</xsl:text>
        <xsl:text>tir</xsl:text>
        <xsl:text>tiv</xsl:text>
        <xsl:text>tkl</xsl:text>
        <xsl:text>tlh</xsl:text>
        <xsl:text>tli</xsl:text>
        <xsl:text>tmh</xsl:text>
        <xsl:text>tog</xsl:text>
        <xsl:text>ton</xsl:text>
        <xsl:text>tpi</xsl:text>
        <xsl:text>tsi</xsl:text>
        <xsl:text>tsn</xsl:text>
        <xsl:text>tso</xsl:text>
        <xsl:text>tuk</xsl:text>
        <xsl:text>tum</xsl:text>
        <xsl:text>tup</xsl:text>
        <xsl:text>tur</xsl:text>
        <xsl:text>tut</xsl:text>
        <xsl:text>tvl</xsl:text>
        <xsl:text>twi</xsl:text>
        <xsl:text>tyv</xsl:text>
        <xsl:text>udm</xsl:text>
        <xsl:text>uga</xsl:text>
        <xsl:text>uig</xsl:text>
        <xsl:text>ukr</xsl:text>
        <xsl:text>umb</xsl:text>
        <xsl:text>und</xsl:text>
        <xsl:text>urd</xsl:text>
        <xsl:text>uzb</xsl:text>
        <xsl:text>vai</xsl:text>
        <xsl:text>ven</xsl:text>
        <xsl:text>vie</xsl:text>
        <xsl:text>vol</xsl:text>
        <xsl:text>vot</xsl:text>
        <xsl:text>wak</xsl:text>
        <xsl:text>wal</xsl:text>
        <xsl:text>war</xsl:text>
        <xsl:text>was</xsl:text>
        <xsl:text>wel</xsl:text>
        <xsl:text>wen</xsl:text>
        <xsl:text>wln</xsl:text>
        <xsl:text>wol</xsl:text>
        <xsl:text>xal</xsl:text>
        <xsl:text>xho</xsl:text>
        <xsl:text>yao</xsl:text>
        <xsl:text>yap</xsl:text>
        <xsl:text>yid</xsl:text>
        <xsl:text>yor</xsl:text>
        <xsl:text>ypk</xsl:text>
        <xsl:text>zap</xsl:text>
        <xsl:text>zen</xsl:text>
        <xsl:text>zha</xsl:text>
        <xsl:text>chi</xsl:text>
        <xsl:text>znd</xsl:text>
        <xsl:text>zul</xsl:text>
        <xsl:text>zun</xsl:text>
        <xsl:text>zxx</xsl:text>
        <xsl:text>zza</xsl:text>
    </xsl:variable>
    <!-- Do you have a Handle server?  If so, specify the base URI below including the trailing slash a la: http://hdl.loc.gov/ -->
    <xsl:variable name="handleServer">
		<xsl:text>http://hdl.loc.gov/</xsl:text>
    </xsl:variable>
    <xsl:template match="*[not(node())]"/> <!-- strip empty DC elements that are output by tools like ContentDM -->
    <xsl:template match="/">
        <xsl:if test="sru_dc:dcCollection">
            <xsl:apply-templates select="sru_dc:dcCollection"/>
        </xsl:if>
        <xsl:if test="sru_dc:dc">
            <xsl:apply-templates select="sru_dc:dc"/>
        </xsl:if>
        <xsl:if test="oai_dc:dc">
            <xsl:apply-templates/>
        </xsl:if>
    </xsl:template>
    <xsl:template match="sru_dc:dcCollection">
        <modsCollection xmlns="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-2.xsd">
			<xsl:apply-templates select="sru_dc:dc">
				<xsl:with-param name="dcCollection">
					<xsl:text>true</xsl:text>
				</xsl:with-param>
			</xsl:apply-templates>
        </modsCollection>
    </xsl:template>
    <xsl:template match="sru_dc:dc">
		<xsl:param name="dcCollection"/>
		<xsl:choose>
			<xsl:when test="$dcCollection = 'true'">
				<mods version="3.2">
					<xsl:call-template name="dcMain"/>
				</mods>
			</xsl:when>
			<xsl:otherwise>
				<mods version="3.2" xmlns="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-2.xsd">
					<xsl:call-template name="dcMain"/>
				</mods>
			</xsl:otherwise>
		</xsl:choose>
        
    </xsl:template>
    <xsl:template match="oai_dc:dc">
        <mods version="3.2" xmlns="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-2.xsd">
			<xsl:call-template name="dcMain"/>
        </mods>
    </xsl:template>
    <xsl:template name="dcMain">
	    <xsl:for-each select="dc:title">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:creator">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
             <xsl:for-each select="dc:contributor">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:type">
                <xsl:choose>
			<xsl:when test="contains(text(), 'Collection') or contains(text(), 'collection')">
				<genre authority="dct">
                    			<xsl:text>collection</xsl:text>
                		</genre>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="."/>
			</xsl:otherwise>
		</xsl:choose>
            </xsl:for-each>
            <xsl:for-each select="dc:subject | dc:coverage">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:description">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:publisher">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:date">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:format">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:identifier">
                <xsl:choose>
                    <xsl:when test="starts-with(text(), 'http://')">
                        <location>
                            <url>
                                <xsl:value-of select="."/>
                            </url>
                        </location>
                        <xsl:apply-templates select="."/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <xsl:for-each select="dc:source | dc:relation">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:language">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            <xsl:for-each select="dc:rights">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
    </xsl:template>
    <xsl:template match="dc:title">
        <titleInfo>
            <title>
                <xsl:apply-templates/>
            </title>
        </titleInfo>
    </xsl:template>
    <xsl:template match="dc:creator">
        <name>
            <namePart>
                <xsl:apply-templates/>
            </namePart>
            <role>
                <roleTerm type="text">
                    <xsl:text>creator</xsl:text>
                </roleTerm>
            </role>
            <!--<displayForm>
                <xsl:value-of select="."/>
            </displayForm>-->
        </name>
    </xsl:template>
    <xsl:template match="dc:subject">
        <subject>
            <topic>
                <xsl:apply-templates/>
            </topic>
        </subject>
    </xsl:template>
    <xsl:template match="dc:description">
        <!--<abstract>
            <xsl:apply-templates/>
        </abstract>-->
        <note>
            <xsl:apply-templates/>
        </note>
        <!--<tableOfContents>
            <xsl:apply-templates/>
        </tableOfContents>-->
    </xsl:template>
    <xsl:template match="dc:publisher">
        <originInfo>
            <publisher>
                <xsl:apply-templates/>
            </publisher>
        </originInfo>
    </xsl:template>
    <xsl:template match="dc:contributor">
        <name>
            <namePart>
                <xsl:apply-templates/>
            </namePart>
            <!-- <role>
                <roleTerm type="text">
                    <xsl:text>contributor</xsl:text>
                </roleTerm>
            </role> -->
        </name>
    </xsl:template>
    <xsl:template match="dc:date">
        <originInfo>
            <!--<dateIssued>
                <xsl:apply-templates/>
            </dateIssued>-->
            <!--<dateCreated>
                <xsl:apply-templates/>
            </dateCreated>
            <dateCaptured>
                <xsl:apply-templates/>
            </dateCaptured>-->
            <dateOther>
                <xsl:apply-templates/>
            </dateOther>
        </originInfo>
    </xsl:template>
    <xsl:template match="dc:type">
        <!-- based on DCMI Type Vocabulary as of 2006-10-27 at http://dublincore.org/documents/dcmi-type-vocabulary/ ... see also the included dcmiType.xsl serving as variable $types -->
	<xsl:choose>
            <xsl:when test="string(text()) = 'Dataset' or string(text()) = 'dataset'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
                    	<xsl:text>software</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>dataset</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'Event' or string(text()) = 'event'">
                <genre authority="dct">
                    <xsl:text>event</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'Image' or string(text()) = 'image'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>still image</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>image</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'InteractiveResource' or string(text()) = 'interactiveresource' or string(text()) = 'Interactive Resource' or string(text()) = 'interactive resource' or string(text()) = 'interactiveResource'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>software, multimedia</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>interactive resource</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'MovingImage' or string(text()) = 'movingimage' or string(text()) = 'Moving Image' or string(text()) = 'moving image' or string(text()) = 'movingImage'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>moving image</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>moving image</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'PhysicalObject' or string(text()) = 'physicalobject' or string(text()) = 'Physical Object' or string(text()) = 'physical object' or string(text()) = 'physicalObject'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>three dimensional object</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>physical object</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'Service' or string(text()) = 'service'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>software, multimedia</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>service</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'Software' or string(text()) = 'software'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>software, multimedia</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>software</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'Sound' or string(text()) = 'sound'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>sound recording</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>sound</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'StillImage' or string(text()) = 'stillimage' or string(text()) = 'Still Image' or string(text()) = 'still image' or string(text()) = 'stillImage'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>still image</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>still image</xsl:text>
                </genre>
            </xsl:when>
            <xsl:when test="string(text()) = 'Text' or string(text()) = 'text'">
                <typeOfResource>
			<xsl:if test="../dc:type[string(text()) = 'collection' or string(text()) = 'Collection']">
				<xsl:attribute name="collection">
					<xsl:text>yes</xsl:text>
				</xsl:attribute>
			</xsl:if>
			<xsl:text>text</xsl:text>
                </typeOfResource>
                <genre authority="dct">
                    <xsl:text>text</xsl:text>
                </genre>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="not(string($types) = text())">
			<xsl:variable name="lowercaseType" select="translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
                	<!--<typeOfResource>
                    		<xsl:text>mixed material</xsl:text>
                	</typeOfResource>-->
                	<genre>
                    		<xsl:value-of select="$lowercaseType"/>
                	</genre>
		</xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="dc:format">
        <physicalDescription>
            <xsl:choose>
                <xsl:when test="contains(text(), '/')">
                    <xsl:variable name="mime" select="substring-before(text(), '/')"/>
                    <xsl:choose>
                        <xsl:when test="contains($mimeTypeDirectories, $mime)">
                        <internetMediaType>
                            <xsl:apply-templates/>
                        </internetMediaType>
                        </xsl:when>
                        <xsl:otherwise>
                            <note>
                                <xsl:apply-templates/>
                            </note>
                        </xsl:otherwise>
                    </xsl:choose>                    
                </xsl:when>
                <xsl:when test="starts-with(text(), '1') or starts-with(text(), '2') or starts-with(text(), '3') or starts-with(text(), '4') or starts-with(text(), '5') or starts-with(text(), '6') or starts-with(text(), '7') or starts-with(text(), '8') or starts-with(text(), '9')">
                    <extent>
                        <xsl:apply-templates/>
                    </extent>
                </xsl:when>
                <xsl:when test="contains($forms, text())">
                    <form>
                        <xsl:apply-templates/>
                    </form>
                </xsl:when>
                <xsl:otherwise>
                    <note>
                        <xsl:apply-templates/>
                    </note>
                </xsl:otherwise>
            </xsl:choose>
        </physicalDescription>
    </xsl:template>
    <xsl:template match="dc:identifier">
        <xsl:variable name="iso-3166Check">
            <xsl:value-of select="substring(text(), 1, 2)"/>
        </xsl:variable>
        <identifier>
            <xsl:attribute name="type">
                <xsl:choose>
                    <!-- handled by location/url -->
                    <xsl:when test="starts-with(text(), 'http://') and (not(contains(text(), $handleServer) or not(contains(substring-after(text(), 'http://'), 'hdl'))))">
                        <xsl:text>uri</xsl:text>
                    </xsl:when>
                    <xsl:when test="starts-with(text(),'urn:hdl') or starts-with(text(),'hdl') or starts-with(text(),'http://hdl.')">
                        <xsl:text>hdl</xsl:text>
                    </xsl:when>
                    <xsl:when test="starts-with(text(), 'doi')">
                        <xsl:text>doi</xsl:text>
                    </xsl:when>
                    <xsl:when test="starts-with(text(), 'ark')">
                        <xsl:text>ark</xsl:text>
                    </xsl:when>
                    <xsl:when test="contains(text(), 'purl')">
                        <xsl:text>purl</xsl:text>
                    </xsl:when>
                    <xsl:when test="starts-with(text(), 'tag')">
                        <xsl:text>tag</xsl:text>
                    </xsl:when>
                    <!-- will need to update for ISBN 13 as of January 1, 2007, see XSL tool at http://isbntools.com/ -->
                    <xsl:when test="(starts-with(text(), 'ISBN') or starts-with(text(), 'isbn')) or ((string-length(text()) = 13) and contains(text(), '-') and (starts-with(text(), '0') or starts-with(text(), '1'))) or ((string-length(text()) = 10) and (starts-with(text(), '0') or starts-with(text(), '1')))">
                        <xsl:text>isbn</xsl:text>
                    </xsl:when>
                    <xsl:when test="(starts-with(text(), 'ISRC') or starts-with(text(), 'isrc')) or ((string-length(text()) = 12) and (contains($iso3166-1, $iso-3166Check))) or ((string-length(text()) = 15) and (contains(text(), '-') or contains(text(), '/')) and contains($iso3166-1, $iso-3166Check))">
                        <xsl:text>isrc</xsl:text>
                    </xsl:when>
                    <xsl:when test="(starts-with(text(), 'ISMN') or starts-with(text(), 'ismn')) or starts-with(text(), 'M') and ((string-length(text()) = 11) and contains(text(), '-') or string-length(text()) = 9)">
                        <xsl:text>ismn</xsl:text>
                    </xsl:when>
                    <xsl:when test="(starts-with(text(), 'ISSN') or starts-with(text(), 'issn')) or ((string-length(text()) = 9) and contains(text(), '-') or string-length(text()) = 8)">
                        <xsl:text>issn</xsl:text>
                    </xsl:when>
                    <xsl:when test="starts-with(text(), 'ISTC') or starts-with(text(), 'istc')">
                        <xsl:text>istc</xsl:text>
                    </xsl:when>
                    <xsl:when test="(starts-with(text(), 'UPC') or starts-with(text(), 'upc')) or (string-length(text()) = 12 and not(contains(text(), ' ')) and not(contains($iso3166-1, $iso-3166Check)))">
                        <xsl:text>upc</xsl:text>
                    </xsl:when>
                    <xsl:when test="(starts-with(text(), 'SICI') or starts-with(text(), 'sici')) or ((starts-with(text(), '0') or starts-with(text(), '1')) and (contains(text(), ';') and contains(text(), '(') and contains(text(), ')') and contains(text(), '&lt;') and contains(text(), '&gt;')))">
                        <xsl:text>sici</xsl:text>
                    </xsl:when>
                    <xsl:when test="starts-with(text(), 'LCCN') or starts-with(text(), 'lccn')">
                        <!-- probably can't do this quickly or easily without regexes and XSL 2.0 -->
                        <xsl:text>lccn</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>local</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:choose>
		<xsl:when test="starts-with(text(),'urn:hdl') or starts-with(text(),'hdl') or starts-with(text(),$handleServer)">
			<xsl:value-of select="concat('hdl:',substring-after(text(),$handleServer))"/>
		</xsl:when>
		<xsl:otherwise>
			<xsl:apply-templates/>
		</xsl:otherwise>
            </xsl:choose>
        </identifier>
    </xsl:template>
    <xsl:template match="dc:source">
        <relatedItem type="original">
            <titleInfo>
                <title>
                    <xsl:apply-templates/>
                </title>
            </titleInfo>
        </relatedItem>
    </xsl:template>
    <xsl:template match="dc:language">
        <language>
            <xsl:choose>
                <xsl:when test="string-length(text()) = 3 and contains($iso639-2, text())">
                    <languageTerm type="code">
                        <xsl:apply-templates/>
                    </languageTerm>
                </xsl:when>
                <xsl:otherwise>
                    <languageTerm type="text">
                        <xsl:apply-templates/>
                    </languageTerm>
                </xsl:otherwise>
            </xsl:choose>
        </language>
    </xsl:template>
    <xsl:template match="dc:relation">
        <relatedItem>
			<xsl:choose>
				<xsl:when test="starts-with(text(), 'http://')">
					<location>
						<url>
							<xsl:value-of select="."/>
						</url>
					</location>
					<identifer type="uri">
						<xsl:apply-templates/>
					</identifer>
				</xsl:when>
				<xsl:otherwise>
					<titleInfo>
						<title>
							<xsl:apply-templates/>
						</title>
					</titleInfo>
				</xsl:otherwise>
			</xsl:choose>            
        </relatedItem>
    </xsl:template>
    <xsl:template match="dc:coverage">
            <xsl:choose>
                <xsl:when test="string-length(text()) >= 3 and (starts-with(text(), '1') or starts-with(text(), '2') or starts-with(text(), '3') or starts-with(text(), '4') or starts-with(text(), '5') or starts-with(text(), '6') or starts-with(text(), '7') or starts-with(text(), '8') or starts-with(text(), '9') or starts-with(text(), '-') or contains(text(), 'AD') or contains(text(), 'BC')) and not(contains(text(), ':'))">
                    <!-- using XSL 2.0 for date parsing is A Better And Saner Idea -->
                    <subject>
                        <temporal>
                           <xsl:apply-templates/>
                        </temporal>
                    </subject>
                </xsl:when>
                <xsl:when test="contains(text(), '°') or contains(text(), 'geo:lat') or contains(text(), 'geo:lon') or contains(text(), ' N ') or contains(text(), ' S ') or contains(text(), ' E ') or contains(text(), ' W ')">
                    <!-- predicting minutes and seconds with ' or " might break if quotes used for other purposes exist in the text node -->
                    <subject>
                        <cartographics>
                            <coordinates>
                                <xsl:apply-templates/>
                            </coordinates>
                        </cartographics>
                    </subject>
                </xsl:when>
                <xsl:when test="contains(text(), ':')">
                    <xsl:if test="starts-with(text(), '1') and contains(text(), ':') and (contains(substring-after(text(), ':'), '1') or contains(substring-after(text(), ':'), '2') or contains(substring-after(text(), ':'), '3') or contains(substring-after(text(), ':'), '4') or contains(substring-after(text(), ':'), '5') or contains(substring-after(text(), ':'), '6') or contains(substring-after(text(), ':'), '7') or contains(substring-after(text(), ':'), '8') or contains(substring-after(text(), ':'), '9'))">
                        <subject>
                            <cartographics>
                                <scale>
                                    <xsl:apply-templates/>
                                </scale>
                            </cartographics>
                        </subject>
                    </xsl:if>
                </xsl:when>
                <xsl:when test="contains($projections, text())">
                    <subject>
                        <cartographics>
                            <projection>
                                <xsl:apply-templates/>
                            </projection>
                        </cartographics>
                    </subject>
                </xsl:when>
                <xsl:otherwise>
                    <subject>
                        <geographic>
                            <xsl:apply-templates/>
                        </geographic>
                    </subject>
                </xsl:otherwise>
            </xsl:choose>
    </xsl:template>
    <xsl:template match="dc:rights">
        <accessCondition>
            <xsl:apply-templates/>
        </accessCondition>
    </xsl:template>
</xsl:stylesheet>
