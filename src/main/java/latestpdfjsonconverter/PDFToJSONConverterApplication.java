package latestpdfjsonconverter;
	
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;
	
public class PDFToJSONConverterApplication {
	public static Integer expectedSerialNo = null;
	
	public static String inputFolderPath = "D:\\New_2025_PDF_Files";
	public static String outputFolderPath = "D:\\NEW_2025_OUTPUT_JSON_FILES\\";
	
	public static HashMap<String, JSONObject> additionalData = null;
	
	public static void main(String[] args) throws Exception {
	    File folder = new File(inputFolderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            //System.out.println("Input folder does not exist: " + inputFolderPath);
            return;
        }
        
        System.out.println("PDF To JSON Converter started");
        // Iterate over each file in the folder
        Integer totalProcessedFilesCount = 0;
        
        ArrayList<String> gatePassFilesList = new ArrayList<>();
        
        for (File file : folder.listFiles()) {
        	System.out.println("Processing file :" + file.getName());
//        	
//        	if (! file.getName().contains("701886103122024INCJJ6BE1070120251918")) {
//        		continue;
//        	}
        	List<String> ignoreFilesList = Arrays.asList("551912710092024INCJJ6BE1160920241353.pdf", 
        												 "475842529072024INCJJ6BE1200820242051.pdf",
        												 "576529124092024INCJJ6BE1011020241721.pdf",
        												 "601511308102024INCJJ6BE1141020242149.pdf",
        												 "701886103122024INCJJ6BE1070120251918.pdf"
        												);
        	
	    	if (ignoreFilesList.contains(file.getName())) {
	    		continue;
	    	}
        	
        	try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper pdfStripper = new PDFTextStripper();

                pdfStripper.setSortByPosition(true);

                String text = pdfStripper.getText(document);
              //  System.out.println("text: " + text);
                text = normalizePdfString(text);
                
//	             System.out.println(text);
//	            extractAdditionalData(text);
//                System.exit(-1);
                
                Integer totalItems = getTotalItems(text);
                
                additionalData = extractAdditionalData(text, totalItems);
                
                expectedSerialNo = 1;
                
                Pattern pattern = Pattern.compile(
                    "BE No.*?(\\d+)\\s+" +        // Group 1: BE No (digits)
                    "(\\d{2}/\\d{2}/\\d{4})\\s+" + // Group 2: BE Date (dd/MM/yyyy)
                    "([A-Z])"                     // Group 3: BE Type (single letter)
                );

                Matcher matcher = pattern.matcher(text);

                String beNo = "";
                String beDate = "";
                String beType = "";

                if (matcher.find()) {
                    beNo = matcher.group(1);
                    beDate = matcher.group(2);
                    beType = matcher.group(3);
                }

                //System.out.println("BE No: " + beNo);
                //System.out.println("BE Date: " + beDate);
                //System.out.println("BE Type: " + beType);

                
                JSONArray invoiceItems = extractInvoiceItems(text);
                
                if (totalItems != invoiceItems.length()) {
                	if (text.contains("Gatepass -Custodian Copy")) {
                		gatePassFilesList.add(file.getName());
                		totalProcessedFilesCount++;
                		continue;
                	} else {
                		System.out.println("Total items not matched expected "+ totalItems + " actual " + invoiceItems.length());
                    	System.exit(-1);
                	}
                }
                JSONObject data = new JSONObject();
                data.put("BE Number", beNo);
                data.put("BE Date", beDate);
                data.put("BE Type", beType);
                data.put("Items", invoiceItems);
        
                String outputFileName = beNo + "_" + beDate.replace("/", "-") + ".json";
                
                String outputFilePath = outputFolderPath + outputFileName;
                
                try (FileWriter outputFileObject = new FileWriter(outputFilePath)) {
                	outputFileObject.write(data.toString(4));
                    //System.out.println("JSON file saved: " + outputFileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                
//                //System.out.println("Extracted Text:\n" + text);
                totalProcessedFilesCount++;
            } catch (IOException e) {
                e.printStackTrace();
            }		

        }
        
        System.out.println("PDF To JSON Converter ended");
        System.out.println("Total Processed Files : " + totalProcessedFilesCount);
        
        if (! gatePassFilesList.isEmpty()) {
        	System.out.println("Gate Pass Files Count " + gatePassFilesList.size());
        	
        	for (String gatePassFileName : gatePassFilesList) {
        		System.out.println(gatePassFileName);
        	}
        }
	}
	
