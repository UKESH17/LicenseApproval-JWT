package com.htc.licenseapproval.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.htc.licenseapproval.dto.BUdetailsDTO;
import com.htc.licenseapproval.dto.ChangeExpireData;
import com.htc.licenseapproval.dto.CoursesDTO;
import com.htc.licenseapproval.dto.LicenseApprovalDTO;
import com.htc.licenseapproval.dto.NewRequestListDTO;
import com.htc.licenseapproval.dto.RequestDetailsDTO;
import com.htc.licenseapproval.dto.RequestResponseDTO;
import com.htc.licenseapproval.entity.BUdetails;
import com.htc.licenseapproval.entity.Courses;
import com.htc.licenseapproval.entity.UploadedFile;
import com.htc.licenseapproval.enums.RequestType;
import com.htc.licenseapproval.enums.Status;
import com.htc.licenseapproval.file.compressor.Compressor;
import com.htc.licenseapproval.repository.BUdetailsRepository;
import com.htc.licenseapproval.response.BaseResponse;
import com.htc.licenseapproval.service.ExcelService;
import com.htc.licenseapproval.service.RequestDetailsService;
import com.htc.licenseapproval.service.RequestHeaderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/licenseApproval/approvalRequest")
@CrossOrigin(origins = "http://localhost:5173",allowCredentials = "true")
@Tag(name = "License Approval Request APIs-RequestListController", description = "Endpoints for handling license approval requests")
public class RequestListController {

	@Autowired
	private RequestHeaderService requestListService;

	@Autowired
	private RequestDetailsService requestDetailsService;

	@Autowired
	private Compressor compressor;

	@Autowired
	private ExcelService excelService;
	
	@Autowired
	private BUdetailsRepository buRepository;
		
