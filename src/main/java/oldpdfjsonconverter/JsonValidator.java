package oldpdfjsonconverter;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonValidator {
	private static final String INPUT_FOLDER_PATH = "D:\\testnewfilesoutput";
	
	public static void main(String[] args) throws Exception {
		File folder = new File(INPUT_FOLDER_PATH);
       
		if (!folder.exists() || !folder.isDirectory()) {
            //System.out.println("Input folder does not exist: " + inputFolderPath);
            return;
        }

		System.out.println("Json Validator Started");
        
		ArrayList<String> errorsList = new ArrayList<>();
		
		Integer processedFilesCount = 0;
		for (File file : folder.listFiles()) {
//        	System.out.println("Processing file :" + file.getName());

        	String jsonString = new String(Files.readAllBytes(file.toPath()), "UTF-8");
        	
        	JSONObject data = new JSONObject(jsonString);
            
            String beNumber = data.getString("BE_NUMBER");
            String beDate = data.getString("BE_DATE");
            
            if (beNumber == null || beNumber.isEmpty()) {
            	String errorMessage = file.getName() + " - BE Number is empty";
            	errorsList.add(errorMessage);
            }
            
            if (beDate == null || beDate.isEmpty()) {
            	String errorMessage = file.getName() + " - BE Date is empty";
            	errorsList.add(errorMessage);
            }
            
            JSONArray items = data.getJSONArray("ITEMS");
            
            for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
            	JSONObject item = items.getJSONObject(itemIndex);
            	
                String itemSerialNo = item.optString("ITEM_SERIAL_NO");
                String description = item.optString("DESCRIPTION");
                String uqc = item.optString("UQC");
                String concatenateDescription = item.optString("CONCATENATE_DESCRIPTION");
                String quantity = item.optString("QUANTITY");
                String partCode = item.optString("PART_CODE");

                String prefix = file.getName() + " - Item Serial No " + (itemIndex + 1) + " : ";

                if (itemSerialNo == null || itemSerialNo.isEmpty()) {
                    errorsList.add(prefix + "Item Serial No is empty");
                } else if (! itemSerialNo.equals((itemIndex + 1) + "")) {
                    errorsList.add(prefix + "Item Serial No mismatch expected " + (itemIndex + 1) + " but actual " + itemSerialNo);
                }

                if (description == null || description.isEmpty()) {
                    errorsList.add(prefix + "Description is empty");
                } else if (description.length() > 100) {
                    errorsList.add(prefix + "Description too large " + description);
                }
                
                if (uqc == null || uqc.isEmpty()) {
                    errorsList.add(prefix + "UQC is empty");
                } else {
                	if (!uqc.equals("NOS") &&
            	        !uqc.equals("KGS") &&
            	        !uqc.equals("MTR") && 
            	        !uqc.equals("PCS")) {
            	        errorsList.add(prefix + "UQC is invalid. UQC - " + uqc);
            	    }
                }

                if (concatenateDescription == null || concatenateDescription.isEmpty()) {
                    errorsList.add(prefix + "Concatenate Description is empty");
                }

                if (quantity == null || quantity.isEmpty()) {
                    errorsList.add(prefix + "Quantity is empty");
                } else {
                	String normalizedQty = quantity.trim();

            	    try {
            	        Integer.parseInt(quantity);
            	    } catch (NumberFormatException e) {
            	        errorsList.add(prefix + "Quantity is not a valid number -" + quantity);
            	    }
                }

                if (partCode == null || partCode.isEmpty()) {
                    errorsList.add(prefix + "Part Code is empty");
                } else {
                	String normalizedPartCode = partCode.trim();

                    if (normalizedPartCode.length() != 12) {
                    	if (! partCode.matches("^\\d{7}-\\d{2}#HQ$")) {
                    	   errorsList.add(prefix + "Part Code length must be exactly 12 - " + partCode);
                        } 
                    }

                    if (!normalizedPartCode.endsWith("#HQ")) {
                        errorsList.add(prefix + "Part Code must end with #HQ - " + partCode);
                    }
                }
            }
        	processedFilesCount++;
		}
		
		System.out.println("Json Validator Ended");
		System.out.println("Processed Files Count : " + processedFilesCount);
		
		if (errorsList.size() > 0) {
			System.out.println("Errors found - " + errorsList.size());
		} else {
			System.out.println("No Errors Found");
		}
		
		for (String error: errorsList) {
			System.out.println(error);
		}
	}
}
