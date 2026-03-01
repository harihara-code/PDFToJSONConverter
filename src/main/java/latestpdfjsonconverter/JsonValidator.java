package latestpdfjsonconverter;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonValidator {
	private static final String INPUT_FOLDER_PATH = "D:\\NEW_2025_OUTPUT_JSON_FILES";
	
	public static void main(String[] args) throws Exception {
		File folder = new File(INPUT_FOLDER_PATH);
       
		if (!folder.exists() || !folder.isDirectory()) {
            //System.out.println("Input folder does not exist: " + inputFolderPath);
            return;
        }

		System.out.println("Json Validator Started");
        
		ArrayList<String> errorsList = new ArrayList<>();
		
		Integer processedFilesCount = 0;
		Integer validatedRowsCount = 0;
		for (File file : folder.listFiles()) {
//        	System.out.println("Processing file :" + file.getName());

        	String jsonString = new String(Files.readAllBytes(file.toPath()), "UTF-8");
        	
        	JSONObject data = new JSONObject(jsonString);
            
            String beNumber = data.getString("BE Number");
            String beDate = data.getString("BE Date");
            String beType = data.getString("BE Type");
            
            if (beNumber == null || beNumber.isEmpty()) {
            	String errorMessage = file.getName() + " - BE Number is empty";
            	errorsList.add(errorMessage);
            }
            
            if (beDate == null || beDate.isEmpty()) {
            	String errorMessage = file.getName() + " - BE Date is empty";
            	errorsList.add(errorMessage);
            }
            
            if (beType == null || beType.isEmpty()) {
            	String errorMessage = file.getName() + " - BE Type is empty";
            	errorsList.add(errorMessage);
            }
            
            JSONArray items = data.getJSONArray("Items");
            
            for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
            	JSONObject item = items.getJSONObject(itemIndex);
            	
                String invoiceNo = item.optString("Invoice No");
                String itemSerialNo = item.optString("Item Serial No");
                String description = item.optString("Description");
                String uqc = item.optString("UQC");
                String amount = item.optString("Amount");
                String concatenateDescription = item.optString("Concatenate Description");
                String quantity = item.optString("Quantity");
                String partCode = item.optString("Part Code");
                String unitPrice = item.optString("Unit Price");
                String customerQuantity = item.optString("CUSTOMER_QUANTITY");
                String customerUQC = item.optString("CUSTOMER_UQC");
                String supplierQuantity = item.optString("SUPPLIER_QUANTITY");
                String supplierUQC = item.optString("SUPPLIER_UQC");
                
                String prefix = file.getName() + " - Item Serial No " + (itemIndex + 1) + " : ";

                if (invoiceNo == null || invoiceNo.isEmpty()) {
                    errorsList.add(prefix + "Invoice No is empty");
                }

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

                if (amount == null || amount.isEmpty()) {
                    errorsList.add(prefix + "Amount is empty");
                } else {
                	String normalizedAmount = amount.trim();

                    try {
                        Double.parseDouble(normalizedAmount);

                        if (!normalizedAmount.matches("^-?\\d+\\.\\d{2}$")) {
                            errorsList.add(prefix + "Amount must be a valid number with exactly 2 decimal places - " + amount);
                        }

                    } catch (NumberFormatException e) {
                        errorsList.add(prefix + "Amount is not a valid number - " + amount);
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
            	        Double.parseDouble(normalizedQty);

            	        if (!normalizedQty.matches("^-?\\d+\\.\\d{6}$")) {
            	            errorsList.add(prefix + "Quantity must have exactly 6 decimal places");
            	        }

            	    } catch (NumberFormatException e) {
            	        errorsList.add(prefix + "Quantity is not a valid number");
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

                if (unitPrice == null || unitPrice.isEmpty()) {
                    errorsList.add(prefix + "Unit Price is empty");
                } else {
                	String normalizedUnitPrice = unitPrice.trim();

                	try {
                	    Double.parseDouble(normalizedUnitPrice);
                	    
                	    if (!normalizedUnitPrice.matches("^-?\\d+\\.\\d{6}$")) {
                	        errorsList.add(prefix + "Unit Price must have exactly 6 decimal places " + unitPrice);
                	    }

                	} catch (NumberFormatException e) {
                	    errorsList.add(prefix + "Unit Price is not a valid number");
                	}
                }
                
                if (customerQuantity == null || customerQuantity.isEmpty()) {
                    errorsList.add(prefix + "Customer Quantity is empty");
                } else {
            	    try {
            	        Double.parseDouble(customerQuantity);
            	    } catch (NumberFormatException e) {
            	        errorsList.add(prefix + "Customer Quantity is not a valid number " + customerQuantity);
            	    }
                }
                
                if (customerUQC == null || customerUQC.isEmpty()) {
                    errorsList.add(prefix + "Customer UQC is empty");
                } else {
                	if (!customerUQC.equals("NOS") &&
            	        !customerUQC.equals("KGS") &&
            	        !customerUQC.equals("MTR") && 
            	        !customerUQC.equals("PCS")) {
            	        errorsList.add(prefix + "Customer UQC is invalid. UQC - " + customerUQC);
            	    }
                }
                
                if (supplierQuantity == null || supplierQuantity.isEmpty()) {
                    errorsList.add(prefix + "Supplier Quantity is empty");
                } else {
            	    try {
            	        Double.parseDouble(supplierQuantity);
            	    } catch (NumberFormatException e) {
            	        errorsList.add(prefix + "Supplier Quantity is not a valid number " + supplierQuantity);
            	    }
                }
                
                if (supplierUQC == null || supplierUQC.isEmpty()) {
                    errorsList.add(prefix + "Supplier UQC is empty");
                } else {
                	if (!supplierUQC.equals("NOS") &&
            	        !supplierUQC.equals("KGS") &&
            	        !supplierUQC.equals("MTR") && 
            	        !supplierUQC.equals("PCS")) {
            	        errorsList.add(prefix + "Supplier UQC is invalid. UQC - " + supplierUQC);
            	    }
                }
                validatedRowsCount++;
            }
            
        	processedFilesCount++;
		}
		
		System.out.println("Json Validator Ended");
		System.out.println("Processed Files Count : " + processedFilesCount);
		System.out.println("Validated Rows Count : " + validatedRowsCount);
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
