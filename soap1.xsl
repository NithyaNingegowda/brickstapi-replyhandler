<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
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
			<OpDefRqst seq="1">01<xsl:value-of select="/smpp/source/ton"/><xsl:value-of select="/smpp/source/npi"/><xsl:value-of select="/smpp/source/address"/><xsl:value-of select="/smpp/destination/ton"/><xsl:value-of select="/smpp/destination/npi"/><xsl:value-of select="/smpp/destination/address"/><xsl:value-of select="/smpp/messageid"/><xsl:value-of select="/smpp/payloadtimestamp"/><xsl:value-of select="/smpp/messageBase64"/></OpDefRqst>
		</RqstPayload>
	</soapenv:Body>
</soapenv:Envelope>
</xsl:template>
</xsl:stylesheet>
