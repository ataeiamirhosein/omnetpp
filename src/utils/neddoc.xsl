<!--
==============================================================
 File: neddoc.xsl - part of OMNeT++
==============================================================

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  Copyright (C) 2002-2003 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output method="html" indent="yes" encoding="iso-8859-1"/>

<xsl:param name="outputdir" select="'html'"/>

<xsl:key name="module" match="//simple-module|//compound-module" use="@name"/>
<xsl:key name="channel" match="//channel" use="@name"/>

<!-- ROOT -->
<xsl:template match="/">
   <xsl:document href="{$outputdir}/index.html">
      <html>
         <head>
            <title>Model documentation -- generated from NED files</title>
         </head>
         <frameset cols="35%,65%">
            <frameset rows="60%,40%">
               <frame src="modules.html" name="componentsframe"/>
               <frame src="files.html" name="filesframe"/>
            </frameset>
            <frame src="overview.html" name="mainframe"/>
         </frameset>
         <noframes>
            <h2>frame alert</h2>
            <p>This document is designed to be viewed using HTML frames. If you see this message,
            you are using a non-frame-capable browser.</p>
         </noframes>
      </html>
   </xsl:document>

   <xsl:document href="{$outputdir}/style.css">
      body,td,p,ul,ol,li,h1,h2,h3,h4 {font-family:arial,sans-serif }
      body,td,p,ul,ol,li { font-size=10pt }
      h1 { font-size=18pt; text-align=center }
      pre.comment { font-size=10pt; padding-left=5pt }
      pre.src { font-size=8pt; background=#E0E0E0; padding-left=5pt }
      th { font-size=10pt; text-align=left; vertical-align=top; background=#E0E0f0 }
      td { font-size=10pt; text-align=left; vertical-align=top }
      .navbarlink { font-size=8pt; }
      .indextitle { font-size=12pt; }
      .comptitle  { font-size=14pt; }
      .subtitle   { font-size=12pt; margin-bottom: 3px}
      FIXME.paramtable { border=2px ridge; border-collapse=collapse;}
   </xsl:document>

   <xsl:document href="{$outputdir}/simplemodules.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <xsl:call-template name="create-simplemodule-index"/>
   </xsl:document>

   <xsl:document href="{$outputdir}/modules.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <xsl:call-template name="create-module-index"/>
   </xsl:document>

   <!--
   <xsl:document href="{$outputdir}/compoundmodules.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <xsl:call-template name="create-module-index"/>
   </xsl:document>
   -->

   <xsl:document href="{$outputdir}/channels.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <xsl:call-template name="create-channel-index"/>
   </xsl:document>

   <xsl:document href="{$outputdir}/networks.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <xsl:call-template name="create-network-index"/>
   </xsl:document>

   <xsl:document href="{$outputdir}/files.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <xsl:call-template name="create-fileindex"/>
   </xsl:document>

   <xsl:document href="{$outputdir}/overview.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <center><h1>OMNeT++ Model Documentation</h1></center>
      <center><i>Generated from NED files</i></center>
      <p>This documentation has been generated from NED files. Use the links in
      the left frames to navigate around.</p>
      <p>Generated by opp_neddoc.</p>
   </xsl:document>

   <xsl:apply-templates/>

</xsl:template>


<!-- INDEX PAGES -->

<xsl:template name="create-simplemodule-index">
   <p class="navbar">
      <span class="navbarlink">modules (simple)</span><xsl:text> - </xsl:text>
      <a href="modules.html" class="navbarlink">modules (all)</a><xsl:text> - </xsl:text>
      <a href="channels.html" class="navbarlink">channels</a><xsl:text> - </xsl:text>
      <a href="networks.html" class="navbarlink">networks</a>
   </p>
   <h3 class="indextitle">Simple Modules</h3>
   <ul>
      <xsl:for-each select="//simple-module">
         <xsl:sort select="@name"/>
         <li>
            <a href="{concat(@name,'-',generate-id(.))}.html" target="mainframe"><xsl:value-of select="@name"/></a>
            <!--
            <i> (<xsl:value-of select="ancestor::ned-file/@filename"/>)</i>
            -->
         </li>
      </xsl:for-each>
   </ul>
</xsl:template>

<xsl:template name="create-module-index">
   <p class="navbar">
      <a href="simplemodules.html" class="navbarlink">modules (simple)</a><xsl:text> - </xsl:text>
      <span class="navbarlink">modules (all)</span><xsl:text> - </xsl:text>
      <a href="channels.html" class="navbarlink">channels</a><xsl:text> - </xsl:text>
      <a href="networks.html" class="navbarlink">networks</a>
   </p>
   <h3 class="indextitle">All Modules</h3>
   <ul>
      <xsl:for-each select="//simple-module|//compound-module">
         <xsl:sort select="@name"/>
         <li>
            <a href="{concat(@name,'-',generate-id(.))}.html" target="mainframe"><xsl:value-of select="@name"/></a>
            <!--
            <i> (<xsl:value-of select="ancestor::ned-file/@filename"/>)</i>
            -->
         </li>
      </xsl:for-each>
   </ul>
</xsl:template>

<xsl:template name="create-channel-index">
   <p class="navbar">
      <a href="simplemodules.html" class="navbarlink">modules (simple)</a><xsl:text> - </xsl:text>
      <a href="modules.html" class="navbarlink">modules (all)</a><xsl:text> - </xsl:text>
      <span class="navbarlink">channels</span><xsl:text> - </xsl:text>
      <a href="networks.html" class="navbarlink">networks</a>
   </p>
   <h3 class="indextitle">Channels</h3>
   <ul>
      <xsl:for-each select="//channel">
         <xsl:sort select="@name"/>
         <li>
            <a href="{concat(@name,'-',generate-id(.))}.html" target="mainframe"><xsl:value-of select="@name"/></a>
            <!--
            <i> (<xsl:value-of select="ancestor::ned-file/@filename"/>)</i>
            -->
         </li>
      </xsl:for-each>
   </ul>
</xsl:template>

<xsl:template name="create-network-index">
   <p class="navbar">
      <a href="simplemodules.html" class="navbarlink">modules (simple)</a><xsl:text> - </xsl:text>
      <a href="modules.html" class="navbarlink">modules (all)</a><xsl:text> - </xsl:text>
      <a href="channels.html" class="navbarlink">channels</a><xsl:text> - </xsl:text>
      <span class="navbarlink">networks</span>
   </p>
   <h3 class="indextitle">Networks</h3>
   <ul>
      <xsl:for-each select="//network">
         <xsl:sort select="@name"/>
         <li>
            <a href="{concat(@name,'-',generate-id(.))}.html" target="mainframe"><xsl:value-of select="@name"/></a>
            <!--
            <i> (<xsl:value-of select="ancestor::ned-file/@filename"/>)</i>
            -->
         </li>
      </xsl:for-each>
   </ul>
</xsl:template>

<xsl:template name="create-fileindex">
   <h3 class="indextitle">NED Files</h3>
   <ul>
      <xsl:for-each select="//ned-file">
         <xsl:sort select="@filename"/>
         <li>
            <a href="{concat('file-',generate-id(.))}.html" target="mainframe"><xsl:value-of select="@filename"/></a>
         </li>
      </xsl:for-each>
   </ul>
</xsl:template>


<!-- COMPONENT PAGES -->

<xsl:template match="channel">
   <xsl:document href="{$outputdir}/{concat(@name,'-',generate-id(.))}.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <h2 class="comptitle">Channel <i><xsl:value-of select="@name"/></i></h2>
      <xsl:call-template name="print-file"/>
      <xsl:call-template name="process-comment"/>
      <xsl:call-template name="print-attrs"/>
      <xsl:call-template name="print-channel-used-in"/>
      <xsl:call-template name="print-source"/>
   </xsl:document>
</xsl:template>

<xsl:template match="simple-module">
   <xsl:document href="{$outputdir}/{concat(@name,'-',generate-id(.))}.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <h2 class="comptitle">Simple Module <i><xsl:value-of select="@name"/></i></h2>
      <xsl:call-template name="print-file"/>
      <xsl:call-template name="process-comment"/>
      <xsl:call-template name="print-params"/>
      <xsl:call-template name="print-gates"/>
      <xsl:call-template name="print-module-used-in"/>
      <xsl:call-template name="print-source"/>
   </xsl:document>
</xsl:template>

<xsl:template match="compound-module">
   <xsl:document href="{$outputdir}/{concat(@name,'-',generate-id(.))}.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <h2 class="comptitle">Compound Module <i><xsl:value-of select="@name"/></i></h2>
      <xsl:call-template name="print-file"/>
      <xsl:call-template name="process-comment"/>
      <xsl:call-template name="print-params"/>
      <xsl:call-template name="print-gates"/>
      <xsl:call-template name="print-uses"/>
      <xsl:call-template name="print-module-used-in"/>
      <xsl:call-template name="print-source"/>
   </xsl:document>
</xsl:template>

<xsl:template match="network">
   <xsl:document href="{$outputdir}/{concat(@name,'-',generate-id(.))}.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <h2 class="comptitle">Network <i><xsl:value-of select="@name"/></i></h2>
      <xsl:call-template name="print-file"/>
      <xsl:call-template name="process-comment"/>
      <xsl:call-template name="print-type"/>
      <xsl:call-template name="print-substparams"/>
      <xsl:call-template name="print-source"/>
   </xsl:document>
</xsl:template>

<xsl:template match="ned-file">
   <xsl:document href="{$outputdir}/{concat('file-',generate-id(.))}.html" method="html">
      <xsl:call-template name="print-html-header"/>
      <h2 class="comptitle">File <i><xsl:value-of select="@filename"/></i></h2>
      <xsl:call-template name="process-comment"/>
      <h3 class="subtitle">Contains:</h3>
      <ul>
         <xsl:for-each select="simple-module|compound-module|channel|network">
            <xsl:sort select="@name"/>
            <li>
               <a href="{concat(@name,'-',generate-id(.))}.html"><xsl:value-of select="@name"/></a>
               <i> (<xsl:value-of select="local-name()"/>)</i>
            </li>
         </xsl:for-each>
      </ul>
   </xsl:document>
   <xsl:apply-templates/>
</xsl:template>


<!-- HELPER TEMPLATES -->

<xsl:template name="print-html-header">
   <head>
      <link rel="stylesheet" type="text/css" href="style.css" />
   </head>
</xsl:template>

<xsl:template name="print-file">
   <xsl:if test="ancestor::ned-file/@filename">
      <b>File: <a href="{concat('file-',generate-id(ancestor::ned-file))}.html"><xsl:value-of select="ancestor::ned-file/@filename"/></a></b>
   </xsl:if>
</xsl:template>

<xsl:template name="print-params">
   <xsl:if test="params/param">
      <h3 class="subtitle">Parameters:</h3>
      <table class="paramtable">
         <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Description</th>
         </tr>
         <xsl:for-each select="params/param">
            <xsl:sort select="@name"/>
            <tr>
               <td width="150"><xsl:value-of select="@name"/></td>
               <td width="100">
                  <i>
                  <xsl:choose>
                     <xsl:when test="@data-type"><xsl:value-of select="@data-type"/></xsl:when>
                     <xsl:otherwise>numeric</xsl:otherwise>
                  </xsl:choose>
                  </i>
               </td>
               <td><xsl:call-template name="process-tablecomment"/></td>
            </tr>
         </xsl:for-each>
      </table>
   </xsl:if>
</xsl:template>

<xsl:template name="print-gates">
   <xsl:if test="gates/gate">
      <h3 class="subtitle">Gates:</h3>
      <table class="paramtable">
         <tr>
            <th>Name</th>
            <th>Direction</th>
            <th>Description</th>
         </tr>
         <xsl:for-each select="gates/gate">
            <xsl:sort select="@name"/>
            <tr>
               <td width="150"><xsl:value-of select="@name"/><xsl:if test="@is-vector='yes'"> [ ]</xsl:if></td>
               <td width="100"><i><xsl:value-of select="@direction"/></i></td>
               <td><xsl:call-template name="process-tablecomment"/></td>
            </tr>
         </xsl:for-each>
      </table>
   </xsl:if>
</xsl:template>

<xsl:template name="print-attrs">
   <xsl:if test="channel-attr">
      <h3 class="subtitle">Attributes:</h3>
      <table class="paramtable">
         <tr>
            <th>Name</th>
            <th>Value</th>
            <th>Description</th>
         </tr>
         <xsl:for-each select="channel-attr">
            <xsl:sort select="@name"/>
            <tr>
               <td width="150"><xsl:value-of select="@name"/></td>
               <td width="300"><i><xsl:value-of select="@value"/></i></td>
               <td><xsl:call-template name="process-tablecomment"/></td>
            </tr>
         </xsl:for-each>
      </table>
   </xsl:if>
</xsl:template>

<xsl:template name="print-substparams">
   <xsl:if test="substparams/substparam">
      <h3 class="subtitle">Parameter values:</h3>
      <table class="paramtable">
         <tr>
            <th>Name</th>
            <th>Value</th>
            <th>Comment</th>
         </tr>
         <xsl:for-each select="substparams/substparam">
            <xsl:sort select="@name"/>
            <tr>
               <td width="150"><xsl:value-of select="@name"/></td>
               <td width="300"><i><xsl:value-of select="@value"/></i></td>
               <td><xsl:call-template name="process-tablecomment"/></td>
            </tr>
         </xsl:for-each>
      </table>
   </xsl:if>
</xsl:template>

<xsl:template name="print-uses">
   <xsl:if test="key('module',.//submodule/@type-name)">
      <h3 class="subtitle">Builds on modules:</h3>
      <table>
        <xsl:for-each select="key('module',.//submodule/@type-name)">
           <xsl:sort select="@name"/>
           <xsl:call-template name="print-componentref"/>
        </xsl:for-each>
      </table>
   </xsl:if>
   <xsl:if test="key('channel',.//conn-attr[@name='channel']/@value)">
      <h3 class="subtitle">Builds on channels:</h3>
      <table>
         <xsl:for-each select="key('channel',.//conn-attr[@name='channel']/@value)">
            <xsl:sort select="@name"/>
            <xsl:call-template name="print-componentref"/>
         </xsl:for-each>
      </table>
   </xsl:if>
</xsl:template>

<xsl:template name="print-module-used-in">
   <xsl:variable name="name" select="@name"/>
   <xsl:if test="//compound-module[.//submodule[@type-name=$name]]">
      <h3 class="subtitle">Used in compound modules:</h3>
      <table>
         <xsl:for-each select="//compound-module[.//submodule[@type-name=$name]]">
            <xsl:call-template name="print-componentref"/>
         </xsl:for-each>
      </table>
   </xsl:if>
   <xsl:if test="//network[@type-name=$name]">
      <h3 class="subtitle">Networks:</h3>
      <table>
         <xsl:for-each select="//network[@type-name=$name]">
            <xsl:call-template name="print-componentref"/>
         </xsl:for-each>
      </table>
   </xsl:if>
</xsl:template>

<xsl:template name="print-channel-used-in">
   <xsl:variable name="name" select="@name"/>
   <xsl:if test="//compound-module[.//conn-attr[@name='channel' and @value=$name]]">
      <h3 class="subtitle">Used in compound modules:</h3>
      <table>
         <xsl:for-each select="//compound-module[.//conn-attr[@name='channel' and @value=$name]]">
            <xsl:call-template name="print-componentref"/>
         </xsl:for-each>
      </table>
   </xsl:if>
</xsl:template>

<xsl:template name="print-type">
   <xsl:if test="key('module',@type-name)">
      <h3 class="subtitle">Instance of:</h3>
      <table>
         <xsl:for-each select="key('module',@type-name)">
            <xsl:sort select="@name"/>
            <xsl:call-template name="print-componentref"/>
         </xsl:for-each>
      </table>
   </xsl:if>
</xsl:template>

<xsl:template name="print-componentref">
   <tr>
      <td>
         <a href="{concat(@name,'-',generate-id(.))}.html"><xsl:value-of select="@name"/></a>
      </td>
      <td>
         <xsl:choose>
            <xsl:when test="@banner-comment">
               <span class="briefcomment"><xsl:value-of select="@banner-comment"/></span>
            </xsl:when>
            <xsl:otherwise>(no description)</xsl:otherwise>
         </xsl:choose>
      </td>
   </tr>
</xsl:template>

<xsl:template name="print-source">
   <xsl:if test="@source-code">
      <h3 class="subtitle">Source code:</h3>
      <pre class="src"><xsl:value-of select="@source-code"/></pre>
   </xsl:if>
</xsl:template>


<xsl:template name="process-tablecomment">
   <xsl:param name="comment" select="@banner-comment|@right-comment"/>
   <xsl:choose>
      <xsl:when test="$comment">
         <!-- <pre class="comment"><xsl:value-of select="$comment"/></pre> -->
         <span class="comment"><xsl:value-of select="$comment"/></span>
      </xsl:when>
      <xsl:otherwise> </xsl:otherwise>
   </xsl:choose>
</xsl:template>

<xsl:template name="process-comment">
   <xsl:param name="comment" select="@banner-comment"/>
   <xsl:choose>
      <xsl:when test="$comment">
         <pre class="comment"><xsl:value-of select="$comment"/></pre>
      </xsl:when>
      <xsl:otherwise><p>(no description)</p></xsl:otherwise>
   </xsl:choose>
</xsl:template>

</xsl:stylesheet>


