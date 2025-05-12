package com.htc.licenseapproval.service;

import java.io.IOException;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestParam;

import com.htc.licenseapproval.dto.BUResponseDTO;
import com.htc.licenseapproval.dto.LicenseApprovalDTO;
import com.htc.licenseapproval.dto.NewRequestListDTO;
import com.htc.licenseapproval.dto.RequestDetailsDTO;
import com.htc.licenseapproval.dto.RequestResponseDTO;
import com.htc.licenseapproval.dto.ResponseDTO;
import com.htc.licenseapproval.entity.RequestDetails;
import com.htc.licenseapproval.entity.RequestHeader;
import com.htc.licenseapproval.entity.UploadedFile;
import com.htc.licenseapproval.enums.LicenseType;
import com.htc.licenseapproval.enums.Status;

public interface RequestHeaderService {
	
	public RequestHeader findById(String requestId);

	public RequestResponseDTO newRequestHeader(NewRequestListDTO newRequestListDTO);

	public LicenseApprovalDTO requestApproval(String requestId, Status status);

	public byte[] getApprovalMail(String requestId) throws IOException;
	
	public byte[] getExcelFile(String requestId) throws IOException;

	public ResponseDTO<List<RequestResponseDTO>> totalRequest();

	public List<RequestDetails> getAllRequestLists();

	public ResponseDTO<List<RequestResponseDTO>> totalRequestsByEmp(Long empid);
	
	public ResponseDTO<List<RequestResponseDTO>> allActiveLicense( LicenseType licenseType);

	public ResponseDTO<List<RequestResponseDTO>> allPendingLicense( LicenseType licenseType);
 
	public ResponseDTO<List<RequestResponseDTO>> allExpireSoonLicense( LicenseType licenseType);

	public ResponseDTO<List<RequestResponseDTO>> allExpiredLicense( LicenseType licenseType);

	public ResponseDTO<List<RequestResponseDTO>> pendingRequest( LicenseType licenseType);

	public ResponseDTO<List<RequestResponseDTO>> rejectedRequest( LicenseType licenseType);

	public ResponseDTO<List<RequestResponseDTO>> approvedRequest( LicenseType licenseType);

	public Map<Month, List<RequestResponseDTO>> quarterlyReport( LicenseType licenseType);

	public Map<Month, List<RequestResponseDTO>> annualReport( LicenseType licenseType);

	public Map<Month, List<RequestResponseDTO>> quarterlyReportBYBU(String BU, LicenseType licenseType);
	
	public Map<Month, Map<String, List<RequestDetailsDTO>>> quarterlyReportPerBU(LicenseType licenseType);

	public BUResponseDTO<List<RequestResponseDTO>> totalRequestPerBU(String name, LicenseType licenseType);
	
	public  Map<String, BUResponseDTO<List<RequestResponseDTO>>> totalRequestForAllBUs(LicenseType licenseType);

	public ResponseDTO<List<RequestDetails>> allConsumedLicense(LicenseType licenseType);

	Map<Month, List<RequestResponseDTO>> quarterlyReportperQuater(LicenseType licenseType, String quarter);
	

}
