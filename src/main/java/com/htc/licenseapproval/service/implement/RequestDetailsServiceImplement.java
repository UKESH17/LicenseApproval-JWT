package com.htc.licenseapproval.service.implement;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htc.licenseapproval.dto.ChangeExpireData;
import com.htc.licenseapproval.dto.CoursesDTO;
import com.htc.licenseapproval.dto.RequestDetailsDTO;
import com.htc.licenseapproval.dto.RequestResponseDTO;
import com.htc.licenseapproval.entity.Courses;
import com.htc.licenseapproval.entity.LicenseLogMessages;
import com.htc.licenseapproval.entity.RequestDetails;
import com.htc.licenseapproval.file.compressor.Compressor;
import com.htc.licenseapproval.mapper.MapperService;
import com.htc.licenseapproval.repository.LicenseLogMessagesRepository;
import com.htc.licenseapproval.repository.RequestDetailsRepository;
import com.htc.licenseapproval.repository.UploadedFileRepository;
import com.htc.licenseapproval.service.RequestDetailsService;
import com.htc.licenseapproval.utils.DateFormatter;

@Service
public class RequestDetailsServiceImplement implements RequestDetailsService {

	@Autowired
	private MapperService mapperService;

	@Autowired
	private RequestDetailsRepository requestDetailsRepository;
	
	@Autowired
	private LicenseLogMessagesRepository licenseLogMessagesRepository;

	@Autowired
	private UploadedFileRepository uploadedFileRepository;
	
	@Autowired 
	private RequestHeaderServiceImplement headerServiceImplement;

	@Autowired
	private Compressor compressor;
	
	/* FIND */

	@Override
	public RequestDetails findById(String requestDetailId) {
		return this.requestDetailsRepository.findById(requestDetailId)
				.orElseThrow(() -> new RuntimeException("Request detail id not found"));
	}

	/* ADD NEW COURSES */

	@Override
	public RequestDetailsDTO addCourses(String requestDetailsId, Courses courses) {

		RequestDetails requestDetails = this.findById(requestDetailsId);
		Set<Courses> allCourses = requestDetails.getCourses();
		if (allCourses.isEmpty()) {
			allCourses = new HashSet<>();
		}
		allCourses.add(courses);
		allCourses.stream().forEach(course -> course.setRequestDetails(requestDetails));;
		requestDetails.setCourses(allCourses);
		return mapperService.toRequestDetailsDTO(requestDetailsRepository.save(requestDetails));

	}

	/* FETCH COURSES */

	@Override
	public Set<CoursesDTO> getCourseandCertificates(String requestDetailsId) {
		return this.findById(requestDetailsId).getCourses().stream().map(course -> {
			course.getCertificates().forEach(cert -> {
				try {
					cert.setFileData(compressor.decompress(cert.getFileData()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			return mapperService.toCourseDTO(course);
		}).collect(Collectors.toSet());
	}

	/* DOWNLOAD CERTIFICATES */

	@Override
	public byte[] downloadCertificate(Long fileId) throws IOException, RuntimeException {
		return compressor.decompress(uploadedFileRepository.findById(fileId)
				.orElseThrow(() -> new RuntimeException("File not found with id : " + fileId)).getFileData());
	}

	@Override
	public RequestResponseDTO changeExpireDate(String requestID, ChangeExpireData changeExpiredDateDTO) {
		RequestDetails requestDetails = this.findById(requestID);
		
		LicenseLogMessages logMessages =  new LicenseLogMessages();
		logMessages.setLoggedDate(DateFormatter.normaliseDate(LocalDateTime.now()));
		logMessages.setLogMessages("License Expired Date changed ->  for request details id "+requestID+" from "+requestDetails.getLicenseDetails().getLicenseExpireDate()+"\nreason - "+changeExpiredDateDTO.getLogMessage());
		logMessages.setRequestDetails(requestDetails);
		
		requestDetails.getLicenseDetails().setLicenseExpireDate(DateFormatter.normaliseDate(changeExpiredDateDTO.getExpireDate()));
		RequestDetails request =requestDetailsRepository.save(requestDetails);
	
		licenseLogMessagesRepository.save(logMessages);
		headerServiceImplement.statusUpdate(request,LocalDateTime.now());
		return mapperService.toResponseDTO(request);
	}

	@Override
	public Set<LicenseLogMessages> licenseLogPerRequestId(String requestID) {
		return this.findById(requestID).getLicenseLogMessages();
	}

	@Override
	public int totalEnrollmentcount(Long employeeId) {	
		return this.allRequestDetailsByEmployeeId(employeeId).size();
	}
	
	@Override
	public List<RequestDetails> allRequestDetailsByEmployeeId(Long employeeId){
		return requestDetailsRepository.findAllByEmpid(employeeId);
	}
	

	@Override
	public Map<Long, List<RequestDetailsDTO>> totalReport() {
		Map<Long, List<RequestDetailsDTO>> totalReport = new HashMap<>();
		for(RequestDetails requestDetails : requestDetailsRepository.findAll()) {
			Long empId =requestDetails.getEmpid();
			if(!totalReport.containsKey(empId)){
				totalReport.put(empId, this.allRequestDetailsByEmployeeId(empId).stream().map( mapperService::toRequestDetailsDTO).collect(Collectors.toList()));
			}
		}
		return totalReport;
	}
	
	@Override
	public Map<Long, List<RequestDetails>> totalReports() {
		Map<Long, List<RequestDetails>> totalReport = new HashMap<>();
		for(RequestDetails requestDetails : requestDetailsRepository.findAll()) {
			Long empId =requestDetails.getEmpid();
			if(!totalReport.containsKey(empId)){
				totalReport.put(empId, this.allRequestDetailsByEmployeeId(empId));
			}
		}
		return totalReport;
	}

}
