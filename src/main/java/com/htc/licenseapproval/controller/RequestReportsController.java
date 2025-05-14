package com.htc.licenseapproval.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.htc.licenseapproval.dto.BUResponseDTO;
import com.htc.licenseapproval.dto.RequestDetailsDTO;
import com.htc.licenseapproval.dto.RequestResponseDTO;
import com.htc.licenseapproval.dto.ResponseDTO;
import com.htc.licenseapproval.enums.LicenseType;
import com.htc.licenseapproval.response.BaseResponse;
import com.htc.licenseapproval.service.RequestDetailsService;
import com.htc.licenseapproval.service.RequestHeaderService;
import com.htc.licenseapproval.utils.DateFormatter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/licenseApproval/approvalRequest")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class RequestReportsController {
	@Autowired
	private RequestHeaderService requestListService;

	@Autowired
	private RequestDetailsService requestDetailsService;
	
	@GetMapping("/date")
    public LocalDateTime date( 
    		  @RequestParam
              @Parameter(description = "pattern = yyyy-MM-dd") String Date) {
		
		LocalDate lacalDate = LocalDate.parse(Date);
		LocalDateTime date = DateFormatter.normaliseDate(lacalDate.atStartOfDay());
		
		return date;
		
	}
	
	@Operation(summary = "Overall Report By emp id -- unuses")
	@GetMapping(value = "/report/overallReport")
	public ResponseEntity<Map<Long, List<RequestDetailsDTO>>> overallReport() {
		return ResponseEntity.ok(requestDetailsService.totalReport());
	}

	@Operation(summary = "Overall Report for all Bu")
	@GetMapping(value = "/report/overallReport/perBUbyMap")
	public ResponseEntity<Map<String, BUResponseDTO<List<RequestResponseDTO>>>> buMapForTotalRequest(
			@RequestParam LicenseType licenseType) {
		return ResponseEntity.ok(requestListService.totalRequestForAllBUs(licenseType));
	}

	// REPORTS CONTAINS ONLY CONSUMED LICENSE (Except PENDING)
	@Operation(summary = "Get quarterly report by BU", description = "Returns license request count per quarter")
	@GetMapping("/report/quarterlyReport/byBU")
	public ResponseEntity<BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>>> quarterlyReportByBU(
			@RequestParam String Bu, @RequestParam LicenseType licenseType) {

		Map<Month, ResponseDTO<List<RequestResponseDTO>>> map1 = new EnumMap<>(Month.class);

		Map<Month, List<RequestResponseDTO>> report = requestListService.quarterlyReportBYBU(Bu, licenseType);
		for (Month key : report.keySet()) {
			ResponseDTO<List<RequestResponseDTO>> responseDTO = new ResponseDTO<>();
			responseDTO.setData(report.get(key));
			responseDTO.setCount(report.get(key).size());

			map1.put(key, responseDTO);

		}
		BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>> response = new BaseResponse<>();
		response.setMessage("Quarterly report by BU fetched successfully ");
		response.setData(map1);
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// REPORTS CONTAINS ONLY CONSUMED LICENSE (Except PENDING)
	@Operation(summary = "Get annual report", description = "Returns license request count per month for a year")
	@GetMapping("/report/annualReport")
	public ResponseEntity<BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>>> annualReport(
			@RequestParam LicenseType licenseType) {

		Map<Month, ResponseDTO<List<RequestResponseDTO>>> map = new EnumMap<>(Month.class);

		Map<Month, List<RequestResponseDTO>> report = requestListService.annualReport(licenseType);
		for (Month key : report.keySet()) {
			ResponseDTO<List<RequestResponseDTO>> responseDTO = new ResponseDTO<>();
			responseDTO.setData(report.get(key));
			responseDTO.setCount(report.get(key).size());

			map.put(key, responseDTO);

		}
		BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>> response = new BaseResponse<>();
		response.setMessage("Annual report fetched successfully ");
		response.setData(map);
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);

	}

	@GetMapping("/report/quartertlyReport/perBu")
	public ResponseEntity<Map<Month, Map<String, ResponseDTO<List<RequestDetailsDTO>>>>> quartertlyReportperBu(
			LicenseType licenseType) {
		Map<Month, Map<String, ResponseDTO<List<RequestDetailsDTO>>>> map1 = new EnumMap<>(Month.class);
		Map<Month, Map<String, List<RequestDetailsDTO>>> report = requestListService.quarterlyReportPerBU(licenseType);
		for (Month key : report.keySet()) {
			Map<String, ResponseDTO<List<RequestDetailsDTO>>> newMap = new HashMap<>();
			for (String bu : report.get(key).keySet()) {
				ResponseDTO<List<RequestDetailsDTO>> responseDTO = new ResponseDTO<>();
				responseDTO.setData(report.get(key).get(bu));
				responseDTO.setCount(report.get(key).get(bu).size());
				newMap.put(bu, responseDTO);
			}
			map1.put(key, newMap);
		}
		return ResponseEntity.ok(map1);
	}

	@Operation(summary = "quarterly Report per quarter (Q1, Q2, Q3, Q4)")
	@GetMapping(value = "/report/quarterReport/perQuarter")
	public ResponseEntity<Map<Month, Map<String, ResponseDTO<List<RequestDetailsDTO>>>>> quarterReportPerQuarter(
			@RequestParam LicenseType licenseType, @RequestParam String quarter) {
		
		Map<Month, Map<String, ResponseDTO<List<RequestDetailsDTO>>>> map1 = new EnumMap<>(Month.class);
		Map<Month, Map<String, List<RequestDetailsDTO>>> report = requestListService.quarterlyReportperQuater(licenseType, quarter);
		for (Month key : report.keySet()) {
			Map<String, ResponseDTO<List<RequestDetailsDTO>>> newMap = new HashMap<>();
			for (String bu : report.get(key).keySet()) {
				ResponseDTO<List<RequestDetailsDTO>> responseDTO = new ResponseDTO<>();
				responseDTO.setData(report.get(key).get(bu));
				responseDTO.setCount(report.get(key).get(bu).size());
				newMap.put(bu, responseDTO);
			}
			map1.put(key, newMap);
		}
		
		return ResponseEntity.ok(map1);
	}

	@Operation(summary = "Get quarterly report", description = "Returns license request count per quarter")
	@GetMapping("/report/quarterlyReport")
	public ResponseEntity<BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>>> quarterlyReport(
			@RequestParam LicenseType licenseType) {
		Map<Month, ResponseDTO<List<RequestResponseDTO>>> map = new EnumMap<>(Month.class);
		Map<Month, List<RequestResponseDTO>> report = requestListService.quarterlyReport(licenseType);
		for (Month key : report.keySet()) {
			ResponseDTO<List<RequestResponseDTO>> responseDTO = new ResponseDTO<>();
			responseDTO.setData(report.get(key));
			responseDTO.setCount(report.get(key).size());
			map.put(key, responseDTO);

		}
		BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>> response = new BaseResponse<>();
		response.setMessage(" Quarterly report fetched successfully ");
		response.setData(map);
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