	public static String normalizePdfString(String pdfText) {
	    if (pdfText == null) return null;

//		    return pdfText
//		            .replace("\r", " ")
//		            .replace("\n", " ")          // remove line breaks
//		            .replaceAll("-\\s+", "")     // fix hyphen split words
//		            .replace("\\\"", "\"")       // remove escape before quote
//		            .replace("\\\\", "\\")       // reduce double backslash
//		            .replaceAll("\\s{2,}", " ")  // collapse spaces
//		            .trim();
	    return pdfText
	            // 1. Replace carriage returns and line breaks with a space
	            .replace("\r", " ")
	            .replace("\n", " ")
	            // 2. Remove hyphen **only if it is followed by a line break** (word split across lines)
	            .replaceAll("-\\s*(?=\\S)", "-")  // keep hyphens in codes
	            // 3. Collapse multiple spaces into one
	            .replaceAll("\\s{2,}", " ")
	            .trim();
	}
	
	public static JSONArray extractInvoiceItems(String text) {
		JSONArray invoiceItems = new JSONArray();
		
		String startMarker = "PART -II -INVOICE & VALUATION DETAILS";
        String endMarker = "GLOSSARY A : LC -Letter of Credit;";

        Pattern pattern = Pattern.compile(
                Pattern.quote(startMarker) + ".*?" + Pattern.quote(endMarker),
                Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String section = matcher.group().trim();
            //System.out.println(section);
            
//            System.exit(-1);
            String tableContent = "";
            
            Integer tableContentStartIndex = section.indexOf("1.S NO. 2.CTH 3.DESCRIPTION 4.UNIT PRICE 5.QUANTITY 6.UQC 7.AMOUNT");
            tableContent = section.substring(tableContentStartIndex);
            
            String invoiceNumber = extractInvoiceNumber(section);
            JSONArray itemsArray = processTableContent(tableContent, invoiceNumber);
            invoiceItems.putAll(itemsArray);
        }
        
        return invoiceItems;
    }
	
	public static String extractInvoiceNumber(String text) {
		String invoiceNumber = "";
		
		Pattern pattern = Pattern.compile("5\\.CONTRACT NO & DATE \\d+\\s+(\\S+)");

		Matcher matcher = pattern.matcher(text);

		if (matcher.find()) {
			invoiceNumber = matcher.group(1);
		}
		
		return invoiceNumber;
	}
	
	public static JSONArray processTableContent(String tableContent, String invoiceNo) {
		JSONArray itemsArray = new JSONArray();
		//System.out.println("table content " + tableContent);
		
		boolean isRowAvailable = true;
		
		JSONObject itemDetails = null;
		
		while (isRowAvailable) {
			Integer currentSerial = expectedSerialNo;
			Integer nextSerial = expectedSerialNo + 1;
			
			Pattern pattern = Pattern.compile(
				    "(?s)\\b" + currentSerial + "\\b\\s+\\d{8}\\s+.*?(?=\\s*\\b" + nextSerial + "\\b\\s+\\d{8}\\s+)"
				);
			Matcher matcher = pattern.matcher(tableContent);
		    
			String rowContent = null;
		    
		    if (matcher.find()) {
		     rowContent = matcher.group().trim();
		    } else {
		    	String endText = "GLOSSARY A : LC -Letter of Credit;";

		    	pattern = Pattern.compile(
		    	    "(?s)" + expectedSerialNo +  // e.g., 10
		    	    "\\s+\\d{8}\\s+.*?(?=\\s*" + Pattern.quote(endText) + ")"
		    	);

		    	matcher = pattern.matcher(tableContent);
		    	
		    	if (matcher.find()) {
		    	   rowContent = matcher.group().trim();
		    	} else {
		    		System.exit(-1);
		    	}
		    	isRowAvailable = false;
		    }
		    
		    if (rowContent != null) {
		    	itemDetails = processRowContent(rowContent);
		    	itemDetails.put("Invoice No", invoiceNo);
		    }
		    
		    itemsArray.put(itemDetails);
		    
		    expectedSerialNo++;
		}
	
        return itemsArray;
	}
	
