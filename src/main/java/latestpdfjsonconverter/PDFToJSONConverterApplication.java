	package latestpdfjsonconverter;
	
	import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;
	
	
	public class PDFToJSONConverterApplication {
		public static Integer expectedSerialNo = null;
		
		public static String inputFolderPath = "D:\\outward2026\\outward2026";
		public static String outputFolderPath = "D:\\test\\";
		
		public static void main(String[] args) throws Exception {
		    File folder = new File(inputFolderPath);
	        if (!folder.exists() || !folder.isDirectory()) {
	            //System.out.println("Input folder does not exist: " + inputFolderPath);
	            return;
	        }
	
	        // Iterate over each file in the folder
	        for (File file : folder.listFiles()) {
	        	System.out.println("Processing file :" + file.getName());
	        	
	        	try (PDDocument document = PDDocument.load(file)) {
	                PDFTextStripper pdfStripper = new PDFTextStripper();
	
	                pdfStripper.setSortByPosition(true);
	
	                String text = pdfStripper.getText(document);
	                text = normalizePdfString(text);
	                
	                //System.out.println(text);
	//                System.exit(-1);
	
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
	                
	                JSONObject data = new JSONObject();
	                data.put("BE Number", beNo);
	                data.put("BE Date", beDate);
	                data.put("BE Type", beType);
	                data.put("Items", invoiceItems);
	        
	                String outputFileName = beNo + "_" + beDate.replace("/", "-") + ".json";
	                
	                String outputFilePath = outputFolderPath + outputFileName;
	                Path outputPath = Paths.get(outputFilePath);

	             // Write JSON to new output file
		             Files.write(
		                 outputPath,
		                 data.toString(4).getBytes(StandardCharsets.UTF_8)
		             );
//	                try (FileWriter outputFileObject = new FileWriter(outputFilePath)) {
//	                	outputFileObject.write(data.toString(4));
//	                    //System.out.println("JSON file saved: " + outputFileName);
//	                } catch (IOException e) {
//	                    e.printStackTrace();
//	                }
	                
	                
	//                //System.out.println("Extracted Text:\n" + text);
	
	            } catch (IOException e) {
	                e.printStackTrace();
	            }		
	
	        }
	    }
		
		public static String normalizePdfString(String pdfText) {
	        if (pdfText == null) return null;
	
	        return pdfText
	                .replaceAll("[\\r\\n]+", " ") // replace line breaks with space
	                .replaceAll("-\\s+", "")       // fix hyphenated words
	                .replaceAll("\\s{2,}", " ")   // collapse multiple spaces
	                .replaceAll("[^\\p{Print}]", "") // remove non-printable characters
	                .trim();
	    }
		
		public static JSONArray extractInvoiceItems(String text) {
			JSONArray invoiceItems = new JSONArray();
			
			String startMarker = "PART II INVOICE & VALUATION DETAILS";
	        String endMarker = "GLOSSARY A : LC Letter of Credit;";
	
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
					    "(?s)\\b" + currentSerial + "\\b\\s+\\d{8}\\s+.*?(?=\\s*\\b" + nextSerial + "\\b\\s+\\d{8})"
					);
				Matcher matcher = pattern.matcher(tableContent);
			    
				String rowContent = null;
			    
			    if (matcher.find()) {
			     rowContent = matcher.group().trim();
			    } else {
			    	String endText = "GLOSSARY A : LC Letter of Credit;";
	
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
	        
	        // Regex to extract all fields including trailing description after amount
	        Pattern pattern = Pattern.compile(
	            "(\\d+)\\s+" +          // Group 1: Serial No
	            "(\\d+)\\s+" +          // Group 2: CTH / Part Code
	            "(.*?)\\s+" +           // Group 3: Description before Unit Price
	            "(\\d+\\.\\d+)\\s+" +   // Group 4: Unit Price
	            "(\\d+\\.\\d+)\\s+" +   // Group 5: Quantity
	            "(\\w+)\\s+" +          // Group 6: UQC
	            "(\\d+\\.\\d+)\\s*" +   // Group 7: Amount
	            "(.*)"                  // Group 8: Trailing description (optional)
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
	
	        pattern = Pattern.compile("^(\\d[\\d\\s\\-\\.]*|)(.*)$");
	
	    	matcher = pattern.matcher(concatenateDescription);
	
	        if (matcher.find()) {
	            partCode = matcher.group(1) != null ? matcher.group(1).replaceAll("[\\s\\-\\.]+$", "") : "";
	            description = matcher.group(2).trim();
	        }
	
	        //System.out.println("Part Code: " + partCode);
	        //System.out.println("Description: " + description);
	        //System.out.println("----------");
	   
	        // Put values in a Map
	        itemDetails.put("Item Serial No", serialNo);
	        itemDetails.put("UQC", uqc);
	        itemDetails.put("Amount", amount);
	        itemDetails.put("Concatenate Description", concatenateDescription.trim());
	        itemDetails.put("Quantity", quantity);
	        itemDetails.put("Unit Price", unitPrice);
	        itemDetails.put("Description", description.trim());
	        itemDetails.put("Part Code", partCode + "#HQ");
	
			return itemDetails;
		}
	}