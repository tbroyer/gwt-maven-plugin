<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="moduleShortName" />
<xsl:param name="inherits"/>

<xsl:output indent="yes"/>

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="/module">
  <xsl:copy>
    <xsl:if test="$moduleShortName">
      <xsl:attribute name="rename-to">
        <xsl:value-of select="$moduleShortName"/>
      </xsl:attribute>
    </xsl:if>

    <!-- inherit com.google.gwt.core.Core at a bare minimum. -->
    <inherits name="com.google.gwt.core.Core"/>

    <xsl:for-each select="$inherits">
      <xsl:copy />
    </xsl:for-each>

    <xsl:apply-templates select="@*|node()"/>

    <xsl:if test="not(source | super-source)">
      <source path="client"/>
      <source path="shared"/>
      <super-source path="super"/>
    </xsl:if>
  </xsl:copy>
</xsl:template>

<xsl:template match="/module/@rename-to">
  <xsl:if test="not($moduleShortName)">
    <xsl:copy/>
  </xsl:if>
</xsl:template>
</xsl:stylesheet>