	public static JSONObject processRowContent(String rowContent) {
		JSONObject itemDetails = new JSONObject();

	    String serialNo = "";
        String cth = "";
        String unitPrice = "";
        String quantity = "";
        String uqc = "";
        String amount = "";
        String concatenateDescription = "";
        String partCode = "";
        String description = "";
        
//        System.out.println(rowContent);
        // description and amount merged with - issue fix
        rowContent = rowContent.replaceAll("([a-zA-Z0-9]+)\\s*-\\s*(\\d+\\.\\d+)", "$1- $2"); // Regex to extract all fields including trailing description after amount
        rowContent = rowContent.replaceAll("-\\.(\\d{6})", "- 0.$1");
        rowContent = rowContent.replaceAll("-(\\d+\\.\\d+)", "- $1");
   
        Pattern pattern = Pattern.compile(
        	    "(\\d+)\\s+" +                  // Group 1: Serial No
        	    "(\\d+)\\s+" +                  // Group 2: CTH / Part Code
        	    "(.*?)\\s+" +                   // Group 3: Description before Unit Price
        	    "((?:\\d+)?\\.\\d{6})\\s+" +   // Group 4: Unit Price (6 decimal digits)
        	    "((?:\\d+)?\\.\\d{6})\\s+" +   // Group 5: Quantity (6 decimal digits)
        	    "(\\w+)\\s+" +                  // Group 6: UQC
        	    "((?:\\d+)?\\.\\d{2})\\s*" +   // Group 7: Amount (2 decimal digits)
        	    "(.*)"                          // Group 8: Trailing description (optional)
        	);

        Matcher matcher = pattern.matcher(rowContent);

        if (matcher.find()) {
            serialNo = matcher.group(1);
            cth = matcher.group(2);
            concatenateDescription = matcher.group(3) + " " + matcher.group(8); // combine before and after amount
            unitPrice = matcher.group(4);
            quantity = matcher.group(5);
            uqc = matcher.group(6);
            amount = matcher.group(7);
        }
        
//        if (serialNo.equals("48")) {
//        	System.out.println("contanateDescription : " + concatenateDescription);
//        }
        
//        pattern = Pattern.compile("^(\\d[\\d\\s\\-\\.]*|)(.*)$");
//
//    	matcher = pattern.matcher(concatenateDescription);
//
//        if (matcher.find()) {
//            partCode = matcher.group(1) != null ? matcher.group(1).replaceAll("[\\s\\-\\.]+$", "") : "";
//            description = matcher.group(2).trim();
//            
//            if (partCode.length() > 12) {
////            	System.out.println(partCode);
////            	if (partCode.contains("577419702")) {
////            		System.out.println("test");
////            	}
////            	if (partCode.matches("\\d{9}[\\s\\.].+$")) {
////            		description = partCode.substring(10) + description;
////            		partCode = partCode.substring(0, 9);
////            	} else if (partCode.contains(".")) {
////            		String[] splittedParts = partCode.split("\\.");
////            		
////            		partCode = splittedParts[0];
////            		description = splittedParts[1] + " " + description;
////            	}
//
//            }
//        }
    	StringBuilder digits = new StringBuilder();
    	int index = 0;
    	int digitCount = 0;
    	for (int i = 0; i < concatenateDescription.length(); i++) {
    	    char ch = concatenateDescription.charAt(i);
    	    digits.append(ch);
	        
    	    if (Character.isDigit(ch)) {
    	    	digitCount++;
    	    }

    	    if (digitCount == 9) {
    	        index = i + 1; 
    	        break;
    	    }
    	}

    	if (digitCount == 9) {
    	    partCode = digits.toString();
    	    description = concatenateDescription.substring(index).trim();

//    	    System.out.println("Part Code: " + partCode);
//    	    System.out.println("Remaining: " + description);
    	} else {
    		System.out.println(concatenateDescription);
    	    System.out.println("9 digits not found at start");
    	    System.exit(-1);
    	}
        
        if (unitPrice.startsWith(".")) {
        	unitPrice = "0" + unitPrice;
        }
        
        if (amount.startsWith(".")) {
        	amount = "0" + amount;
        }

//        System.out.println("Part Code: " + partCode);
        //System.out.println("Description: " + description);
        //System.out.println("----------");
//        System.out.println(rowContent);
        
        if (serialNo.equals("")) {
        	System.out.println("serial no is empty");
        	System.exit(-1);
        }
        // Put values in a Map
        itemDetails.put("Item Serial No", serialNo);
        itemDetails.put("HSN", cth);
        itemDetails.put("UQC", uqc);
        itemDetails.put("Amount", amount);
        itemDetails.put("Concatenate Description", concatenateDescription.trim());
        itemDetails.put("Quantity", quantity);
        itemDetails.put("Unit Price", unitPrice);
        itemDetails.put("Description", description.trim());
        itemDetails.put("Part Code", partCode + "#HQ");
        
        JSONObject additionalDataObject = additionalData.get(serialNo);
        
        if (additionalDataObject == null) {
        	System.out.println("Additional Data Object null");
        	System.exit(-1);
        }
        
        String customerQuantity = additionalDataObject.getString("CUSTOMER_QUANTITY");
        String customerUQC = additionalDataObject.getString("CUSTOMER_UQC");
        String supplierQuantity = additionalDataObject.getString("SUPPLIER_QUANTITY");
        String supplierUQC = additionalDataObject.getString("SUPPLIER_UQC");
        
        itemDetails.put("CUSTOMER_QUANTITY", customerQuantity);
        itemDetails.put("CUSTOMER_UQC", customerUQC);
        itemDetails.put("SUPPLIER_QUANTITY", supplierQuantity);
        itemDetails.put("SUPPLIER_UQC", supplierUQC);
        
		return itemDetails;
	}
	
