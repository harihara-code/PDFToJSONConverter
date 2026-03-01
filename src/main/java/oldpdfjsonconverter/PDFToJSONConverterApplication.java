package oldpdfjsonconverter;
	
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;
	
public class PDFToJSONConverterApplication {
	public static Integer expectedSerialNo = null;
	
	public static String inputFolderPath = "D:\\testnewfiles";
	public static String outputFolderPath = "D:\\testnewfilesoutput\\";
	
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
      
        	
        	try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
//              pdfStripper.setSortByPosition(true);
                
                String text = pdfStripper.getText(document);
                text = normalizePdfString(text);
                
                String beNumber = "";
                String beDate = "";
                
                Pattern pattern = Pattern.compile(
                	    "\\b(\\d{7})\\s*-\\s*(\\d{2}/\\d{2}/\\d{4})\\b"
                	);

            	Matcher matcher = pattern.matcher(text);

            	if (matcher.find()) {
            		beNumber = matcher.group(1);
            	    beDate = matcher.group(2);
            	}
            	
            	if (beNumber.isEmpty()) {
            		System.out.println("BE Number is empty");
            		System.exit(-1);
            	}
            	
            	if (beDate.isEmpty()) {
            		System.out.println("BE Date is empty");
            		System.exit(-1);
            	}
            		
                Integer serialNo = 1;
                
                Boolean rowAvailable = true;
                
                JSONArray items = new JSONArray();
                
                while (rowAvailable) {
                	pattern = Pattern.compile(
                			"\\b" + serialNo + "\\b\\s+NA\\s+\\d+\\s+.*?Others");	
                	
                	matcher = pattern.matcher(text);

                	if (matcher.find()) {
                	    String rowContent = matcher.group();
                	    
                	    JSONObject itemDetails = processRowContent(rowContent);
                	    items.put(itemDetails);
                	    
                	    serialNo++;
                	} else {
                		rowAvailable = false;
                	}
                }
                
                System.out.println("------------------");
//               System.exit(-1);
	            
                JSONObject data = new JSONObject();
                data.put("BE_NUMBER", beNumber);
                data.put("BE_DATE", beDate);
                data.put("ITEMS", items);
        
                String outputFileName = beNumber + "_" + beDate.replace("/", "-") + ".json";
                
                String outputFilePath = outputFolderPath + outputFileName;
                
                try (FileWriter outputFileObject = new FileWriter(outputFilePath)) {
                	outputFileObject.write(data.toString(4));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                totalProcessedFilesCount++;
            } catch (IOException e) {
                e.printStackTrace();
            }		

        }
        
        System.out.println("PDF To JSON Converter ended");
        System.out.println("Total Processed Files : " + totalProcessedFilesCount);
        
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
	
	
	public static JSONObject processRowContent(String rowContent) {
		JSONObject itemDetails = new JSONObject();
		
	    Pattern pattern = Pattern.compile(
	    	    "\\b(\\d+)\\b\\s+NA\\s+(\\d+)\\s+(\\w+)\\s+(.*?-?Others)",
	    	    Pattern.DOTALL
	    	);

    	Matcher matcher = pattern.matcher(rowContent);

    	while (matcher.find()) {
    	    String serialNo = matcher.group(1);
    	    String quantity = matcher.group(2);
    	    String unit = matcher.group(3);
    	    String concatenateDescription = matcher.group(4);

    	    itemDetails.put("ITEM_SERIAL_NO", serialNo);
    	    itemDetails.put("QUANTITY", quantity);
    	    itemDetails.put("UQC", unit);
    	    itemDetails.put("CONCATENATE_DESCRIPTION", concatenateDescription);
    	    
    	    setPartCodeAndDescription(itemDetails, concatenateDescription);
    	}
    	
    	return itemDetails;
	}
	
	public static void setPartCodeAndDescription(JSONObject itemDetails, String concatenateDescription) {
		String partCode = "";
		String description = "";
		
		StringBuilder digits = new StringBuilder();
    	int index = 0;
    	int digitCount = 0;
    	for (int i = 0; i < concatenateDescription.length(); i++) {
    	    char ch = concatenateDescription.charAt(i);
    	    digits.append(ch);
	        
    	    if (Character.isDigit(ch)) {
    	    	digitCount++;
    	    } else if (ch == ' ' || ch == '.' || ch == '-') {
    	    	continue;
    	    } else {
    	    	System.out.println("Invalid part code : " + concatenateDescription);
    	    	System.exit(-1);
    	    }

    	    if (digitCount == 9) {
    	        index = i + 1; 
    	        break;
    	    }
    	}

    	if (digitCount == 9) {
    	    partCode = digits.toString() + "#HQ";
    	    description = concatenateDescription.substring(index).trim();

    	} else {
    		System.out.println(concatenateDescription);
    	    System.out.println("9 digits not found at start");
    	    System.exit(-1);
    	}
    	itemDetails.put("DESCRIPTION", description);
    	itemDetails.put("PART_CODE", partCode);
    }
}