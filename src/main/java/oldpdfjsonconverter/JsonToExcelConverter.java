package oldpdfjsonconverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

public class JsonToExcelConverter {
	 private static final Integer BATCH_SIZE = 200;
	 
	 private static final String inputFolderPath = "D:\\testnewfilesoutput";
	 private static final String outputFilePath = ".\\2025_Excel_File.xlsx";
	 
	 private static ArrayList<JSONObject> dataList = new ArrayList<>();
	 
	 public static void main(String[] args) throws Exception {
	  	
	  	 File folder = new File(inputFolderPath);
	        if (!folder.exists() || !folder.isDirectory()) {
	            //System.out.println("Input folder does not exist: " + inputFolderPath);
	            return;
	        }
	        System.out.println("Json To Excel Converter Started");
	        Integer totalProcessedFilesCount = 0;
	        Integer totalRowsCount = 0;
	        
	        // Iterate over each file in the folder
	        for (File file : folder.listFiles()) {
//	        	if (! file.getName().contains("6740540")) {
//	        		continue;
//	        	}
	        	System.out.println("processing " + file.getName());
	        	String jsonString = new String(Files.readAllBytes(file.toPath()), "UTF-8");
//	        	System.out.println("json string :" + jsonString);
	        	
	        	JSONObject data = new JSONObject(jsonString);
	            
	            
	            String beNumber = data.getString("BE_NUMBER");
	            String beDate = data.getString("BE_DATE");
	            
	            JSONArray items = data.getJSONArray("ITEMS");
//	            System.out.println("Total items: " + items.length());
	            for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
//	            	System.out.println("item index: " + itemIndex);
	            	
	            	JSONObject itemDetail = items.getJSONObject(itemIndex);
	            	
	            	itemDetail.put("BE_NUMBER", beNumber);
	            	itemDetail.put("BE_DATE", beDate);
//	            	System.out.println("adding item ");
	            	addItemDataToExcel(itemDetail);
	            	totalRowsCount++;
	            }
	            
	            totalProcessedFilesCount++;
	        }
	        
	        if (dataList.size() > 0) {
	        	writeToExcel();	
	        }

	        System.out.println("Excel created successfully");
	        System.out.println("Json To Excel Converter Ended");
	        
	        System.out.println("Total Files Processed " + totalProcessedFilesCount);
	        System.out.println("Total Rows Count " + totalRowsCount);
	        
    }
	 
	 public static void addItemDataToExcel(JSONObject itemData) throws Exception {
		 dataList.add(itemData);
		 
		 if (dataList.size() > BATCH_SIZE) {
			 writeToExcel();
			 dataList.clear();
		 }
	 }
	 	 
	 public static void writeToExcel() throws Exception {
		 Workbook workbook;
         Sheet sheet;
         int rowNum = 0;

         
         File excelFile = new File(outputFilePath);
    
         if (excelFile.exists() && excelFile.length() > 0) {
    	    try (FileInputStream fis = new FileInputStream(excelFile)) {
    	        workbook = new XSSFWorkbook(fis);
    	    }
    	    sheet = workbook.getSheetAt(0);
    	    rowNum = sheet.getLastRowNum() + 1; // append after last row
    	} else {
    	    // File does not exist or is empty -> create workbook and sheet
    	    workbook = new XSSFWorkbook();
    	    sheet = workbook.createSheet("Data");
    	    rowNum = writeHeaders(sheet, 0); // write headers
    	}

         for (int dataListIndex = 0; dataListIndex < dataList.size(); dataListIndex++) {
             JSONObject data = dataList.get(dataListIndex);
        	 rowNum = writeRow(sheet, data, rowNum); 
         }

         // Save workbook
         try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
             workbook.write(fos);
         }
         workbook.close();


	 }
	 
	    private static int writeHeaders(Sheet sheet, int rowNum) {
	        Row headerRow = sheet.createRow(rowNum++);
	        String[] headers = {"BE Number", "BE Date", "Item Serial No", "Part Code", "Description", "Concatenate Description", "UQC", "Quantity"};

	        for (int i = 0; i < headers.length; i++) {
	            headerRow.createCell(i).setCellValue(headers[i]);
	        }
	        return rowNum;
	    }
	 
	    private static int writeRow(Sheet sheet, JSONObject data, int rowNum) {
	        Row row = sheet.createRow(rowNum++);
	        
	        String itemSerialNo = data.optString("Item Serial No");
	        
	        if (itemSerialNo.isEmpty()) {
	        	System.out.print(data);
	        }
	        row.createCell(0).setCellValue(data.optString("BE_NUMBER"));
	        row.createCell(1).setCellValue(data.optString("BE_DATE"));
	        row.createCell(2).setCellValue(Integer.parseInt(data.optString("ITEM_SERIAL_NO")));
	        row.createCell(3).setCellValue(data.optString("PART_CODE"));
	        row.createCell(4).setCellValue(data.optString("DESCRIPTION"));
	        row.createCell(5).setCellValue(data.optString("CONCATENATE_DESCRIPTION"));
	        row.createCell(6).setCellValue(data.optString("UQC"));
	        row.createCell(7).setCellValue(Integer.parseInt(data.optString("QUANTITY")));
	          
	        return rowNum;
	    }
}