	public static Integer getTotalItems(String text) {
		Integer totalItems = null;
		
		Pattern pattern = Pattern.compile("Nos\\s+\\d+\\s+(\\d+)\\b");
		Matcher matcher = pattern.matcher(text);

		if (matcher.find()) {
		    String number = matcher.group(1);
		    totalItems = Integer.parseInt(number);
		}
		
		return totalItems;
	}

	public static HashMap extractAdditionalData(String text, Integer totalItems) {
		HashMap<String, JSONObject> additionalData = new HashMap<>();
		String customerQty = "";
		String customerUQC = "";
		String supplierQty = "";
		String supplierUQC = "";
		
		Pattern pattern = Pattern.compile(
                "1\\.INVSNO\\s+2\\.ITEMSN\\s+3\\.CTH\\s+4\\.CETH\\s+5\\.ITEM DESCRIPTION\\s+6\\.FS\\s+7\\.PQ\\s+8\\.DC\\s+9\\.WC10\\.AQ.*?23\\.PRODN\\s*24\\.CNTRL",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(text);

        Integer count = 0;

        while (matcher.find()) {
            String itemContent = matcher.group();
            itemContent = itemContent.replaceAll("1.INVSNO 2.ITEMSN 3.CTH 4.CETH 5.ITEM DESCRIPTION 6.FS 7.PQ 8.DC 9.WC10.AQ ", "");
            String[] splittedParts = itemContent.split(" ");
            String itemSerialNo = splittedParts[1];
//            System.out.println(itemSerialNo);
            
            String expectedItemSerialNo = (count + 1) + "";
            if (! itemSerialNo.equals(expectedItemSerialNo)) {
            	System.out.println("Item Serial No " + itemSerialNo + ", expected item serial no : " + expectedItemSerialNo);
                
            	System.out.println("Item Serial No mismatched");
            }
            
            Integer index = itemContent.indexOf("13.C.QTY 14.C.UQC 15.S.QTY 16.S.UQC 17.SCH 18.STND/PR 19.RSP 20.REIMP 21.PROV 22.END USE");
            
            if (index > -1) {
            	itemContent = itemContent.substring(index);
            	itemContent = itemContent.replaceAll("13.C.QTY 14.C.UQC 15.S.QTY 16.S.UQC 17.SCH 18.STND/PR 19.RSP 20.REIMP 21.PROV 22.END USE ","");
//            	System.out.println(itemContent);
            	
            	splittedParts = itemContent.split(" ");
            	customerQty = splittedParts[2];
            	customerUQC = splittedParts[3];
            	supplierQty = splittedParts[4];
            	supplierUQC = splittedParts[5];
            	
            } else {
            	System.out.println("Item Content Different");
            	System.exit(-1);
            }
            
            if (customerQty.startsWith(".")) {
            	customerQty = "0" + customerQty;
            }
            
            if (supplierQty.startsWith(".")) {
            	supplierQty = "0" + supplierQty;
            }
            
            JSONObject data = new JSONObject();
            data.put("CUSTOMER_QUANTITY", customerQty);
            data.put("CUSTOMER_UQC", customerUQC);
            data.put("SUPPLIER_QUANTITY", supplierQty);
            data.put("SUPPLIER_UQC", supplierUQC);
            
            additionalData.put(itemSerialNo, data);
            count++;
        }

        
        if (! count.equals(totalItems)) {
        	if (! text.contains("Gatepass -Custodian Copy")) {
        		System.out.println("Total Items " + totalItems);
                System.out.println("Count " + count);
            	System.out.println("Extracting additional data failed count is not match with total items");
                System.exit(-1);
            }
        }
        
        return additionalData;
	}
}