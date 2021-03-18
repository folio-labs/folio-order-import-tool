package org.olf.folio.order.util;

import org.apache.commons.lang3.text.WordUtils;
import org.marc4j.marc.DataField;

public class MarcUtils {

	public MarcUtils() {
		// TODO Auto-generated constructor stub
	}
	
	public String getTitle(DataField twoFourFive) {
		String title = new String();
		if (twoFourFive != null) {
		    title = twoFourFive.getSubfieldsAsString("a");
		    String titleTwo = twoFourFive.getSubfieldsAsString("b");
	        String titleThree = twoFourFive.getSubfieldsAsString("c");
	        if (titleTwo != null) title += " " + titleTwo;
	        if (titleThree != null) title += " " + titleThree;
		}
	    return title;
	}
	
	public String getPrice(DataField nineEighty, DataField nineEightyOne) {
		String price = new String();
		if (nineEighty != null) {
		    price = nineEighty.getSubfieldsAsString("m");
	        if (price == null ) {
	    	    price = nineEightyOne.getSubfieldsAsString("i");	
	        }
		}
	    return price;
	}
	
	public String getQuantiy(DataField nineEighty ) {
		String quantity = new String();
		if (nineEighty != null) {
	        quantity =  nineEighty.getSubfieldsAsString("q");
	        if (quantity == null) {
	    	    quantity =  nineEighty.getSubfieldsAsString("g");	
	        }
		}
	    return quantity;
	}
	
	public String getFundCode(DataField nineEighty ) {
		String fundCode = new String();
		if (nineEighty != null) {
	        fundCode = nineEighty.getSubfieldsAsString("b");
	        if (fundCode == null) {
	    	    fundCode = nineEighty.getSubfieldsAsString("h");	
	        }
		}
	    return fundCode;
	}
	
	public String getVendorCode(DataField nineEighty ) {
		String vendorCode = new String();
		if (nineEighty != null) {
		    vendorCode = nineEighty.getSubfieldsAsString("v");
		}
		return vendorCode;
	}
	
	public String getVendorItemId(DataField nineEighty ) {
		String vendorItemId= new String();
		if (nineEighty != null) {
		    vendorItemId = nineEighty.getSubfieldsAsString("c");
		}
		return vendorItemId;
	}
	
	public String getLocation(DataField nineFiveTwo ) {
		String location = new String();
	    if (nineFiveTwo != null) {
	       location = nineFiveTwo.getSubfieldsAsString("b");
	       location = WordUtils.capitalize(location.toLowerCase());
	    } else {
	        location = "Olin";
	    }
	    return location;
	}
	
	public String getRequestor(DataField nineEightyOne ) {
		String requestor = new String();
	    if (nineEightyOne != null) {
	    	requestor = nineEightyOne.getSubfieldsAsString("r");
	    } 
	    return requestor;
	}
	
	public String getNotes(DataField nineEighty ) {
		String notes = new String();
		if (nineEighty != null) {
		    notes =  nineEighty.getSubfieldsAsString("n");
		}
		return notes;
	}
	
	public String getEmail(DataField nineEighty ) {
		String email = new String();
		if (nineEighty != null) {
		    email = nineEighty.getSubfieldsAsString("y");
		}
		return email;
	}
	
	public String getPersonName(DataField nineEighty ) {
		String personName = new String();
		if (nineEighty != null) {
		    personName = nineEighty.getSubfieldsAsString("s");
		}
		return personName;
	}
	
	public String getElectronicIndicator(DataField nineEighty ) {
		String electronicIndicator = new String();
		if (nineEighty != null) {
			electronicIndicator = nineEighty.getSubfieldsAsString("z");
		}
		return electronicIndicator;
	}
	

}
