package latestpdfjsonconverter;

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
	 
	 private static final String inputFolderPath = "D:\\test";
	 private static final String outputFilePath = ".\\2026_Excel_File.xlsx";
	 
	 private static ArrayList<JSONObject> dataList = new ArrayList<>();
	 
	 public static void main(String[] args) throws Exception {
	  	
	  	 File folder = new File(inputFolderPath);
	        if (!folder.exists() || !folder.isDirectory()) {
	            //System.out.println("Input folder does not exist: " + inputFolderPath);
	            return;
	        }

	        // Iterate over each file in the folder
	        for (File file : folder.listFiles()) {
	        	System.out.println("processing " + file.getName());
	        	String jsonString = new String(Files.readAllBytes(file.toPath()), "UTF-8");
	            JSONObject data = new JSONObject(jsonString);
	            
	            
	            String beNumber = data.getString("BE Number");
	            String beDate = data.getString("BE Date");
	            String beType = data.getString("BE Type");
	            
	            JSONArray items = data.getJSONArray("Items");
	            System.out.println("Total items: " + items.length());
	            for (int itemIndex = 0; itemIndex < items.length(); itemIndex++) {
	            	System.out.println("item index: " + itemIndex);
	            	
	            	JSONObject itemDetail = items.getJSONObject(itemIndex);
	            	
	            	itemDetail.put("BE Number", beNumber);
	            	itemDetail.put("BE Date", beDate);
	            	itemDetail.put("BE Type", beType);
//	            	System.out.println("adding item ");
	            	addItemDataToExcel(itemDetail);
	            }
	        }
	        
	        if (dataList.size() > 0) {
	        	writeToExcel();	
	        }

        System.out.println("Excel created successfully");
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
	        String[] headers = {"BE Date", "BE Number", "BE Type", "Invoice Number", "Item Serial No", "Part Code", "Description", "Concatenate Description", "UNIT PRICE", "UQC", "QUANTITY", "AMOUNT"};

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
	        row.createCell(0).setCellValue(data.optString("BE Number"));
	        row.createCell(1).setCellValue(data.optString("BE Date"));
	        row.createCell(2).setCellValue(data.optString("BE Type"));
	        row.createCell(3).setCellValue(data.optString("Invoice No"));
	        row.createCell(4).setCellValue(data.optString("Item Serial No"));
	        row.createCell(5).setCellValue(data.optString("Part Code"));
	        row.createCell(6).setCellValue(data.optString("Description"));
	        row.createCell(7).setCellValue(data.optString("Concatenate Description"));
	        row.createCell(8).setCellValue(data.optString("Unit Price"));
	        row.createCell(9).setCellValue(data.optString("UQC"));
	        row.createCell(10).setCellValue(data.optString("Quantity"));
	        row.createCell(11).setCellValue(data.optString("Amount"));
		       
	        return rowNum;
	    }
}