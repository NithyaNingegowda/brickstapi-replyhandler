<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--  from http://www.dpawson.co.uk/xsl/sect2/padding.html#d8226e109 -->
<xsl:template name="append-pad">    
  <!-- recursive template to left justify and append  -->
  <!-- the value with whatever padChar is passed in   -->
    <xsl:param name="padChar"> </xsl:param>
    <xsl:param name="padVar"/>
    <xsl:param name="length"/>
    <xsl:choose>
      <xsl:when test="string-length($padVar) &lt; $length">
        <xsl:call-template name="append-pad">
          <xsl:with-param name="padChar" select="$padChar"/>
          <xsl:with-param name="padVar" select="concat($padVar,$padChar)"/>
          <xsl:with-param name="length" select="$length"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="substring($padVar,1,$length)"/>
      </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template match="/">
<soapenv:Envelope
	xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
	xmlns:xsd="http://www.w3.org/2001/XMLSchema"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<soapenv:Header>
		<OpHdr>
			<OpHdrVersNum>1.2</OpHdrVersNum>
			<OpDefCnt>1</OpDefCnt>
			<IgnrErrFlag>Y</IgnrErrFlag>
			<OpDefHdr seq="1">
				<SvceId>SMSBankMobileV11</SvceId>
				<OpId>TBSMSREQ</OpId>
				<SvceVersNum>1.0</SvceVersNum>
				<PayloadType>XML</PayloadType>
			</OpDefHdr>
		</OpHdr>
		<ISMHdr>
			<ISMHdrVersNum>1.2</ISMHdrVersNum>
			<AppName>PHHSBCOHRD</AppName>
			<ClntId>PHU1IRIS</ClntId>
			<ClntHostId>PHU1IRIS</ClntHostId>
			<MsgInstcId>1</MsgInstcId>
			<InbndChanlId>IRIS</InbndChanlId>
			<MsgCreatTmsp><xsl:value-of select="/smpp/headertimestamp"/></MsgCreatTmsp>
		</ISMHdr>
	</soapenv:Header>
	<soapenv:Body>
		<RqstPayload>
			<OpDefRqst seq="1">01<xsl:value-of select="/smpp/source/ton"/><xsl:value-of select="/smpp/source/npi"/>
			<xsl:call-template name="append-pad">
			  <xsl:with-param name="padVar" select="/smpp/source/address"/>
			  <xsl:with-param name="padChar" select="' '"/>
			  <xsl:with-param name="length" select="20"/>
			</xsl:call-template>
			<xsl:value-of select="/smpp/destination/ton"/><xsl:value-of select="/smpp/destination/npi"/>
			<xsl:call-template name="append-pad">
			  <xsl:with-param name="padVar" select="/smpp/destination/address"/>
			  <xsl:with-param name="padChar" select="' '"/>
			  <xsl:with-param name="length" select="20"/>
			</xsl:call-template>
			<xsl:call-template name="append-pad">
			  <xsl:with-param name="padVar" select="/smpp/messageid"/>
			  <xsl:with-param name="padChar" select="' '"/>
			  <xsl:with-param name="length" select="65"/>
			</xsl:call-template>
			<xsl:value-of select="/smpp/payloadtimestamp"/>
			<xsl:call-template name="append-pad">
			  <xsl:with-param name="padVar" select="/smpp/messageBase64"/>
			  <xsl:with-param name="padChar" select="' '"/>
			  <xsl:with-param name="length" select="216"/>
			</xsl:call-template>
			</OpDefRqst>
		</RqstPayload>
	</soapenv:Body>
</soapenv:Envelope>
</xsl:template>
</xsl:stylesheet>