	@Operation(summary = "Create new license request", description = "Submit a new license request with approval mail and optional Excel file")
	@PostMapping(value = "/newRequest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BaseResponse<RequestResponseDTO>> newRequest(
			@RequestPart(value="JSON-body",required = true) String newRequestListJson,
			@RequestPart(value ="approval-mail"
			,required = false) MultipartFile approvalmail,
			@RequestPart(value = "excel", required = false) MultipartFile excel) throws IOException {
		
		ObjectMapper objectMapper = new ObjectMapper();
		NewRequestListDTO newRequestListDTO = objectMapper.readValue(newRequestListJson, NewRequestListDTO.class);
		System.out.println(approvalmail);
		System.out.println( approvalmail != null);
		if(approvalmail != null) {
		String fileName = approvalmail.getOriginalFilename();
		
		if (fileName != null && !(fileName.endsWith(".pdf"))) {	
			BaseResponse<RequestResponseDTO> response = new BaseResponse<RequestResponseDTO>();
			response.setMessage("Invalid file format! Please upload a PDF file (.pdf).");
			response.setData(null);
			response.setCode(HttpStatus.BAD_REQUEST.value());
			return ResponseEntity.badRequest().body(response);	
		}
		
		UploadedFile approvalMail = new UploadedFile();
		approvalMail.setFileName(fileName);
		approvalMail.setFileType(approvalmail.getContentType());
		approvalMail.setFileData(compressor.compress(approvalmail.getBytes()));
		newRequestListDTO.setApprovalMail(approvalMail);
		
		}
		
		
		if (newRequestListDTO.getRequestType().equals(RequestType.MULTIPLE) && excel != null) {
			UploadedFile excelFile = new UploadedFile();
			newRequestListDTO.setRequestDetails(excelService.readExcelAndProcess(excel));
			excelFile.setFileName(excel.getOriginalFilename());
			excelFile.setFileType(excel.getContentType());
			excelFile.setFileData(compressor.compress(excel.getBytes()));
			newRequestListDTO.setExcelFile(excelFile);
		}

		BaseResponse<RequestResponseDTO> response = new BaseResponse<RequestResponseDTO>();
		response.setMessage("Request successfully received "
				+ (approvalmail!=null ? "and file uploaded successfully with name "+approvalmail.getOriginalFilename() : ""));
		response.setData(requestListService.newRequestHeader(newRequestListDTO));
		response.setCode(HttpStatus.CREATED.value());
		
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	@Operation(summary = "Approve or reject a request", description = "Change the status of a license request (approve or reject)")
	@PutMapping(value = "/approval/{requestHeaderId}")
	public ResponseEntity<BaseResponse<LicenseApprovalDTO>> approveRequest(@RequestParam Status status, @PathVariable String requestHeaderId)
			throws IOException {
		
		BaseResponse<LicenseApprovalDTO> response = new BaseResponse<>();
		response.setMessage("License status updated");
		response.setData(requestListService.requestApproval(requestHeaderId, status));
		response.setCode(HttpStatus.CREATED.value());
		
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
	
	@Operation(summary = "For updating end date", description = "Change the end date of the license request ")
	@PutMapping(value = "/endDate/{requestID}")
	public ResponseEntity<BaseResponse<RequestResponseDTO>> changeExpiryDate(@RequestParam String expiredDate,@RequestParam String reason,@PathVariable String requestID)
	{
		ChangeExpireData changeExpiredDateDTO = new ChangeExpireData(reason, expiredDate);
			
		BaseResponse<RequestResponseDTO> response = new BaseResponse<>();
		response.setMessage("Updated end date ");
		response.setData(requestDetailsService.changeExpireDate(requestID, changeExpiredDateDTO));
		response.setCode(HttpStatus.CREATED.value());
		
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	@Operation(summary = "Add courses to a request", description = "Attach course details and certificate files to a request")
	@PostMapping(value = "/add/Courses/{requestDetailsId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BaseResponse<RequestDetailsDTO>> addCourses(
		    @PathVariable String requestDetailsId,
		    @RequestPart(value="course-JSON") String coursesDTOjson,
		    @RequestPart(value="certificates",required = false) MultipartFile[] files
		) throws IOException {

		ObjectMapper objectMapper = new ObjectMapper();
		CoursesDTO coursesDTOs = objectMapper.readValue(coursesDTOjson, CoursesDTO.class);
		
		Courses courses = new Courses();
		courses.setCertificateDone(coursesDTOs.isCertificateDone());
		courses.setCourseDetails(coursesDTOs.getCourseDetails());
		courses.setHoursSpent(coursesDTOs.getHoursSpent());

		if(files!=null) {
		Set<UploadedFile> uploadedFiles = new HashSet<>();
		for (MultipartFile multipartFile : files) {
			UploadedFile uploadedFile = new UploadedFile();
			uploadedFile.setFileData(compressor.compress(multipartFile.getBytes()));
			uploadedFile.setFileName(multipartFile.getOriginalFilename());
			uploadedFile.setFileType(multipartFile.getContentType());
			uploadedFiles.add(uploadedFile);
			uploadedFile.setCourses(courses);
		}
		courses.setCertificates(uploadedFiles);
		}

		BaseResponse<RequestDetailsDTO> response = new BaseResponse<>();
		response.setMessage("Added courses to a request ");
		response.setData(requestDetailsService.addCourses(requestDetailsId, courses));
		response.setCode(HttpStatus.CREATED.value());
		
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
	
	@PostMapping("/addBU")
	public ResponseEntity<BaseResponse<BUdetails>> addBU(@RequestBody BUdetailsDTO bu){
		BUdetails bUdetails = new BUdetails();
		bUdetails.setBu(bu.getBu());
		bUdetails.setBuHead(bu.getBuHead());
		bUdetails.setBuDeliveryHead(bu.getBuDeliveryHead());
		BaseResponse<BUdetails> response = new BaseResponse<>();
		response.setMessage("BU successfully added");
		response.setData(buRepository.save(bUdetails));
		response.setCode(HttpStatus.CREATED.value());
		
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	} 
	
	@PostMapping(value="/excel/addAllBU", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<BaseResponse<Set<BUdetailsDTO>>> addBUbyExcel(@RequestPart("bu-excel") MultipartFile file){ 

		BaseResponse<Set<BUdetailsDTO>> response = new BaseResponse<>();
		response.setMessage("Added all BUS");
		response.setData(excelService.readExcelAndProcessforBudetails(file));
		response.setCode(HttpStatus.CREATED.value());
		
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}
	
	@DeleteMapping("/delete/bu")
	public String deleteBu(String name) {
		buRepository.delete(buRepository.findByBu(name).get());
		return "success";
	}
}
