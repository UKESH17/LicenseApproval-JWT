package com.htc.licenseapproval.controller;

import java.io.IOException;
import java.time.Month;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.htc.licenseapproval.dto.BUResponseDTO;
import com.htc.licenseapproval.dto.CoursesDTO;
import com.htc.licenseapproval.dto.NewRequestListDTO;
import com.htc.licenseapproval.dto.RequestDetailsDTO;
import com.htc.licenseapproval.dto.RequestResponseDTO;
import com.htc.licenseapproval.dto.ResponseDTO;
import com.htc.licenseapproval.entity.BUdetails;
import com.htc.licenseapproval.entity.LicenseLogMessages;
import com.htc.licenseapproval.entity.RequestDetails;
import com.htc.licenseapproval.enums.LicenseType;
import com.htc.licenseapproval.mapper.MapperService;
import com.htc.licenseapproval.repository.BUdetailsRepository;
import com.htc.licenseapproval.response.BaseResponse;
import com.htc.licenseapproval.service.RequestDetailsService;
import com.htc.licenseapproval.service.RequestHeaderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/licenseApproval/approvalRequest")
@CrossOrigin(origins = "http://localhost:5173",allowCredentials = "true")
@Tag(name = "License Approval Data Fetching Controller", description = "APIs for handling license approval requests data")
public class RequestDataController {

	@Autowired
	private RequestHeaderService requestListService;

	@Autowired
	private RequestDetailsService requestDetailsService;

	@Autowired
	private BUdetailsRepository bUdetailsRepository;
	

	@Autowired
	private MapperService mapperService;

	@Operation(summary = "Get all requests", description = "Fetches all license approval requests")
	@GetMapping("/getall/requests")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllRequestList() throws IOException {
		long start = System.currentTimeMillis();
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All Requests fetched successfully ");
		response.setData(requestListService.totalRequest());
		response.setCode(HttpStatus.ACCEPTED.value());
		long end = System.currentTimeMillis();
		log.info("Upload took: " + (end - start) + " ms");
		return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
	}

	@GetMapping("/getall/BUs")
	public List<BUdetails> getAllBUs() {
		return bUdetailsRepository.findAll();
	}

	@Operation(summary = "Get course and certificates", description = "Returns course and certificate details by request ID")
	@GetMapping(value = "/getall/certificates/{requestDetailsId}")
	public ResponseEntity<BaseResponse<Set<CoursesDTO>>> getCourseDetails(
			@Parameter(description = "ID of request details") @PathVariable String requestDetailsId)
			throws IOException {

		BaseResponse<Set<CoursesDTO>> response = new BaseResponse<>();
		response.setMessage("All course and certificates fetched successfully for request");
		response.setData(requestDetailsService.getCourseandCertificates(requestDetailsId));
		response.setCode(HttpStatus.OK.value());

		return ResponseEntity.ok(response);
	}
	@Operation(summary = "Get requests by employee", description = "Fetches requests made by a specific employee")
	@GetMapping(value = "/getallrequests/{empID}")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllRequestListByEmployee(
			@PathVariable Long empID) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All Requests fetched successfully with employee id : " + empID);
		response.setData(requestListService.totalRequestsByEmp(empID));
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get pending licenses", description = "Fetches all pending license requests")
	@GetMapping("/getall/pendingLicense")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllPendingLicenses(
			@RequestParam LicenseType licenseType) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All pending licenses fetched successfully ");
		response.setData(requestListService.allPendingLicense(licenseType));
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	

	@Operation(summary = "Get active licenses", description = "Fetches all Consumed license requests - which include all license expect pending")
	@GetMapping("/getall/ConsumedLicense")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllConsumedLicenses(
			@RequestParam LicenseType licenseType) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		
		ResponseDTO<List<RequestResponseDTO>> res = new ResponseDTO<>();
		List<RequestResponseDTO> allConsumed=requestListService.allConsumedLicense(licenseType).getData().stream().map(mapperService::toResponseDTO).collect(Collectors.toList());
		res.setData(allConsumed);
		res.setCount(allConsumed.size());
		
