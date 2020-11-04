<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:r="http://www.interactive-instruments.de/ShapeChange/Result">
  <!-- (c) 2012 interactive instruments GmbH, Bonn -->
 <!-- NOTE: Use XML attribute 'encoding' to change the output encoding. Example: encoding="iso-8859-1" -->
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="/">
    <html>
      <head>
        <title>ShapeChange result</title>
		<style type="text/css">
body
{
background-color:#f4f6fe;
}
h1
{
font-family:Arial, Helvetica, sans-serif;
font-size:24px;
color:#151B8D;
text-align:center;
}
h2, h3, h4
{
font-family:Arial, Helvetica, sans-serif;
color:#151B8D;
text-align:center;
}
a:link 
{
color:#325595;
text-decoration:none;
border-bottom:1px dotted; 
outline: none;
}
a:hover
{
border-bottom:1px solid;
color:#325595;
}
p, li
{
font-family:Arial, Helvetica, sans-serif;
font-size:14px;
margin-top: 0px;
margin-bottom: 4px;
}
.small
{
font-size:10px;
}
table
{
font-family:Arial, Helvetica, sans-serif;
border-style:none;
border-collapse:collapse;
border:0px;
padding:0px;
margin-left: auto;
margin-right: auto;
}
tr
{
vertical-align:top;
}
td
{
border:1px solid #98bf21;
padding:5px 20px 5px 20px;
}
</style>
<script type="text/javascript" language="JavaScript">
function toggleMe(a)
{
var list = document.getElementsByClassName(a);

if(!list)return true;

for (var i=0; i&lt;list.length;i++) {
  if(list[i].style.display=="none")
  {
  list[i].style.display="table-row"
  }
  else
  {
  list[i].style.display="none"
  }
}

return true;
}
</script>

	</head>
    <body>
      <h1>ShapeChange result</h1>
      <table>
      		<xsl:if test="//@resultCode">
      			<tr>
      			<td><p>Result:</p></td>
      			<td><p><xsl:choose><xsl:when test="//@resultCode=0">Conversion Completed</xsl:when><xsl:otherwise>Aborted due to fatal error</xsl:otherwise></xsl:choose></p></td>
		      	</tr>
      		</xsl:if>
      		<xsl:if test="//@start">
      			<tr>
      			<td><p>Start time:</p></td>
      			<td><p><xsl:value-of select="//@start"/></p></td>
		      	</tr>
      		</xsl:if>
      		<xsl:if test="//@end">
      			<tr>
      			<td><p>End time:</p></td>
      			<td><p><xsl:value-of select="//@end"/></p></td>
		      	</tr>
      		</xsl:if>
      		<xsl:if test="//@config">
      			<tr>
      			<td><p>Configuration file:</p></td>
      			<td><p><xsl:value-of select="//@config"/></p></td>
		      	</tr>
      		</xsl:if>
      		<xsl:if test="//@version">
      			<tr>
      			<td><p>ShapeChange version:</p></td>
      			<td><p><xsl:value-of select="//@version"/></p></td>
		      	</tr>
      		</xsl:if>
      </table>
    <a>
      <xsl:attribute name="name">messages</xsl:attribute>
      <h2>Messages</h2>
    </a>
    <xsl:choose>
     <xsl:when test="//r:FatalError|//r:Error|//r:Warning|//r:Info|//r:Debug|//r:ProcessFlowFatalError|//r:ProcessFlowError|//r:ProcessFlowWarning|//r:ProcessFlowInfo|//r:ProcessFlowDebug">
		<div id="menu">
		 <p align="center">Click on the following buttons to toggle log messages of the according category.</p>
		 <table>
		  <tbody>
		   <tr>
		    <th>Process Flow</th>
		    <th>Process Execution</th>		    
		   </tr>
		   <tr>
		    <td><a onclick="return toggleMe('ProcessFlowDebug')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Debug</a></td>
		    <td><a onclick="return toggleMe('Debug')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Debug</a></td>
		   </tr>
		   <tr>
		    <td><a onclick="return toggleMe('ProcessFlowInfo')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Info</a></td>
		    <td><a onclick="return toggleMe('Info')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Info</a></td>
		    </tr>
		   <tr>
		    <td><a onclick="return toggleMe('ProcessFlowWarning')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Warning</a></td>
		    <td><a onclick="return toggleMe('Warning')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Warning</a></td>
		   </tr>
		   <tr>
		    <td><a onclick="return toggleMe('ProcessFlowError')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Error</a></td>
		    <td><a onclick="return toggleMe('Error')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Error</a></td>
		   </tr>
		   <tr>
		    <td><a onclick="return toggleMe('ProcessFlowFatalError')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Fatal Error</a></td>
		    <td><a onclick="return toggleMe('FatalError')" href="javascript:void(0)"><img src="http://shapechange.net/resources/images/open.gif" width="10" height="10" border="0" alt=""/><xsl:text> </xsl:text>Fatal Error</a></td>
		   </tr>
		  </tbody>
		 </table>
			<br/>
		</div>
	    <table border="0">
		  <tr>
	  		<th><p>Severity</p></th>
		  	<th><p>Message</p></th>
