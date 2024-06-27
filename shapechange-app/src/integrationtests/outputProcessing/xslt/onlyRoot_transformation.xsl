<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/*">
		<xsl:copy>
			<xsl:copy-of select="@*" />
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>