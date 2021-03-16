package org.olf.folio.order;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.junit.jupiter.api.Disabled; 

 
public class MarcReaderTest {
    
	 
	@Test
	public void test1() throws FileNotFoundException {

		String filePath = (String) "/cul/src/order-import-poc/marc-test-files/building_bridges.mrc";
		FileInputStream in = new FileInputStream(filePath);	 
		MarcReader reader = new MarcStreamReader(in);
		Record record = null;
		while (reader.hasNext()) {
			record = reader.next();
			DataField twoFourFive = (DataField) record.getVariableField("245");
			String title = twoFourFive.getSubfieldsAsString("a");
			DataField nineEighty = (DataField) record.getVariableField("980");
			String fundCode = nineEighty.getSubfieldsAsString("b");
			String vendorCode =  nineEighty.getSubfieldsAsString("v");
			
			System.out.println("vendorCode: "+ vendorCode);
			System.out.println("fundCode: "+ fundCode);
		
		}

	}


}