<!--			<th><p>Source</p></th>-->
		  </tr>    
	     <xsl:for-each select="//r:FatalError|//r:Error|//r:Warning|//r:Info|//r:Debug|//r:ProcessFlowFatalError|//r:ProcessFlowError|//r:ProcessFlowWarning|//r:ProcessFlowInfo|//r:ProcessFlowDebug">
	        <xsl:apply-templates select="."/>
	      </xsl:for-each>
	    </table>
    </xsl:when>
    <xsl:otherwise>
    	<p align="center">No messages</p>
    </xsl:otherwise>
    </xsl:choose>
    <a>
      <xsl:attribute name="name">results</xsl:attribute>
      <h2>Results</h2>
    </a>
    <xsl:choose>
    <xsl:when test="//r:Result">
	    <table border="0">
		  <tr>
	  		<th><p>Target</p></th>
	  		<th><p>Scope</p></th>
		  	<th><p>File</p></th>
		  </tr>    
	      <xsl:for-each select="//r:Result">
	      	<xsl:sort select="@target"/>
	      	<xsl:sort select="@scope"/>
	        <xsl:apply-templates select="."/>
	      </xsl:for-each>
	    </table>
    </xsl:when>
    <xsl:otherwise>
    	<p align="center">No results</p>
    </xsl:otherwise>
    </xsl:choose>
    <hr/>
    <p align="center">
      <small>This report was generated by <a href="http://shapechange.net">ShapeChange</a></small>
    </p>
  </body>
</html>
</xsl:template>
 <xsl:template match="r:FatalError|r:Error|r:Warning|r:Info|r:Debug|//r:ProcessFlowFatalError|//r:ProcessFlowError|//r:ProcessFlowWarning|//r:ProcessFlowInfo|//r:ProcessFlowDebug">
  <tr class="{name(.)}" style="display:table-row">
  	<td><p><xsl:value-of select="name(.)"/></p></td>
  	<td><p><xsl:value-of select="@message"/></p><xsl:for-each select="*/@message"><p><small><xsl:call-template name="replace_ins"><xsl:with-param name="string" select="."/></xsl:call-template></small></p></xsl:for-each></td>
<!--	<td><p><xsl:value-of select="@source"/></p></td>-->
  </tr>
 </xsl:template>
<xsl:template match="r:Result">
  <tr>
  	<td><p><xsl:value-of select="@target"/></p></td>
  	<td><p><xsl:value-of select="@scope"/></p></td>
  	<td><p><a href="{@href}"><xsl:value-of select="."/></a></p></td>
  </tr>
</xsl:template>
<xsl:template name="replace_ins">
    <xsl:param name="string"/>
    <xsl:choose>
        <xsl:when test="contains($string,'[[ins]]')">
		    <xsl:call-template name="replace_del">
		        <xsl:with-param name="string" select="substring-before($string,'[[ins]]')"/>
		    </xsl:call-template>
            <ins style="background:#e6ffe6;">
                <xsl:value-of select="substring-before(substring-after($string,'[[ins]]'),'[[/ins]]')"/>
            </ins>
            <xsl:call-template name="replace_ins">
                <xsl:with-param name="string" select="substring-after($string,'[[/ins]]')"/>
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
		    <xsl:call-template name="replace_del">
		        <xsl:with-param name="string" select="$string"/>
		    </xsl:call-template>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>
<xsl:template name="replace_del">
    <xsl:param name="string"/>
    <xsl:choose>
        <xsl:when test="contains($string,'[[del]]')">
            <xsl:value-of select="substring-before($string,'[[del]]')"/>
            <del style="background:#ffe6e6;">
                <xsl:value-of select="substring-before(substring-after($string,'[[del]]'),'[[/del]]')"/>
            </del>
            <xsl:call-template name="replace_del">
                <xsl:with-param name="string" select="substring-after($string,'[[/del]]')"/>
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$string"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>
</xsl:stylesheet>