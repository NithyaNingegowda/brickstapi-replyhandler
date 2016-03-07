package com.kana.connect.server.receiver;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import com.brickstreet.smpp.protocol.message.SMPPRequest;
import com.brickstreet.smpp.protocol.util.SMPPIO;
import com.kana.connect.server.smpp.message.DeliverSM;
import com.kana.connect.server.smpp.util.DefaultAlphabetEncoding;

public class TestSmppReceiverContent
{
    @Test
    public void testSoapMessage() throws IOException, TransformerException
    {
        // 
        // Build an SMPP 3.4 deliver_sm PDU from raw bytes
        //
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // write header
        SMPPIO.writeInt(0, 4, baos);    // pdu length; fill this in later
        SMPPIO.writeInt(5, 4, baos);    // deliver_sm = 5
        SMPPIO.writeInt(0, 4, baos);    // command_status null
        SMPPIO.writeInt(1, 4, baos);    // sequence_number
        
        // mandatory params
        
        SMPPIO.writeCString("", baos);  // null-terminated string, len = 1
        
        // src addr ton
        SMPPIO.writeInt(0, 1, baos);
        // src addr npi
        SMPPIO.writeInt(0, 1, baos);
        // src addr
        SMPPIO.writeCString("14155551212", baos); // null-terminated octet string
        
        // dst addr ton
        SMPPIO.writeInt(0, 1, baos);
        // dst addr npi
        SMPPIO.writeInt(0, 1, baos);
        // dst addr
        SMPPIO.writeCString("16035551212", baos); // null-terminated octet string
        
        // esm_class
        SMPPIO.writeInt(0, 1, baos);
        // protocol id
        SMPPIO.writeInt(0, 1, baos);
        // priority flag
        SMPPIO.writeInt(0, 1, baos);
        // schedule delivery time
        SMPPIO.writeInt(0, 1, baos);
        // validity period, must be null
        SMPPIO.writeInt(0, 1, baos);
        // registered delivery
        SMPPIO.writeInt(0, 1, baos);
        // replace if present flag; must be null
        SMPPIO.writeInt(0, 1, baos);
        // data coding; 0 = default alphabet
        SMPPIO.writeInt(0, 1, baos);
        // sm_default_msg_id; must be null
        SMPPIO.writeInt(0, 1, baos);

        // encode message
        DefaultAlphabetEncoding smppEncoding = new DefaultAlphabetEncoding();
        byte[] msg = smppEncoding.encodeString("Test Msg 1"); 
        // write message length
        SMPPIO.writeInt(msg.length, 1, baos);
        // write message
        baos.write(msg);
        
        // get PDU byte arracy
        byte[] pdu = baos.toByteArray();
        
        // update PDU length at front of byte array
        int pdu_len = pdu.length;
        byte[] pdu_len_bytes = SMPPIO.intToBytes(pdu_len, 4);
        pdu[0] = pdu_len_bytes[0];
        pdu[1] = pdu_len_bytes[1];
        pdu[2] = pdu_len_bytes[2];
        pdu[3] = pdu_len_bytes[3];
              
        DeliverSM deliverSM = new DeliverSM();
        deliverSM.readFrom(pdu, 0);
                
        SmppReceiverMessage srm = new SmppReceiverMessage(deliverSM, 1, 1);
        String xml = SMSKeywordDispatchReplyHandler.smppToXml(srm);
        System.out.println(xml);
        
        // pass xml to xsl
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        // fetch xsl content
        InputStream xslStream = new FileInputStream("soap1.xsl");
        Templates templ = null;
        try {
            StreamSource ssource = new StreamSource(xslStream);
            templ = transformerFactory.newTemplates(ssource);
        } finally {
            if (xslStream != null) {
                try {
                    xslStream.close();
                } catch (Exception x) { /* dont care */
                }
            }
        }

        Transformer cloneTrans = templ.newTransformer();

        // create source and result objects
        StreamSource ssource = new StreamSource(new StringReader(xml));
        StringWriter swriter = new StringWriter();
        StreamResult sresult = new StreamResult(swriter);

        cloneTrans.transform(ssource, sresult);
        String result = swriter.toString();

        System.out.println(result);
    }


}