		response.setMessage("All active licenses fetched successfully ");
		response.setData(res);
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get active licenses", description = "Fetches all currently active license requests")
	@GetMapping("/getall/activeLicense")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllActiveLicenses(
			@RequestParam LicenseType licenseType) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All active licenses fetched successfully ");
		response.setData(requestListService.allActiveLicense(licenseType));
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get expiring soon licenses", description = "Fetches all licenses that are expiring soon")
	@GetMapping("/getall/ExpiringSoonLicense")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllExpiringSoonLicense(
			@RequestParam LicenseType licenseType) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All Expiring soon licenses fetched successfully ");
		response.setData(requestListService.allExpireSoonLicense(licenseType));
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get expired licenses", description = "Fetches all expired licenses")
	@GetMapping("/getall/ExpiredLicense")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllExpiredLicense(
			@RequestParam LicenseType licenseType) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("Expired licenses fetched successfully ");
		response.setData(requestListService.allExpiredLicense(licenseType));
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get pending requests", description = "Fetches all license requests with status pending")
	@GetMapping("/getall/pendingRequests")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllPendingRequest(
			@RequestParam LicenseType licenseType) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All Pending Requests fetched successfully ");
		response.setData(requestListService.pendingRequest(licenseType));
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get rejected requests", description = "Fetches all license requests that were rejected")
	@GetMapping("/getall/rejectedRequest")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllRejectedRequest(
			@RequestParam LicenseType licenseType) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All Rejected Requests fetched successfully ");
		response.setData(requestListService.rejectedRequest(licenseType));
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get approved requests", description = "Fetches all license requests that are approved")
	@GetMapping("/getall/approvedRequest")
	public ResponseEntity<BaseResponse<ResponseDTO<List<RequestResponseDTO>>>> getAllApprovedRequest(
			@RequestParam LicenseType licenseType) throws IOException {
		BaseResponse<ResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All Approved Requests fetched successfully ");
		response.setData(requestListService.approvedRequest(licenseType));
		response.setCode(HttpStatus.OK.value());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get requests by Business Unit ---", description = "Fetches all license requests with the business unit")
	@GetMapping("/getall/byBU/{BUname}")
	public ResponseEntity<BaseResponse<BUResponseDTO<List<RequestResponseDTO>>>> getAllRequestByBU(
			@RequestParam LicenseType licenseType, @PathVariable String BUname) throws IOException {

		BaseResponse<BUResponseDTO<List<RequestResponseDTO>>> response = new BaseResponse<>();
		response.setMessage("All Requets per Business unit " + BUname);
		response.setData(requestListService.totalRequestPerBU(BUname, licenseType));
		response.setCode(HttpStatus.OK.value());

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(summary = "Get Log Messages", description = "License Log messages per request id ")
	@GetMapping("/LicenseLogs/{requestId}")
	public ResponseEntity<BaseResponse<Set<LicenseLogMessages>>> licenseLogMessages(@PathVariable String requestId) {

		BaseResponse<Set<LicenseLogMessages>> response = new BaseResponse<>();
		response.setMessage("All log messages");
		response.setData(requestDetailsService.licenseLogPerRequestId(requestId));
		response.setCode(HttpStatus.OK.value());

		return new ResponseEntity<>(response, HttpStatus.OK);

	}


//	// REPORTS CONTAINS ONLY CONSUMED LICENSE (Except PENDING)
//
//	@Operation(summary = "Get quarterly report", description = "Returns license request count per quarter")
//	@GetMapping("/report/quarterlyReport")
//	public ResponseEntity<BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>>> quarterlyReport(
//			@RequestParam LicenseType licenseType) {
//		Map<Month, ResponseDTO<List<RequestResponseDTO>>> map = new EnumMap<>(Month.class);
//
//		Map<Month, List<RequestResponseDTO>> report = requestListService.quarterlyReport(licenseType);
//		for (Month key : report.keySet()) {
//			ResponseDTO<List<RequestResponseDTO>> responseDTO = new ResponseDTO<>();
//			responseDTO.setData(report.get(key));
//			responseDTO.setCount(report.get(key).size());
//
//			map.put(key, responseDTO);
//
//		}
//
//		BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>> response = new BaseResponse<>();
//		response.setMessage(" Quarterly report fetched successfully ");
//		response.setData(map);
//		response.setCode(HttpStatus.OK.value());
//
//		return new ResponseEntity<>(response, HttpStatus.OK);
//	}
//
//	// REPORTS CONTAINS ONLY CONSUMED LICENSE (Except PENDING)
//
//	@Operation(summary = "Get quarterly report by BU", description = "Returns license request count per quarter")
//	@GetMapping("/report/quarterlyReport/byBU")
//	public ResponseEntity<BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>>> quarterlyReportByBU(
//			@RequestParam String Bu, @RequestParam LicenseType licenseType) {
//
//		Map<Month, ResponseDTO<List<RequestResponseDTO>>> map1 = new EnumMap<>(Month.class);
//
//		Map<Month, List<RequestResponseDTO>> report = requestListService.quarterlyReportBYBU(Bu, licenseType);
//		for (Month key : report.keySet()) {
//			ResponseDTO<List<RequestResponseDTO>> responseDTO = new ResponseDTO<>();
//			responseDTO.setData(report.get(key));
//			responseDTO.setCount(report.get(key).size());
//
//			map1.put(key, responseDTO);
//
//		}
//		BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>> response = new BaseResponse<>();
//		response.setMessage("Quarterly report by BU fetched successfully ");
//		response.setData(map1);
//		response.setCode(HttpStatus.OK.value());
//
//		return new ResponseEntity<>(response, HttpStatus.OK);
//	}
//
//	// REPORTS CONTAINS ONLY CONSUMED LICENSE (Except PENDING)
//	@Operation(summary = "Get annual report", description = "Returns license request count per month for a year")
//	@GetMapping("/report/annualReport")
//	public ResponseEntity<BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>>> annualReport(
//			@RequestParam LicenseType licenseType) {
//
//		Map<Month, ResponseDTO<List<RequestResponseDTO>>> map = new EnumMap<>(Month.class);
//
//		Map<Month, List<RequestResponseDTO>> report = requestListService.annualReport(licenseType);
//		for (Month key : report.keySet()) {
//			ResponseDTO<List<RequestResponseDTO>> responseDTO = new ResponseDTO<>();
//			responseDTO.setData(report.get(key));
//			responseDTO.setCount(report.get(key).size());
//
//			map.put(key, responseDTO);
//
//		}
//		BaseResponse<Map<Month, ResponseDTO<List<RequestResponseDTO>>>> response = new BaseResponse<>();
//		response.setMessage("Annual report fetched successfully ");
//		response.setData(map);
//		response.setCode(HttpStatus.OK.value());
//		return new ResponseEntity<>(response, HttpStatus.OK);
//
//	}
//	@GetMapping("/report/quartertlyReport/perBu")
//	public ResponseEntity<Map<Month, Map<String, ResponseDTO<List<RequestDetailsDTO>>>>> quartertlyReportperBu(
//			LicenseType licenseType) {
//
//		Map<Month, Map<String, ResponseDTO<List<RequestDetailsDTO>>>> map1 = new EnumMap<>(Month.class);
//
//		Map<Month, Map<String, List<RequestDetailsDTO>>> report = requestListService.quarterlyReportPerBU(licenseType);
//
//		for (Month key : report.keySet()) {
//
//			Map<String, ResponseDTO<List<RequestDetailsDTO>>> newMap = new HashMap<>();
//
//			for (String bu : report.get(key).keySet()) {
//
//				ResponseDTO<List<RequestDetailsDTO>> responseDTO = new ResponseDTO<>();
//
//				responseDTO.setData(report.get(key).get(bu));
//
//				responseDTO.setCount(report.get(key).get(bu).size());
//
//				newMap.put(bu, responseDTO);
//
//			}
//
//			map1.put(key, newMap);
//
//		}
//
//		return ResponseEntity.ok(map1);
//
//	}
//
//	// not used
//	@Operation(summary = "Overall Report By emp id -- unuses")
//	@GetMapping(value = "/report/overallReport")
//	public ResponseEntity<Map<Long, List<RequestDetailsDTO>>> overallReport() {
//		return ResponseEntity.ok(requestDetailsService.totalReport());
//	}
//	
//	@Operation(summary = "Overall Report for all Bu")
//	@GetMapping(value = "/report/overallReport/perBUbyMap")
//	public ResponseEntity<Map<String, ResponseDTO<List<RequestResponseDTO>>>> buMapForTotalRequest(@RequestParam LicenseType licenseType) {
//		return ResponseEntity.ok(requestListService.totalRequestForAllBUs(licenseType));
//	}
//	
//	@Operation(summary = "quarterly Report per quarter (Q1, Q2, Q3, Q4)")
//	@GetMapping(value = "/report/quarterReport/perQuarter")
//	public ResponseEntity<Map<Month, List<RequestResponseDTO>>> quarterReportPerQuarter(@RequestParam LicenseType licenseType,@RequestParam String quarter) {
//		return ResponseEntity.ok(requestListService.quarterlyReportperQuater(licenseType, quarter));
//	}


}
